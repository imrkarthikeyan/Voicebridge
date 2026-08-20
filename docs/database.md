# Database

PostgreSQL is the production database. Flyway owns schema migrations; Hibernate schema generation is validation-only outside focused tests.

Core tables are introduced incrementally by phase and must preserve normalized relationships between organizers, meetings, participants, speaking requests, speaker history, presentations, and presentation sessions.
