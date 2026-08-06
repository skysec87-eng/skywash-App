# Implementation Plan: skyWash Backend API (Java Spring Boot)

See Notion: https://app.notion.com/p/3b4054937ea2816ea97af8a2a0d4c649

## Status: Implemented (local)

Spring Boot API under `backend/`, running on `:8080`.

| Phase | Status |
|---|---|
| Scaffold + health + CORS | Done |
| Services / partners / nearby / quote / promo / geocode | Done |
| Orders CRUD + cancel + rate + auto progression | Done |
| Auth / payments / chat / cities stubs | Done |
| Docs | Done |

## Run

```bash
export JAVA_HOME="/opt/homebrew/opt/openjdk@21"
export PATH="$JAVA_HOME/bin:$PATH"
cd backend && mvn -DskipTests package && java -jar target/skywash-api-0.1.0.jar
```

Deploy API separately from Vercel frontend (Railway/Render/Fly).
