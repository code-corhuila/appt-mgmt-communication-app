# ADR-001: Remote medical chat client

- Status: Accepted
- Date: 2026-11-15

## Context

Salud Activa already exposes authenticated chat endpoints in its backend. The
communication app must provide a focused mobile client without duplicating
message persistence, doctor replies, or notification delivery.

## Decision

- Implement the client as a standalone native Android application in Kotlin.
- Authenticate with `POST /api/auth/login` and use the returned bearer token for
  `GET /api/chat` and `POST /api/chat`.
- Require HTTPS, keep the token in memory, and never log credentials or tokens.
- Treat the backend as the source of truth for conversation history and replies.

## Consequences

- The app requires an active network connection and a compatible Salud Activa API.
- A new app process requires the patient to sign in again.
- Backend changes to the authentication or chat response contracts must be
  reflected in this client and its tests.
