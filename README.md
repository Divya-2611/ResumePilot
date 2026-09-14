# ResumePilot

A production-grade full-stack web app that optimizes resumes against job descriptions using AI.
Upload a resume (PDF/DOCX), paste a job description, and get an AI-optimized, ATS-friendly resume
with a keyword match score, side-by-side diff, version history, and PDF/DOCX download.

- **Frontend:** React 18, Vite, Tailwind CSS, React Router, React Hook Form, Axios, Context API
- **Backend:** Java 21, Spring Boot 3.3, Spring Security (JWT), Spring Data JPA, PostgreSQL (Neon), Apache POI / PDFBox / OpenPDF
- **AI providers (swappable via env):** OpenAI | Gemini | Claude — with deterministic local fallback so the app works without an API key

## Features

- Email + OTP signup verification, password reset, JWT access (15 min) + rotating refresh tokens (7 days) stored hashed
- Resume upload (PDF/DOCX), text extraction, and management
- Job description paste/upload with skills, keywords, and requirements extraction
- AI resume optimization with side-by-side diff, ATS keyword analysis, and score gauge
- Optimized resume download as PDF or DOCX; full optimization history
- Role-based access (USER / ADMIN) with an admin panel (user list, promote/demote, delete), change password, dark/light mode, toasts, skeletons, pagination + search

## Project Layout

```
ResumePilot/
├── backend/                 # Spring Boot REST API
├── frontend/                # React SPA (Vite)
└── database/schema.sql      # PostgreSQL production DDL (reference)
```

## Prerequisites

- Java 21+ (JDK 22+ requires Lombok ≥ 1.18.46, already pinned)
- Maven 3.9+
- Node 20+ / npm
- Docker (for the containerized stack)
- A Neon PostgreSQL instance — connection details are in `backend/.env` (the only database used)

## Backend Setup

```bash
cd backend
mvn clean package          # compile + run 24 unit/context tests
./run.sh                   # loads .env (Neon DB) and starts on http://localhost:8080
```

The app auto-creates the schema on Neon (`ddl-auto=update`).
A default admin is seeded on first boot, with credentials taken from `ADMIN_EMAIL` / `ADMIN_PASSWORD`
(no defaults — set them before going live).

### Environment Variables

| Variable | Default | Description |
|---|---|---|
| `DB_HOST` / `DB_PORT` / `DB_NAME` | localhost / `5432` / `resume_optimizer` | PostgreSQL connection (Neon in production) |
| `DB_USERNAME` / `DB_PASSWORD` | `postgres` / `postgres` | PostgreSQL credentials |
| `DB_SSLMODE` | `require` | `disable` \| `require` (Neon needs `require`) |
| `JWT_SECRET` | dev default (change in prod) | 256-bit JWT signing secret |
| `JWT_ACCESS_EXPIRATION_MS` | `900000` | access token TTL (15 min) |
| `JWT_REFRESH_EXPIRATION_MS` | `604800000` | refresh token TTL (7 days) |
| `AI_PROVIDER` | `OPENAI` | `OPENAI` \| `GEMINI` \| `CLAUDE` |
| `OPENAI_API_KEY` / `OPENAI_MODEL` | — / `gpt-4o-mini` | OpenAI credentials |
| `GEMINI_API_KEY` / `GEMINI_MODEL` | — / `gemini-2.5-flash` | Gemini credentials |
| `ANTHROPIC_API_KEY` / `CLAUDE_MODEL` | — / `claude-3-5-sonnet-20241022` | Claude credentials |
| `MAIL_MODE` | `api` | `api` = Brevo transactional HTTP API (recommended); `smtp` = SMTP relay mode |
| `BREVO_API_KEY` | — | Brevo API key (`xkeysib-...`) — required for `MAIL_MODE=api` |
| `MAIL_FROM` | `spring.mail.username` | Verified Brevo sender address |
| `MAIL_HOST` / `MAIL_PORT` / `MAIL_SSL` / `MAIL_STARTTLS` | `smtp-relay.brevo.com` / `465` / `true` / `false` | SMTP relay settings (`MAIL_MODE=smtp` only) |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | — | SMTP login + key (`MAIL_MODE=smtp` only) |
| `UPLOAD_DIR` | `./uploads` | Local file storage root (mode `local` only) |
| `STORAGE_MODE` | `db` | `db` = file bytes stored in `stored_files` table (default, survives restarts); `local` = files on disk |
| `MIGRATE_LOCAL_FILES` | `false` | Set `true` once to migrate legacy `uploads/` files into DB storage |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://localhost:3000` (+ 127.0.0.1 variants) | Comma-separated origins |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` | — (set before first boot) | Seeded admin account |

