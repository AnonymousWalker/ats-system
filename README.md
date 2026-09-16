# Resume–JD Matcher

Portfolio demo: paste or upload a resume, match it against a job description with **deterministic skill extraction** and **weighted scoring**.

Stack: **React (Vite) + Spring Boot 4 + PostgreSQL**.

## Quick start (one origin)

```bash
docker compose up --build
```

Open **http://localhost:8080**

This starts Postgres, the API, and an nginx gateway that serves the UI and proxies `/api` under the same origin.

## Local development

### Backend only (+ Docker Postgres)

```bash
docker compose up -d postgres
export JAVA_HOME=...   # Java 21
./mvnw spring-boot:run
```

API: http://localhost:8080

### Frontend (Vite)

```bash
cd frontend
pnpm install
pnpm dev
```

UI: http://localhost:5173 (proxies `/api` → backend)

## Environment variables

See [`.env.example`](.env.example). Spring Boot also reads:

| Variable | Default | Purpose |
|----------|---------|---------|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/ats` | JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | `ats` | DB user |
| `SPRING_DATASOURCE_PASSWORD` | `ats` | DB password |
| `ATS_CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | Allowed browser origins (comma-separated) |
| `ATS_RATE_LIMIT_CREATES_PER_MINUTE` | `30` | Create-endpoint rate limit per IP |
| `POSTGRES_DB` / `USER` / `PASSWORD` | `ats` | Compose Postgres settings |
| `WEB_PORT` | `8080` | Host port for the nginx gateway |
| `PLAYWRIGHT_BASE_URL` | `http://localhost:8080` | E2E base URL |

## Tests

```bash
# Backend (Testcontainers Postgres)
./mvnw test

# Frontend unit/build checks
cd frontend && pnpm lint && pnpm build

# Browser journey against the containerized stack
docker compose up --build -d
cd frontend && pnpm test:e2e
```

## Project layout

```
src/                 Spring Boot API
frontend/            React + Vite UI (+ Playwright e2e/)
Dockerfile           Backend image
frontend/Dockerfile  UI image (nginx)
docker-compose.yml   Postgres + API + one-origin gateway
```

## Demo flow

1. Use sample pair (or paste/upload a resume + JD)
2. Extract skills
3. Correct Required/Preferred labels
4. Calculate score
5. Delete analysis when finished (temporary public demo data)
