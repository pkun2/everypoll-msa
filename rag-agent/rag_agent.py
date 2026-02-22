import os
import json
import asyncio
from typing import List, Dict, Any, Optional, Literal
from fastapi import FastAPI
from pydantic import BaseModel
from langchain_openai import OpenAIEmbeddings, ChatOpenAI
from langchain_chroma import Chroma
from langchain_core.documents import Document
from langchain_core.messages import HumanMessage, SystemMessage
from langgraph.graph import StateGraph, START, END
from typing_extensions import TypedDict

from core.filter import TextCleaner
from core.batcher import CommentBatcher
from services.kafka_consumer import KafkaConsumerService

app = FastAPI(title="EveryPoll RAG Agent API")

# 환경 변수 설정
EMBEDDING_API_URL = os.getenv("EMBEDDING_API_URL", "http://localhost:8000/v1")
LLM_API_URL = os.getenv("LLM_API_URL", "http://localhost:8001/v1")
KAFKA_BOOTSTRAP_SERVERS = os.getenv("KAFKA_BOOTSTRAP_SERVERS", "kafka-kraft:9092")
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

def retrieve_guidelines(state: PolicyCheckState):
    """가이드라인 문서 검색"""
    # 실제로는 벡터 스토어에서 'guideline' 타입 문서를 검색해야 함
    # 여기서는 예시 문구를 반환
    docs = vectorstore.similarity_search("policy guideline", k=1)
    if docs:
        context = docs[0].page_content
    else:
        context = "기본 정책: 혐오 표현, 정치적 편향성, 분란 조장, 욕설이 포함된 투표는 금지됩니다."
    return {"guidelines": context}

async def analyze_compliance(state: PolicyCheckState):
    """LLM을 통한 정책 위반 여부 분석"""
    prompt = f"""
    [검토 기준]
    {state['guidelines']}
    
    [투표 정보]
    제목: {state['title']}
    설명: {state['description']}
    
    위 투표가 검토 기준을 위반하는지 분석해 줘.
    위반 가능성이 높다면 "VIOLATION"으로 시작하고 이유를 적어줘.
    문제가 없다면 "PASS"라고 적어줘.
    """
    response = await llm.ainvoke(prompt)
    return {"analysis": response.content}

def determine_action(state: PolicyCheckState):
    """분석 결과에 따른 액션 결정"""
    analysis = state['analysis'].upper()
    is_violation = "VIOLATION" in analysis or "위반" in state['analysis']
    return {
        "is_violation": is_violation,
        "reason": state['analysis'] if is_violation else "Pass"
    }

# 그래프 구성
workflow = StateGraph(PolicyCheckState)
workflow.add_node("retrieve_guidelines", retrieve_guidelines)
workflow.add_node("analyze_compliance", analyze_compliance)
workflow.add_node("determine_action", determine_action)

workflow.add_edge(START, "retrieve_guidelines")
workflow.add_edge("retrieve_guidelines", "analyze_compliance")
workflow.add_edge("analyze_compliance", "determine_action")
workflow.add_edge("determine_action", END)

policy_checker_app = workflow.compile()


# --- 비즈니스 로직: 댓글 요약 및 인덱싱 ---
async def summarize_and_index_comments(poll_id: str, comments: List[str]):
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
            metadata={"poll_id": poll_id, "type": "comment_summary", "timestamp": str(asyncio.get_event_loop().time())}
        )
        vectorstore.add_documents([doc])
        print(f"✅ Indexed summary for poll {poll_id}")
    except Exception as e:
        print(f"❌ Error in summarization: {e}")

batcher = CommentBatcher(callback=summarize_and_index_comments)


# --- Kafka 핸들러 ---
async def handle_comment_created(data: dict):
    poll_id = data.get("pollId")
    content = data.get("content")
    if not poll_id or not content: return

    if cleaner.is_meaningless(content): return
    cleaned_content = cleaner.clean(content)
    await batcher.add_comment(str(poll_id), cleaned_content)

async def handle_poll_created(data: dict):
    """투표 생성 이벤트 -> LangGraph 심층 검증 실행"""
    poll_id = data.get("id")
    title = data.get("title")
    description = data.get("description")
    
    print(f"🕵️ Deep checking poll {poll_id}...")
    
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
        print(f"⚠️ Policy Violation detected in poll {poll_id}: {result['reason']}")
        # TODO: pollService.blind_poll(poll_id) API Call
    else:
        print(f"✅ Poll {poll_id} passed deep check.")


# --- FastAPI 엔드포인트 ---
class CheckRequest(BaseModel):
    text: str

@app.post("/api/v1/verify/fast")
async def fast_verify(request: CheckRequest):
    """Fast Sync Check (동기)"""
    is_bad = cleaner.has_slang(request.text)
    return {"is_allowed": not is_bad, "reason": "slang" if is_bad else None}

@app.get("/health")
async def health_check():
    return {"status": "ok"}

@app.on_event("startup")
async def startup_event():
    kafka_service = KafkaConsumerService(
        bootstrap_servers=KAFKA_BOOTSTRAP_SERVERS,
        topics=["comment-created", "poll-created"],
        handler_map={
            "comment-created": handle_comment_created,
            "poll-created": handle_poll_created
        }
    )
    asyncio.create_task(kafka_service.start())

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