> Without an AI key, optimization falls back to a built-in local analyzer (keyword match, readability,
> action verbs, section scoring) — great for development.

> Without mail credentials, OTP codes are logged to the backend console so you can test signup locally.

## Frontend Setup

```bash
cd frontend
npm install
npm run dev                # starts on http://localhost:5173 (proxies /api to :8080)
npm run build              # production build
npm test                   # vitest unit tests
```

`VITE_API_BASE_URL` defaults to `/api/v1` (same-origin). In dev, Vite proxies `/api` to `:8080`;
in the Docker image, Spring Boot serves both the built site and the API on the same port —
so no CORS in either setup.

## Docker / Deployment

One multi-stage `Dockerfile` produces a single image containing everything: the React site is
embedded inside the Spring Boot jar (no separate Nginx container). It always connects to the
Neon PostgreSQL instance configured in `backend/.env`:

```bash
docker compose up -d --build   # builds + starts the app
# website + API: http://localhost:8080 (single service)
docker compose logs -f backend # watch logs
docker compose down            # stop
```

Alternatively, run the JVM version directly from `backend/` with `./run.sh` (loads `.env` too and
starts on :8080 — same Neon DB).

**Deploying on Render:** the app is deployed as a Docker web service on Render (Singapore
region), created from the Render dashboard (`New +` -> `Web Service` -> this repo -> Docker ->
Singapore). All secrets are set as service environment variables — nothing sensitive lives in
the repo. Health checks run on `/actuator/health` (kept warm 24/7 by an UptimeRobot monitor,
which also prevents the Neon compute from suspending).

**The one rule:** run the app next to its database. Neon is serverless PostgreSQL — this stack
runs both in Singapore (ap-southeast-1), so queries stay in the same region and respond in
milliseconds, even from India.

## API Overview (all under `/api/v1`)

| Area | Endpoints |
|---|---|
| Auth | `POST /auth/register`, `/auth/verify-otp`, `/auth/resend-otp`, `/auth/login`, `/auth/refresh`, `/auth/logout`, `/auth/forgot-password`, `/auth/verify-reset-otp`, `/auth/reset-password` |
| Users | `GET /users/profile`, `PUT /users/profile` (first/last name only — email is immutable after registration), `PUT /users/change-password`, `DELETE /users/profile` |
| Resumes | `POST /resumes/upload`, `POST /resumes/create`, `GET /resumes/all`, `GET /resumes/{id}`, `PUT /resumes/update/{id}`, `DELETE /resumes/delete/{id}`, `POST /resumes/duplicate/{id}`, `POST /resumes/favorite/{id}` |
| Versions | `GET /resumes/{id}/versions`, `POST /resumes/save`, `POST /resumes/{id}/restore/{versionId}`, `POST /resumes/versions/{versionId}/favorite`, `PUT /resumes/{resumeId}/versions/{versionId}/rename`, `DELETE /resumes/{resumeId}/versions/{versionId}` |
| Job Descriptions | `POST /jobs/paste-jd`, `POST /jobs/upload-jd` |
| Optimization | `POST /optimize` (resumeId + job description) |
| Analytics | `GET /dashboard`, `GET /history`, `GET /history/export`, `GET /download` |
| Admin | `GET /admin/users`, `PATCH /admin/users/{id}/role`, `DELETE /admin/users/{id}` |

Responses use a unified `ApiResponse` wrapper: `{ "success": true, "message": "...", "data": ... }`.

## Tests

- **Backend (24 tests):** JWT generation/parsing, auth flows (register → OTP → login → refresh rotation),
  ATS scoring, keyword extraction, rate limiting, and a full Spring context + security smoke test
  (`ApplicationContextTest`, runs on H2).
- **Frontend (5 tests):** formatters, validators, and keyword-highlighting helpers (Vitest + jsdom).

## Notes

- `database/schema.sql` is the reference production DDL; dev uses Hibernate `ddl-auto=update`.
- Refresh tokens are stored as SHA-256 hashes and rotated on every use (old tokens are revoked).
- Files (resumes and JD files) are stored as BLOBs in the `stored_files`
  table (`STORAGE_MODE=db`, the default). References in the DB look like `db://{id}`.
  Set `STORAGE_MODE=local` to fall back to `uploads/{userId}/...` on disk.
- Production checklist: set a strong `JWT_SECRET`, real mail credentials, an AI API key,
  switch `JPA_DDL_AUTO=validate` , and run behind HTTPS.
