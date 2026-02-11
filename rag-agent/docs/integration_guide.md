# 서비스 연동 가이드 (Integration Guide)

RAG 에이전트와 기존 Spring Boot 마이크로서비스 간의 연동을 위한 상세 명세입니다.

## 1. Kafka 토픽 연동

### A. 투표 생성 (`poll-created`)
- **Producer**: `pollService`
- **Payload**: `{"id": Long, "title": String, "description": String}`
- **Usage**: `rag-agent`가 수신하여 정책 위반 심층 검증(Async Deep) 수행.

### B. 댓글 생성 (`comment-created`)
- **Producer**: `voteService` (또는 댓글 서비스가 별도라면 해당 서비스)
- **Payload**: `{"pollId": Long, "content": String}`
- **Usage**: `rag-agent`가 수신하여 전처리 후 배치 요약 및 인덱싱 수행.

## 2. API 연동

### A. 동기 검증 (Fast Slang Check)
- **Method**: `POST`
- **URL**: `http://rag-agent:8000/api/v1/verify/fast`
- **Request**: `{"text": String}`
- **Response**: `{"is_allowed": Boolean, "reason": String}`
- **Usage**: `pollService`에서 투표 저장 전 호출하여 사용자에게 즉각적인 피드백 제공.

### B. 투표 블라인드 처리 (Policy Enforcement)
- **Method**: `PATCH`
- **URL**: `http://poll-service:8082/api/v1/polls/{poll_id}/status`
- **Request**: `{"status": "BLINDED", "reason": "Policy Violation"}`
- **Usage**: `rag-agent`에서 심층 검증 위반 발견 시 호출 (구현 필요).

## 3. 데이터 수집 전처리 (Spring Side)
- **추천 사항**: Kafka로 메시지를 쏘기 전, Spring Boot 애플리케이션 레벨에서도 기본적인 `trim()`이나 빈 문자열 체크를 수행하여 불필요한 네트워크 트래픽 절약.
