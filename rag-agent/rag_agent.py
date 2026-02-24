import os
import re
import json
import asyncio
import logging
from typing import List, Any, Dict
from fastapi import FastAPI
from pydantic import BaseModel, Field
from langchain_openai import OpenAIEmbeddings, ChatOpenAI
from langchain_chroma import Chroma
from langchain_core.documents import Document
from langchain_core.messages import HumanMessage, SystemMessage
from langgraph.graph import StateGraph, START, END
from typing_extensions import TypedDict
from contextlib import asynccontextmanager
from langchain_core.output_parsers import JsonOutputParser
from core.filter import TextCleaner
from core.batcher import CommentBatcher
from services.kafka_consumer import KafkaConsumerService

# 환경 변수 설정
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s"
)
logger = logging.getLogger("rag_agent")

EMBEDDING_API_URL = os.getenv("EMBEDDING_API_URL", "http://localhost:8000/v1")
LLM_API_URL = os.getenv("LLM_API_URL", "http://localhost:8001/v1")
KAFKA_BOOTSTRAP_SERVERS = os.getenv("KAFKA_BOOTSTRAP_SERVERS", "kafka-kraft:9092")
POLL_SERVICE_URL = os.getenv("POLL_SERVICE_URL", "poll-service:8082")
PERSIST_DIRECTORY = "./chroma_db"

# 모델 초기화
embedding_model = OpenAIEmbeddings(
    model="BAAI/bge-m3",
    openai_api_base=EMBEDDING_API_URL,
    openai_api_key="dummy"
)

llm = ChatOpenAI(
    model="pkun2/qwen3_4bit_mixed_kr_2_gptq",
    openai_api_base=LLM_API_URL,
    openai_api_key="dummy",
    temperature=0.1
)

# 벡터 DB 초기화 (Chroma)
vectorstore = Chroma(
    collection_name="everypoll_insights",
    embedding_function=embedding_model,
    persist_directory=PERSIST_DIRECTORY
)

# 유틸리티 초기화
cleaner = TextCleaner()

# --- LangGraph: 심층 정책 검증 (Deep Policy Check) ---

class PolicyCheckState(TypedDict):
    poll_id: str
    title: str
    description: str
    guidelines: str
    analysis: str
    is_violation: bool
    reason: str

async def retrieve_guidelines(state: PolicyCheckState):
    """가이드라인 문서 검색"""
    docs = await vectorstore.asimilarity_search("policy guideline", k=1)
    if docs:
        context = docs[0].page_content
    else:
        context = "기본 정책: 혐오 표현, 정치적 편향성, 분란 조장, 욕설이 포함된 투표는 금지됩니다."
    return {"guidelines": context}

class PolicyResult(BaseModel):
    is_violation: bool = Field(description="정책 위반 여부 (True/False)")
    reason: str = Field(description="위반했다면 그 구체적인 이유, 아니면 'Pass'")

parser = JsonOutputParser(pydantic_object=PolicyResult)

async def analyze_compliance(state: PolicyCheckState):
    """LLM을 통한 정책 위반 여부 분석"""

    format_instructions = parser.get_format_instructions()

    prompt = f"""
    [검토 기준]
    {state['guidelines']}
    [투표 정보]
    제목: {state['title']}
    설명: {state['description']}
    위 투표가 정책을 위반하는지 분석해.
    
    {format_instructions} 
    """

    response = await llm.ainvoke(prompt)
    
    try:
        clean_content = re.sub(
            r'<think>.*?</think>', 
            '', 
            response.content, 
            flags=re.DOTALL
        ).strip()
        
        parsed_result = parser.parse(clean_content)
        logger.info(f"Parsed Result: {parsed_result}")
        return {
            "analysis": parsed_result["reason"],
            "is_violation": parsed_result["is_violation"]
        }
    except Exception as e:
        logger.error(f"Parsing Error: {e}", exc_info=True)
        # 실패시 안전장치 (Fail-safe)
        return {"analysis": "Error parsing output", "is_violation": True}

# 그래프 구성
workflow = StateGraph(PolicyCheckState)
workflow.add_node("retrieve_guidelines", retrieve_guidelines)
workflow.add_node("analyze_compliance", analyze_compliance) 

workflow.add_edge(START, "retrieve_guidelines")
workflow.add_edge("retrieve_guidelines", "analyze_compliance")
workflow.add_edge("analyze_compliance", END) 

policy_checker_app = workflow.compile()


