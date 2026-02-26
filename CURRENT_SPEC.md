# Current Implementation Specification (EveryPoll)

This document outlines the current state of the EveryPoll project, detailing the implemented architecture, services, features, and technical stack.

## 1. Project Overview
EveryPoll is a microservices-based polling application designed for scalability and real-time interaction. It consists of separate services for authentication, poll management, and voting, utilizing Kafka for event-driven communication and Redis for caching.

## 2. Technical Stack
*   **Backend:** Java 17, Spring Boot 3.x
*   **Frontend:** React 18, Vite, Tailwind CSS, Axios
*   **Database:** H2 (Test/Dev), MySQL (Production ready), Redis (Caching)
*   **Messaging:** Apache Kafka
*   **Containerization:** Docker, Docker Compose
*   **Build Tool:** Gradle

## 3. Microservices Architecture

### 3.1. Auth Service (`authService`)
Responsible for user management and authentication.
*   **Features:**
    *   User Registration (Sign Up)
    *   User Login (JWT based)
    *   Access Token Refresh
    *   Logout (Blacklisting via Redis likely, or simply client-side)
    *   Event Publishing: `UserCreatedEvent`, `UserDeletedEvent`
*   **Key Endpoints:**
    *   `POST /api/auth/signup`: Create a new user account.
    *   `POST /api/auth/login`: Authenticate and receive JWT + Refresh Token (Cookie).
    *   `POST /api/auth/refresh`: Refresh access token using refresh token.
    *   `POST /api/auth/logout`: Log out the user.

### 3.2. Poll Service (`pollService`)
Manages the lifecycle of polls.
*   **Features:**
    *   Create, Read, Update, Delete (CRUD) for Polls.
    *   Poll Options Management.
    *   **Blind Feature:** Ability to "blind" a poll (hide results/access) via `PATCH`.
    *   Event Publishing: `PollCreatedEvent`, `PollDeletedEvent`, `PollBlindedEvent`.
*   **Key Endpoints:**
    *   `GET /api/polls`: List all polls.
    *   `POST /api/polls`: Create a new poll.
    *   `GET /api/polls/{id}`: Get poll details.
    *   `PUT /api/polls/{id}`: Update a poll.
    *   `DELETE /api/polls/{id}`: Delete a poll.
    *   `PATCH /api/polls/{id}/blind`: Mark a poll as blinded (Admin/System).

### 3.3. Vote Service (`voteService`)
Handles voting logic and result aggregation.
*   **Features:**
    *   Cast Vote (prevents double voting via checks).
    *   Change Vote.
    *   Cancel Vote.
    *   **Real-time Results:** Aggregates vote counts.
    *   **Caching:** Uses Redis to cache vote counts and stats for performance.
    *   **User History:** Tracks voting history for users.
    *   **Cache Management:** Admin endpoint to rebuild cache.
*   **Key Endpoints:**
    *   `POST /api/votes`: Cast a vote.
    *   `PUT /api/votes`: Change a vote.
    *   `DELETE /api/votes/polls/{pollId}`: Cancel a vote.
    *   `GET /api/votes/polls/{pollId}/check`: Check if current user voted.
    *   `GET /api/votes/polls/{pollId}/me`: Get current user's vote.
    *   `GET /api/votes/polls/{pollId}/results`: Get vote results (aggregated).
    *   `GET /api/votes/polls/{pollId}/stats`: Get voting statistics.
    *   `GET /api/votes/users/{userId}/history`: Get voting history for a user.
    *   `POST /api/votes/polls/{pollId}/cache/rebuild`: Rebuild vote cache (Admin).

### 3.4. Common Module (`common`)
Shared library used across services.
*   **Contents:**
    *   **Security:** `JwtUtil`, `JwtValidator`, `JwtAuthenticationFilter` logic.
    *   **Events:** Defined Kafka events (`UserCreatedEvent`, `PollCreatedEvent`, `VoteCreatedEvent`, etc.).
    *   **Models:** `BaseTimeEntity` (Audit fields: `createdAt`, `updatedAt`).
    *   **Exceptions:** Global exception handling structures.

## 4. Frontend (`frontend`)
Web interface for users to interact with the system.
*   **Framework:** React (Vite)
*   **Styling:** Tailwind CSS
*   **State/Network:** Axios for API calls.
*   **Pages:** (Inferred from structure)
    *   Login / Signup
    *   Poll List
    *   Poll Detail (Voting Interface)
    *   User Profile / History

## 5. Infrastructure & Configuration
*   **Docker Compose:** Orchestrates services, Kafka, Zookeeper, Redis, and Databases.
*   **Gateway:** (Implied or direct access) Services are exposed via specific ports or a gateway (nginx.conf present in frontend).
*   **CI/CD:** Jenkinsfile present for automation.
