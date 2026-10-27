## Stack

- `backend/`: Spring Boot 4 + Kotlin (Java 21, Gradle Kotlin DSL), PostgreSQL with Flyway. Serves the API, the redirect, and the built frontend.
- `frontend/`: Vite + React + TypeScript SPA.
- Deployed as one Docker image behind Caddy via `docker compose`. The current spec is `.scratch/url-shortener-v1/spec.md`.

## Local development

- Java is not on PATH. Run Gradle with `JAVA_HOME` set to `~/.jdks/graalvm-jdk-21.0.7`.
- Backend tests use Testcontainers, so Docker must be running.
- For running the app by hand, start Postgres with `docker compose -f compose.dev.yaml up -d`, then `bootRun` the backend (port 8080) and `npm run dev` the frontend (port 5173, proxies `/api` and numeric paths to the backend).
- Production runs from `compose.yaml` + `Caddyfile` (env from `.env`, see `.env.example`); deploy steps are in `docs/deploy.md`. `compose.dev.yaml` is only the local dev database.

## Agent skills

### Issue tracker

Issues and specs are tracked as local markdown files under `.scratch/<feature-slug>/`. See `docs/agents/issue-tracker.md`.

### Triage labels

Uses the five default triage roles (`needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix`), recorded in a `Status:` line. See `docs/agents/triage-labels.md`.

### Domain docs

Single-context: one `GLOSSARY.md` and `docs/adr/` at the repo root. See `docs/agents/domain.md`.
