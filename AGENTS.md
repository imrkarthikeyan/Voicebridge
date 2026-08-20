# VoiceBridge Agent Rules

Build VoiceBridge module by module. Do not implement later phases until the current phase has compiled, tested, and been reported.

Phase order:
1. Repository setup
2. Backend foundation
3. Authentication
4. Meeting management
5. Audience joining
6. WebSocket/STOMP
7. Speaking queue
8. WebRTC
9. Presentation system
10. Presentation and VoiceBridge integration
11. Frontend polish
12. Security hardening
13. Testing
14. Docker verification
15. Production readiness

Non-negotiable product rules:
- Only one participant may speak at a time.
- There is no speaking timer.
- Audience users do not need accounts.
- QR/token joins must not expose internal IDs.
- WebRTC carries audio directly from audience browser to organizer browser.
- Spring Boot is only the signaling and API layer for audio sessions.
- Closed meetings cannot accept joins, requests, or signaling.
- Duplicate participant names are not allowed within a meeting.
- Duplicate active speaking requests are not allowed.
- Presentation controls and VoiceBridge controls must coexist in the organizer workspace.

Engineering rules:
- Do not expose JPA entities through REST APIs.
- Keep controllers thin and business logic in services.
- Use validation, structured errors, logging, and tests for every implemented feature.
- Do not commit secrets. Use `.env.example` for documented variables.
- Do not leave TODO, FIXME, fake production code, empty service methods, or placeholder implementations.
- After each phase, run backend compile/tests and frontend lint/build/tests where scripts exist.
