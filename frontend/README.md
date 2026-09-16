# Frontend

React + TypeScript + Vite UI for the Resume–JD Matcher API.

## Scripts

| Command | Purpose |
|---------|---------|
| `pnpm dev` | Dev server on :5173 with `/api` proxy |
| `pnpm build` | Production build to `dist/` |
| `pnpm lint` | Oxlint |
| `pnpm test:e2e` | Playwright sample journey (`PLAYWRIGHT_BASE_URL`) |

## E2E

Against the Docker one-origin stack:

```bash
# from repo root
docker compose up --build -d

cd frontend
PLAYWRIGHT_BASE_URL=http://localhost:8080 pnpm test:e2e
```
