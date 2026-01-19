from threading import Lock
from typing import List, Dict, Any, Optional, Literal
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from langchain_core.messages import BaseMessage, AIMessage, HumanMessage, ToolMessage, AnyMessage
from langchain_core.tools import BaseTool, tool
from langchain_openai import OpenAIEmbeddings, ChatOpenAI
from langchain_core.documents import Document
from langchain_core.vectorstores import InMemoryVectorStore
from langgraph.graph import StateGraph, START, END
from typing_extensions import TypedDict, Annotated
import json
import uuid
import operator
import os

app = FastAPI(title="RAG Agent API")

EMBEDDING_API_URL = os.getenv("EMBEDDING_API_URL", "http://localhost:8000/v1")
LLM_API_URL = os.getenv("LLM_API_URL", "http://localhost:8001/v1")

print(f"🔧 Embedding API: {EMBEDDING_API_URL}")
print(f"🔧 LLM API: {LLM_API_URL}")

embedding_model = OpenAIEmbeddings(
    model="BAAI/bge-m3",
    openai_api_base=EMBEDDING_API_URL,
    openai_api_key="dummy"
)

print("✅ 임베딩 모델 연결 완료")

class DocumentManager:
    def __init__(self):
        self.documents: List[Document] = []
        self.current_index: int = 0
        self.lock = Lock()
    
    def add_documents(self, contents: List[str], source: str = "api") -> List[Document]:
        """문서 추가 및 인덱싱"""
        with self.lock:
            new_docs = []
            for content in contents:
                doc = Document(
                    page_content=content,
                    metadata={
                        "source": source,
                        "index": self.current_index,
                        "id": str(uuid.uuid4())
                    }
                )
                new_docs.append(doc)
                self.documents.append(doc)
                self.current_index += 1
            return new_docs
    
    def get_all_documents(self) -> List[Document]:
        """모든 문서 조회"""
        return self.documents
    
    def get_document_count(self) -> int:
        """문서 수 조회"""
        return len(self.documents)
    
    def delete_document(self, doc_id: str) -> bool:
        """문서 삭제 (ID 기반)"""
        with self.lock:
            for i, doc in enumerate(self.documents):
                if doc.metadata.get("id") == doc_id:
                    self.documents.pop(i)
                    return True
            return False

# 문서 관리자 인스턴스
doc_manager = DocumentManager()

sentences = [
    "반품은 구매 후 30일 이내에만 가능하며, 영수증이 필요합니다.",
    "배송은 평일 기준 2~3일 소요되며, 도서 산간 지역은 하루 더 걸립니다.",
    "회원 가입 시 10% 할인 쿠폰을 즉시 지급합니다.",
    "고객 센터 운영 시간은 오전 9시부터 오후 6시까지입니다.",
    "환불은 계좌 이체로 처리되며, 3~5 영업일 소요됩니다.",
    "무료 배송은 3만원 이상 구매 시 적용됩니다.",
    "주말 및 공휴일에는 고객센터가 운영되지 않습니다.",
    "제품 하자 시 전액 환불 또는 교환이 가능합니다."
]

initial_docs = doc_manager.add_documents(sentences, source="manual")

print(f"📚 {len(initial_docs)}개 문서를 벡터 스토어에 로드 중...")

vectorstore = InMemoryVectorStore.from_documents(
    documents=initial_docs,
    embedding=embedding_model
)

print(f"✅ 벡터 스토어 생성 완료 ({doc_manager.get_document_count()}개 문서)")

retriever = vectorstore.as_retriever(
    search_type="similarity",
    search_kwargs={"k": 3}
)

@tool
def retrieve_blog_posts(query: str) -> str:
    """
    사용자의 질문과 의미적으로 가장 유사한 회사 매뉴얼 내용을 검색합니다.
    
    Args:
        query: 검색할 질문 내용
    """
    print(f"🔍 검색 쿼리: {query}")
    docs = retriever.invoke(query)
    print(f"📄 검색된 문서 수: {len(docs)}")
    return "\n\n".join([doc.page_content for doc in docs])

retriever_tool = retrieve_blog_posts

llm = ChatOpenAI(
    model="pkun2/qwen3_4bit_mixed_kr_2_gptq",
    openai_api_base=LLM_API_URL,
    openai_api_key="dummy",
    temperature=0.7,
    max_tokens=2048
)

print("✅ LLM 연결 완료")

tools = [retrieve_blog_posts]
tools_by_name = {tool.name: tool for tool in tools}
llm_with_tools = llm.bind_tools(tools)

class MessagesState(TypedDict):
    messages: Annotated[list[AnyMessage], operator.add]

def should_continue(state: MessagesState) -> Literal["tool_node", "end"]:
    messages = state["messages"]
    last_message = messages[-1]
    if hasattr(last_message, "tool_calls") and last_message.tool_calls:
        return "tool_node"
    return "end"

