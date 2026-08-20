# VoiceBridge — Testing Documentation

This document describes the comprehensive testing strategy, test suite architecture, execution instructions, and test coverage for the VoiceBridge real-time presentation and WebRTC audio platform.

---

## 1. Test Architecture Overview

VoiceBridge employs a layered testing architecture covering unit, integration, concurrency, security, and frontend component/hook behavior:

```
                          ┌──────────────────────────────────────┐
                          │   Frontend Component & Hook Tests   │
                          │   (Vitest + React Testing Library)   │
                          └──────────────────┬───────────────────┘
                                             │
                          ┌──────────────────▼───────────────────┐
                          │   Security & Controller API Tests    │
                          │     (MockMvc + Spring Security)      │
                          └──────────────────┬───────────────────┘
                                             │
                          ┌──────────────────▼───────────────────┐
                          │   WebSocket & Real-time Isolation    │
                          │     (STOMP Client Integration)       │
                          └──────────────────┬───────────────────┘
                                             │
                          ┌──────────────────▼───────────────────┐
                          │  Service Logic & Concurrency Tests   │
                          │  (JUnit 5 + Mockito + ExecutorPool)  │
                          └──────────────────────────────────────┘
```

---

## 2. Test Suites Breakdown

### 2.1 Backend Test Suites (`backend/src/test/java/com/voicebridge`)

| Category | Test File | Target Scope | Key Verification |
| :--- | :--- | :--- | :--- |
| **Authentication** | `AuthControllerTest.java` | REST API | Register, Login, Duplicate Email 409, Invalid Credentials 401 |
| **Authentication** | `AuthServiceTest.java` | Unit | Organizer registration, password encoding, JWT generation |
| **Meeting Management** | `MeetingControllerTest.java` | REST API | Create meeting, 6-char code generation, Close meeting 200, Closed status 400 |
| **Meeting Management** | `MeetingServiceTest.java` | Unit | Code uniqueness, ownership verification, state transitions |
| **Audience Join** | `ParticipantControllerTest.java` | REST API | Token join, duplicate active name rejection, closed meeting join block |
| **Audience Join** | `ParticipantServiceTest.java` | Unit | Session token creation, participant active status, disconnect handling |
| **Presentation System** | `PresentationControllerTest.java` | REST API | File upload (PPTX/PDF), slide generation, start/stop presentation, slide change |
| **Presentation System** | `PresentationServiceTest.java` | Unit | File size limits, multi-slide processing, session isolation |
| **Speaking Queue** | `SpeakingRequestControllerTest.java` | REST API | Hand raise, single active speaker enforcement, organizer approve/end/reject |
| **Speaking Queue** | `SpeakingRequestServiceTest.java` | Unit | Hand raise state transitions, single speaker invariant |
| **Concurrency Hardening** | `SpeakingRequestConcurrencyTest.java` | Multi-Thread | Concurrent `approve(...)` calls across parallel threads enforce `APPROVED <= 1` |
| **Security Hardening** | `SecurityIntegrationTest.java` | Security | Unauthenticated API rejection 401, cross-organizer data isolation 404/403 |
| **Real-time WebSocket** | `WebSocketIntegrationTest.java` | STOMP WS | Connection handshake, `/topic/meeting/{code}` event delivery |
| **Real-time Isolation** | `WebSocketIsolationTest.java` | STOMP WS | Isolation between Meeting A and Meeting B channels |
| **WebRTC Audio** | `WebRtcSignalingTest.java` | Relay Unit | SDP offer/answer relay, ICE candidate forwarding without audio modification |

---

### 2.2 Frontend Test Suites (`frontend/src/`)

| Category | Test File | Component / Hook Scope | Key Verification |
| :--- | :--- | :--- | :--- |
| **Authentication** | `LoginPage.test.jsx` | Page Component | Header rendering, input field bindings, sign in button state |
| **Audience Join** | `AudienceJoinPage.test.jsx` | Page Component | Meeting info fetching, name input validation, join dispatch |
| **Presentation Workspace** | `PresentationViewer.test.jsx` | Component | Slide canvas rendering, thumbnail selection, fallback placeholders |
| **Organizer Control** | `MeetingControlSidebar.test.jsx` | Component | Joined audience list, speaking queue, approve/end speaker callbacks |

---

## 3. How to Run Tests

### 3.1 Backend Tests (Java / Spring Boot)

```bash
# Navigate to backend directory
cd backend

# Run full test suite with clean build
mvn clean test
```

### 3.2 Frontend Tests (Vitest + React Testing Library)

```bash
# Navigate to frontend directory
cd frontend

# Run frontend unit & component tests
npm test

# Run frontend linter
npm run lint

# Run production build validation
npm run build
```

### 3.3 Docker Infrastructure Verification

```bash
# Validate Compose configuration syntax
docker compose config
```

---

## 4. Single-Active-Speaker Invariant Verification

VoiceBridge strictly enforces that **only one participant may speak at a time**.

This business invariant is tested and verified at multiple system boundaries:
1. **Service Layer Mutual Exclusion**: `SpeakingRequestServiceImpl.approve(...)` is synchronized to prevent race conditions during high-concurrency requests.
2. **Concurrency Integration Testing**: `SpeakingRequestConcurrencyTest.java` executes multi-threaded tests using an `ExecutorService` and `CountDownLatch` where 10 threads attempt to approve speakers simultaneously. The assertion validates `approvedCount == 1`.
3. **API Validation**: `SpeakingRequestControllerTest` verifies that attempting to approve a second speaker while one is active returns HTTP `400 Bad Request`.
