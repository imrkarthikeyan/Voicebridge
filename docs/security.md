# VoiceBridge — Security Architecture & Guidelines

This document outlines the security architecture, authorization design, file handling safeguards, and production hardening guidelines for VoiceBridge.

---

## 1. Authentication

### Organizer Authentication
- **Mechanism**: JWT (JSON Web Token) with HMAC-SHA256 signing (`HS256`).
- **Secret Management**:
  - The secret key is loaded from `app.jwt.secret` (environment variable `JWT_SECRET`).
  - Strict startup check: Must be at least **256 bits (32 bytes)** long; fails fast otherwise.
- **Expiration**: Configurable via `app.jwt.expiration-ms` (environment variable `JWT_EXPIRATION_MS`).
- **Stateless Execution**: Validated per request in `JwtAuthenticationFilter`. Passwords are encrypted using BCrypt (`BCryptPasswordEncoder`).

### Audience Participant Authentication
- **Mechanism**: Cryptographically random UUID session tokens (`sessionToken`).
- **Scope**: Audience users join anonymously (no accounts required) via a 6-character meeting code or QR join token.
- **Privacy & Security**: Participant session tokens are assigned server-side and required for all audience operations (`raise-hand`, `me/start`, `me/stop`, `leave`).

---

## 2. Service-Level Ownership & IDOR Protection

Authentication is strictly separated from authorization:
- **Organizer Ownership**: Meeting operations (updating status, slide navigation, presentation management, approving/rejecting speakers) verify that `meeting.organizer.id == authenticatedPrincipal.id`. If an organizer attempts to modify another organizer's meeting ID or presentation ID, a `404 Not Found` exception is thrown.
- **Participant Authorization**: Audience APIs validate the participant's active `sessionToken` and confirm they only modify their own hand-raise or active speaking session.

---

## 3. WebRTC & STOMP Signaling Authorization

- **Audio Privacy**: WebRTC audio is transmitted strictly **peer-to-peer (P2P)** between the approved speaker's browser and the organizer's browser. The backend only handles STOMP signaling.
- **Signaling Guardrails**:
  - `WebRtcSignalingController` validates message structure, ensuring `type` is one of `OFFER`, `ANSWER`, or `ICE_CANDIDATE`.
  - Only allowed roles (`ORGANIZER` and `SPEAKER`) can send signaling messages.
  - Signaling messages for closed or nonexistent meetings are immediately dropped.

---

## 4. File Upload & Path Traversal Safeguards

- **Storage Isolation**: Presentations are saved in isolated directories created with server-generated UUIDs (`uploads/presentations/{uuid}`).
- **Path Traversal Protection**:
  - Filenames are sanitized with `Paths.get(filename).getFileName()`.
  - Target storage paths are normalized and checked: `targetPath.normalize().startsWith(presentationDir)`. Attempts to escape the directory via `../` or `..\\` fail fast with a security exception.
- **File Format & Size Limits**: Strict 50MB file size limit enforced server-side. File extensions are restricted strictly to `.pptx` and `.pdf`.

---

## 5. Rate Limiting & Denial of Service Protection

- **In-Memory Rate Limiting**: `RateLimitInterceptor` monitors incoming IP addresses over a 60-second sliding window:
  - **Login / Register**: Max 15 requests/minute per IP.
  - **Audience Join**: Max 30 joins/minute per IP.
  - **Hand-Raise**: Max 40 requests/minute per IP.
- Violations return an explicit `HTTP 429 Too Many Requests` JSON response (`RATE_LIMIT_EXCEEDED`).

---

## 6. HTTP Security Headers & CORS

- **Security Headers**:
  - `X-Frame-Options: SAMEORIGIN` (mitigates clickjacking attacks).
  - `X-Content-Type-Options: nosniff` (prevents MIME sniffing).
  - `Referrer-Policy: strict-origin-when-cross-origin`.
- **CORS Hardening**: Configured via `CORS_ALLOWED_ORIGINS` (defaults to `http://localhost,http://localhost:5173`). Wildcard `*` with credentials enabled is disabled in production settings.

---

## 7. Production HTTPS Requirement

> [!IMPORTANT]
> **Production WebRTC Microphone Access**: Web browsers enforce secure context policies for `navigator.mediaDevices.getUserMedia()`. Production deployments **MUST** serve VoiceBridge over **HTTPS** with valid SSL/TLS certificates (e.g. Let's Encrypt). Development on `localhost` is exempt by browser vendor policies.