def llm_call(state: MessagesState):
    """LLM 호출"""
    response = llm_with_tools.invoke(state["messages"])
    return {"messages": [response]}

from langgraph.prebuilt import ToolNode
tool_node = ToolNode(tools)

# 그래프 빌드
agent_builder = StateGraph(MessagesState)
agent_builder.add_node("llm_call", llm_call)
agent_builder.add_node("tool_node", tool_node)
agent_builder.add_edge(START, "llm_call")
agent_builder.add_conditional_edges(
    "llm_call",
    should_continue,
    {
        "tool_node": "tool_node",
        "end": END
    }
)
agent_builder.add_edge("tool_node", "llm_call")

agent = agent_builder.compile()

print("✅ LangGraph 에이전트 빌드 완료")

class ChatRequest(BaseModel):
    query: str
    conversation_id: Optional[str] = "default"

class ChatResponse(BaseModel):
    answer: str
    sources: List[str]

class AddDocumentsRequest(BaseModel):
    documents: List[str]
    source: Optional[str] = "api"

class AddDocumentsResponse(BaseModel):
    message: str
    added_count: int
    total_count: int
    documents: List[Dict[str, Any]]

class DocumentResponse(BaseModel):
    id: str
    content: str
    source: str
    index: int

class SearchResult(BaseModel):
    content: str
    metadata: Dict[str, Any]
    score: Optional[float] = None

@app.get("/health")
def health():
    """헬스체크"""
    return {
        "status": "healthy",
        "embedding_api": EMBEDDING_API_URL,
        "llm_api": LLM_API_URL,
        "documents_count": doc_manager.get_document_count()
    }

@app.post("/api/v1/chat", response_model=ChatResponse)
def chat(request: ChatRequest):
    """RAG 채팅 엔드포인트"""
    try:
        print(f"\n{'='*50}")
        print(f"💬 질문: {request.query}")
        print(f"🆔 대화 ID: {request.conversation_id}")
        
        # 에이전트 실행
        messages = [HumanMessage(content=request.query)]
        result = agent.invoke({"messages": messages})
        
        # 최종 답변 추출
        final_message = result["messages"][-1]
        answer = final_message.content
        
        # 검색된 소스 추출
        sources = []
        for msg in result["messages"]:
            if isinstance(msg, ToolMessage):
                sources.append(msg.content)
        
        print(f"✅ 답변: {answer}")
        print(f"📚 소스 개수: {len(sources)}")
        print(f"{'='*50}\n")
        
        return ChatResponse(
            answer=answer,
            sources=sources
        )
    
    except Exception as e:
        print(f"❌ 에러: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/api/v1/documents", response_model=AddDocumentsResponse)
def add_documents(request: AddDocumentsRequest):
    """문서 추가 (런타임에 추가)"""
    try:
        new_docs = doc_manager.add_documents(
            contents=request.documents,
            source=request.source
        )
        
        # 벡터 스토어에 추가
        vectorstore.add_documents(new_docs)
        
        added_docs_info = [
            {
                "id": doc.metadata["id"],
                "content": doc.page_content,
                "source": doc.metadata["source"],
                "index": doc.metadata["index"]
            }
            for doc in new_docs
        ]
        
        print(f"📝 {len(new_docs)}개 문서 추가됨 (총 {doc_manager.get_document_count()}개)")
        
        return AddDocumentsResponse(
            message=f"{len(new_docs)}개 문서 추가 완료",
            added_count=len(new_docs),
            total_count=doc_manager.get_document_count(),
            documents=added_docs_info
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/api/v1/search")
def search(query: str, k: int = 3):
    """직접 검색"""
    try:
        docs = vectorstore.similarity_search(query, k=k)
        
        return {
            "query": query,
            "k": k,
            "results_count": len(docs),
            "results": [
                {
                    "content": doc.page_content,
                    "metadata": doc.metadata
                }
                for doc in docs
            ]
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/api/v1/documents")
def list_documents():
    """저장된 문서 목록"""
    all_docs = doc_manager.get_all_documents()
    return {
        "total": len(all_docs),
        "documents": [
            {
                "id": doc.metadata.get("id"),
                "content": doc.page_content,
                "source": doc.metadata.get("source"),
                "index": doc.metadata.get("index")
            }
            for doc in all_docs
        ]
    }
    
@app.delete("/api/v1/documents/{doc_id}")
def delete_document(doc_id: str):
    """문서 삭제"""
    try:
        success = doc_manager.delete_document(doc_id)
        if success:
            return {
                "message": f"문서 {doc_id} 삭제 완료",
                "total_count": doc_manager.get_document_count()
            }
        else:
            raise HTTPException(status_code=404, detail=f"문서 {doc_id}를 찾을 수 없습니다")
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        app,
        host="0.0.0.0",
        port=8000,
        log_level="info"
    )