# --- 비즈니스 로직: 댓글 요약 및 인덱싱 ---
async def summarize_and_index_comments(poll_id: str, comments: List[str]) -> None:
    """배치된 댓글을 요약하여 벡터 DB에 저장"""
    try:
        combined_comments = "\n".join([f"- {c}" for c in comments])
        prompt = f"""
        다음은 투표(ID: {poll_id})에 달린 댓글들입니다. 
        이 내용들을 종합하여 핵심 의견과 분위기를 2~3문장으로 요약해 주세요.
        
        댓글 목록:
        {combined_comments}
        """
        response = await llm.ainvoke([
            SystemMessage(content="너는 투표 결과를 분석하는 전문 어시스턴트야."),
            HumanMessage(content=prompt)
        ])
        
        summary = response.content
        doc = Document(
            page_content=f"[댓글 요약] 투표 {poll_id}: {summary}",
            metadata={
                "poll_id": poll_id, 
                "type": "comment_summary", 
                "timestamp": str(asyncio.get_event_loop().time())
            }
        )
        await vectorstore.aadd_documents([doc])
        logger.info(f"✅ Indexed summary for poll {poll_id}")
    except Exception as e:
        logger.error(f"❌ Error in summarization: {e}", exc_info=True)

batcher = CommentBatcher(callback=summarize_and_index_comments)


# --- Kafka 핸들러 ---
async def handle_poll_event_router(data: dict):
    event_type = data.get("event") or data.get("eventType") or data.get("type") 
    logger.info(f"받은 이벤트: {event_type}")
    
    if event_type == "POLL_CREATED":
        await handle_poll_created(data)
    else:
        logger.warning(f"알 수 없는 이벤트: {event_type}")


async def handle_comment_created(data: dict):
    poll_id = data.get("pollId")
    content = data.get("content")
    if not poll_id or not content: return

    if cleaner.is_meaningless(content): return
    cleaned_content = cleaner.clean(content)
    await batcher.add_comment(poll_id, cleaned_content)

async def handle_poll_created(data: dict) -> None:
    """투표 생성 이벤트 -> LangGraph 심층 검증 실행"""
    poll_id = data.get("pollId") or data.get("id") # pollId 우선 사용, 없으면 id (호환성)
    title = data.get("title")
    description = data.get("description")
    
    logger.info(f"Deep checking poll {poll_id}...")
    
    # LangGraph 실행
    initial_state = {
        "poll_id": str(poll_id),
        "title": title or "",
        "description": description or "",
        "guidelines": "",
        "analysis": "",
        "is_violation": False,
        "reason": ""
    }
    
    result = await policy_checker_app.ainvoke(initial_state)
    
    if result["is_violation"]:
        logger.warning(
            f"⚠️ Policy Violation detected in poll {poll_id}: {result['reason']}"
        )
        
        # PollService로 블라인드 처리 이벤트(Kafka) 발행
        try:
            from aiokafka import AIOKafkaProducer
            producer = AIOKafkaProducer(
                bootstrap_servers=KAFKA_BOOTSTRAP_SERVERS,
                value_serializer=lambda v: json.dumps(v).encode('utf-8')
            )
            await producer.start()
            try:
                event_data = {
                    "@class": "com.everypoll.common.event.poll.PollBlindedEvent",
                    "type": "pollBlinded",
                    "pollId": int(poll_id),
                    "reason": result["reason"]
                }
                await producer.send_and_wait(
                    "poll-events", 
                    value=event_data,
                    headers=[("__TypeId__", b"pollBlinded")]
                )
                logger.info(f"✅ Blinding event published for poll {poll_id}")
            finally:
                await producer.stop()
        except Exception as e:
            logger.error(
                f"❌ Error publishing blinding event for poll {poll_id}: {e}", 
                exc_info=True
            )
    else:
        logger.info(f"✅ Poll {poll_id} passed deep check.")

@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info("[Startup] Kafka Consumer & Batcher 시작중...")
    kafka_service = KafkaConsumerService(
        bootstrap_servers=KAFKA_BOOTSTRAP_SERVERS,
        topics=["poll-events"],
        handler_map={
            "poll-events": handle_poll_event_router
        }
    )
    
    kafka_task = asyncio.create_task(kafka_service.start())
    app.state.kafka_service = kafka_service
    app.state.kafka_task = kafka_task
    batcher.start()
    logger.info("[Startup] 모든 시스템 정상 가동 완료!")
    
    yield
    
    logger.info("[Shutdown] 리소스 정리 중...")
    
    await kafka_service.stop() 
    
    kafka_task.cancel()
    try:
        await kafka_task
    except asyncio.CancelledError:
        pass
    # Batcher 종료
    batcher.stop()
    logger.info("[Shutdown] 리소스 정리끝!")

app = FastAPI(
    title="EveryPoll RAG Agent API",
    lifespan=lifespan
)

# --- FastAPI 엔드포인트 ---
class CheckRequest(BaseModel):
    text: str

@app.post("/api/v1/verify/fast")
async def fast_verify(request: CheckRequest) -> Dict[str, Any]:
    """Fast Sync Check (동기)"""
    is_bad = cleaner.has_slang(request.text)
    return {"is_allowed": not is_bad, "reason": "slang" if is_bad else None}

@app.get("/health")
async def health_check():
    return {"status": "ok"}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
