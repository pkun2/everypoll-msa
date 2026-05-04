from prometheus_client import Counter, Histogram, Gauge

# --- LangGraph 노드 실행 지연 ---
LANGGRAPH_NODE_DURATION = Histogram(
    "langgraph_node_duration_seconds",
    "LangGraph node execution latency",
    ["node"],
    buckets=[0.05, 0.1, 0.25, 0.5, 1.0, 2.5, 5.0, 10.0, 30.0]
)

# 정책 검사 결과: violation | pass | error
POLICY_CHECK_TOTAL = Counter(
    "policy_check_total",
    "Policy check outcomes",
    ["result"]
)

# --- 댓글 요약 ---
SUMMARIZATION_DURATION = Histogram(
    "summarization_duration_seconds",
    "Comment summarization end-to-end latency",
    buckets=[0.5, 1.0, 2.5, 5.0, 10.0, 30.0, 60.0]
)

SUMMARIZATION_TOTAL = Counter(
    "summarization_total",
    "Comment summarization attempts",
    ["status"]  # success | error
)

# --- 배치 플러시 ---
BATCH_FLUSH_TOTAL = Counter(
    "batch_flush_total",
    "Number of batch flushes",
    ["trigger"]  # size | time
)

BATCH_FLUSH_SIZE = Histogram(
    "batch_flush_size",
    "Comments per batch flush",
    buckets=[1, 2, 3, 4, 5, 10, 20, 50]
)

BATCH_BUFFER_SIZE = Gauge(
    "batch_buffer_current_size",
    "Total comments buffered across all polls"
)

# --- Kafka ---
KAFKA_EVENTS_TOTAL = Counter(
    "kafka_events_total",
    "Kafka events received",
    ["event_type"]
)

KAFKA_ERRORS_TOTAL = Counter(
    "kafka_errors_total",
    "Kafka consumer errors"
)

# --- Vector DB ---
VECTORDB_QUERY_DURATION = Histogram(
    "vectordb_query_duration_seconds",
    "Chroma vector DB operation latency",
    ["operation"],  # similarity_search | add_documents
    buckets=[0.01, 0.05, 0.1, 0.25, 0.5, 1.0, 2.5, 5.0]
)

# --- HTTP API ---
HTTP_REQUEST_DURATION = Histogram(
    "http_request_duration_seconds",
    "HTTP request latency",
    ["method", "endpoint", "status_code"],
    buckets=[0.005, 0.01, 0.025, 0.05, 0.1, 0.25, 0.5, 1.0, 2.5]
)

HTTP_REQUESTS_TOTAL = Counter(
    "http_requests_total",
    "HTTP requests total",
    ["method", "endpoint", "status_code"]
)
