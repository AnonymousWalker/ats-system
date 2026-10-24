# Resume–JD Matcher

Portfolio demo: paste or upload a resume, match it against a job description with **deterministic skill extraction** and **weighted scoring**.

Stack: **React (Vite) + Spring Boot 4 + PostgreSQL**.

## Quick start (one origin)

```bash
docker compose up --build
```

Open **http://localhost:8080**

This starts Postgres, the API, and an nginx gateway that serves the UI and proxies `/api` under the same origin.

Compose applies CPU and memory **limits** so the stack behaves like a small rented VPS (default **2 vCPU / 4 GiB** split across `postgres`, `backend`, and `web`). Copy [`.env.example`](.env.example) to `.env` to change the split. Recreate containers after changing limits: `docker compose up -d --force-recreate`.

The backend container becomes healthy only after Spring Boot reports that the application, PostgreSQL connection, and seeded skill catalog are ready.

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
| `POSTGRES_CPUS` / `MEMORY` | `0.50` / `1G` | Postgres container CPU and RAM limits |
| `BACKEND_CPUS` / `MEMORY` | `1.25` / `2816M` | API container CPU and RAM limits |
| `WEB_CPUS` / `MEMORY` | `0.25` / `256M` | Nginx container CPU and RAM limits |
| `JAVA_TOOL_OPTIONS` | `-XX:MaxRAMPercentage=75.0` | JVM heap as a percent of backend container RAM |

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

## Operations

Spring Boot Actuator exposes a deliberately small operational surface on the backend container:

| Endpoint | Purpose |
|----------|---------|
| `/actuator/health/liveness` | Confirms the application process can keep running |
| `/actuator/health/readiness` | Checks application state, PostgreSQL, and the skill catalog |
| `/actuator/prometheus` | Publishes JVM, HTTP, and datasource metrics for Prometheus |

These endpoints are reachable inside the Compose network at `http://backend:8080`. Nginx does not proxy `/actuator`, so operational data is not exposed through the public web port. Health responses omit component details in normal operation.

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
