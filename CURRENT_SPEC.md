# 현재 구현 명세서 (EveryPoll)

이 문서는 EveryPoll 프로젝트의 현재 상태를 설명하며, 구현된 아키텍처, 서비스, 기능 및 기술 스택에 대한 세부 정보를 제공합니다.

## 1. 프로젝트 개요
EveryPoll은 확장성과 실시간 상호 작용을 위해 설계된 마이크로서비스 기반 투표 애플리케이션입니다. 인증, 투표 관리, 투표 실행을 위한 별도의 서비스로 구성되어 있으며, 이벤트 기반 통신을 위해 Kafka를 사용하고 캐싱을 위해 Redis를 활용합니다.

## 2. 기술 스택
*   **백엔드:** Java 17, Spring Boot 3.x
*   **프론트엔드:** React 18, Vite, Tailwind CSS, Axios
*   **데이터베이스:** H2 (테스트/개발), MySQL (프로덕션용), Redis (캐싱)
*   **메시징:** Apache Kafka
*   **컨테이너화:** Docker, Docker Compose
*   **빌드 도구:** Gradle

## 3. 마이크로서비스 아키텍처

### 3.1. 인증 서비스 (`authService`)
사용자 관리 및 인증을 담당합니다.
*   **기능:**
    *   회원가입 (Sign Up)
    *   로그인 (JWT 기반)
    *   액세스 토큰 갱신 (Refresh)
    *   로그아웃 (Redis를 통한 블랙리스팅 또는 클라이언트 측 처리)
    *   이벤트 발행: `UserCreatedEvent`, `UserDeletedEvent`
*   **주요 엔드포인트:**
    *   `POST /api/auth/signup`: 새로운 사용자 계정 생성.
    *   `POST /api/auth/login`: 인증 및 JWT + 리프레시 토큰(쿠키) 발급.
    *   `POST /api/auth/refresh`: 리프레시 토큰을 사용하여 액세스 토큰 갱신.
    *   `POST /api/auth/logout`: 사용자 로그아웃.

### 3.2. 투표 서비스 (`pollService`)
투표(게시글)의 생명주기를 관리합니다.
*   **기능:**
    *   투표 생성, 조회, 수정, 삭제 (CRUD).
    *   투표 옵션 관리.
    *   **블라인드 기능:** `PATCH`를 통해 투표를 "블라인드" 처리 (결과 숨김/접근 제한).
    *   이벤트 발행: `PollCreatedEvent`, `PollDeletedEvent`, `PollBlindedEvent`.
*   **주요 엔드포인트:**
    *   `GET /api/polls`: 모든 투표 목록 조회.
    *   `POST /api/polls`: 새로운 투표 생성.
    *   `GET /api/polls/{id}`: 투표 상세 정보 조회.
    *   `PUT /api/polls/{id}`: 투표 수정.
    *   `DELETE /api/polls/{id}`: 투표 삭제.
    *   `PATCH /api/polls/{id}/blind`: 투표를 블라인드 처리 (관리자/시스템).

### 3.3. 투표 실행 서비스 (`voteService`)
투표 로직 및 결과 집계를 처리합니다.
*   **기능:**
    *   투표 하기 (중복 투표 방지 체크).
    *   투표 변경.
    *   투표 취소.
    *   **실시간 결과:** 투표 수 집계.
    *   **캐싱:** 성능 향상을 위해 Redis를 사용하여 투표 수 및 통계 캐싱.
    *   **사용자 기록:** 사용자의 투표 기록 추적.
    *   **캐시 관리:** 캐시 재구축을 위한 관리자 엔드포인트.
*   **주요 엔드포인트:**
    *   `POST /api/votes`: 투표 실행.
    *   `PUT /api/votes`: 투표 변경.
    *   `DELETE /api/votes/polls/{pollId}`: 투표 취소.
    *   `GET /api/votes/polls/{pollId}/check`: 현재 사용자의 투표 여부 확인.
    *   `GET /api/votes/polls/{pollId}/me`: 현재 사용자의 투표 내용 조회.
    *   `GET /api/votes/polls/{pollId}/results`: 투표 결과 조회 (집계됨).
    *   `GET /api/votes/polls/{pollId}/stats`: 투표 통계 조회.
    *   `GET /api/votes/users/{userId}/history`: 사용자의 투표 기록 조회.
    *   `POST /api/votes/polls/{pollId}/cache/rebuild`: 투표 캐시 재구축 (관리자).

### 3.4. 공통 모듈 (`common`)
서비스 간에 공유되는 라이브러리입니다.
*   **내용:**
    *   **보안:** `JwtUtil`, `JwtValidator`, `JwtAuthenticationFilter` 로직.
    *   **이벤트:** 정의된 Kafka 이벤트 (`UserCreatedEvent`, `PollCreatedEvent`, `VoteCreatedEvent` 등).
    *   **모델:** `BaseTimeEntity` (감사 필드: `createdAt`, `updatedAt`).
    *   **예외:** 전역 예외 처리 구조.

## 4. 프론트엔드 (`frontend`)
사용자가 시스템과 상호 작용하기 위한 웹 인터페이스입니다.
*   **프레임워크:** React (Vite)
*   **스타일링:** Tailwind CSS
*   **상태/네트워크:** API 호출을 위한 Axios.
*   **페이지:** (구조에서 추론됨)
    *   로그인 / 회원가입
    *   투표 목록
    *   투표 상세 (투표 인터페이스)
    *   사용자 프로필 / 기록

## 5. 인프라 및 구성
*   **Docker Compose:** 서비스, Kafka, Zookeeper, Redis 및 데이터베이스를 오케스트레이션합니다.
*   **게이트웨이:** (암시적 또는 직접 액세스) 서비스는 특정 포트 또는 게이트웨이(프론트엔드의 nginx.conf 존재)를 통해 노출됩니다.
*   **CI/CD:** 자동화를 위한 Jenkinsfile이 존재합니다.
