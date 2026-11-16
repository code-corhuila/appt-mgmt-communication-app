# appt-mgmt-communication-app

Standalone Android chat client for the Salud Activa appointment-management project.
It lets an authenticated patient view their medical chat history and send messages
to the care team through the existing backend API.

## Scope

- Sign in through `POST /api/auth/login`.
- Load the authenticated user's messages through `GET /api/chat`.
- Send patient messages through `POST /api/chat`.
- Keep the JWT in memory only; signing out or closing the app ends the local session.
- Use HTTPS for all API traffic. The client does not generate medical advice or
  substitute for emergency care.

The backend owns message persistence, doctor replies, and notifications. The app
does not fabricate doctor responses.

## Requirements

- JDK 17
- Android SDK Platform 35
- Gradle 8.10.2

## Configure and run

The default API base URL is the Salud Activa deployment. To override it, set
`COMMUNICATION_API_BASE_URL` before invoking Gradle:

```powershell
$env:COMMUNICATION_API_BASE_URL = "https://your-api.example/"
gradle testDebugUnitTest assembleDebug
```

The value must be an HTTPS base URL ending in `/`. No credentials or signing
secrets belong in this repository. Open the project in Android Studio to install
the debug APK on an emulator or device.

## Project layout

```text
app/
  src/main/                 Android client and API integration
  src/test/                 Unit tests
adrs/                       Architecture decisions
.github/workflows/          Build and test checks
```

## Delivery flow

Create a task branch named `hu-<id>-dev` and open a pull request to `develop`.
Promote the reviewed change through pull requests to `qa` and then `main`; do not
push directly to permanent branches. Record each member's own evidence in their
fork under `NN-week/hu-status/` and keep the profile repository's `CONFIG` in
place so automated attribution can identify the contributor.
