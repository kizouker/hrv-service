# HRV backend — Docker & Azure readiness report

Spring Boot 3.4 · Java 21 · PostgreSQL · JWT. Goal: build with Docker and
prepare for Azure ahead of the AZ-401 beta exam.

## Analysis (what the project is)

REST API for HRV readings with JWT auth, device registry, HealthKit batch
sync, analytics (summary/trend), and educational info endpoints. Postgres in
prod, H2 in tests. Already had a basic `Dockerfile` and `docker-compose.yml`;
no Azure artifacts, no CI, and the test suite was red against its own gates.

## Delivered this session

| Area | Files |
|---|---|
| Hardened Docker build (multi-stage, cached deps, non-root, healthcheck) | `Dockerfile`, `.dockerignore` |
| Local stack (Compose v2, `.env` overrides) | `docker-compose.yml`, `.env.example` |
| Azure IaC (Container Apps + ACR + Postgres 16 + Key Vault + Log Analytics, managed identity, no stored passwords) | `infra/main.bicep`, `infra/main.parameters.json` |
| CI (build + test + 70% coverage gate + Docker validation) | `.github/workflows/ci.yml` |
| CD (OIDC → Bicep → ACR build → Container App update → smoke test) | `.github/workflows/azure-deploy.yml` |
| Study guide (manual deploy, OIDC setup, monitoring, costs, weekend checklist) | `AZURE.md` |
| Bug fixes + tests (see below) | `src/main/.../HrvService.java`, `HrvSyncController.java`, 6 test files |

## Bugs found and fixed

1. **HealthKit sync dedup was broken (real data-integrity bug).** Re-posting the
   same payload saved duplicates: nanosecond `Instant`s never match after a DB
   round-trip (Postgres/H2 keep microseconds). `measuredAt` is now truncated
   to milliseconds on write (`HrvService.create/update`) and on compare
   (`HrvSyncController`).
2. **Stale test password.** The hardening commit requires 12-char passwords but
   the auth test still used a 10-char one → 400 instead of 201. Test data
   updated; production policy untouched.

## Verification (observed, not assumed)

- `docker build` — succeeds.
- `docker compose up` — Postgres healthy, app starts,
  `/actuator/health` → `{"status":"UP"}`, Swagger serves (302 to UI).
- `az bicep build` — compiles with zero warnings/errors (12 resources).
- Workflow YAML parses; `docker compose config` valid.
- `mvn -B verify` — **34/34 tests pass, 85% line coverage, BUILD SUCCESS**
  (was: 12 tests, 1 failure, 34% coverage, BUILD FAILURE).

## Open findings → tickets

Investigation also surfaced issues intentionally left unfixed (out of scope or
needing your decision). Each is a ticket under `tickets/`, prioritized:

| Priority | Ticket | Why |
|---|---|---|
| P0 | [Auth intent for `/api/hrv/info/*`](tickets/P0-01-public-hrv-info-auth-intent.md) | Docs say public, `SecurityConfig` requires auth — your call blocks correct behavior |
| P0 | [End-to-end Azure deploy](tickets/P0-02-azure-end-to-end-deploy.md) | Bicep + pipeline unproven against real Azure until run from your terminal |
| P1 | [Password policy not enforced on change-password](tickets/P1-01-password-policy-change-password.md) | `@Valid` missing — security inconsistency vs register |
| P1 | [Expired/malformed JWT likely returns 500, not 401](tickets/P1-02-jwt-filter-401.md) | `extractUsername` throws outside the try-block; needs repro + test |
| P1 | [Local JDK is 25, project needs 21](tickets/P1-03-local-jdk-21.md) | Local `mvn`/`dev.sh` flow broken; Docker/CI unaffected |
| P1 | [`dev.sh` kills port 8080 blindly](tickets/P1-04-dev-sh-port-kill.md) | `kill -9` on any 8080 process is dangerous |
| P1 | [OIDC + CD pipeline setup](tickets/P1-05-oidc-pipeline-setup.md) | Core exam topic, manual steps required |
| P2 | [Health endpoint ignores DB state](tickets/P2-01-health-db-check.md) | Liveness probe can't see DB outages |
| P2 | [Binary blob tracked in git](tickets/P2-02-frontend-zip-hygiene.md) | `hrv-frontend-v2.zip` committed; move to releases/LFS |

## Before Monday

1. Decide P0-01 (one line in `SecurityConfig` either way).
2. Work through P0-02 — one full deploy/teardown cycle from `AZURE.md`.
3. Do P1-05 — OIDC setup is the highest-value exam exercise here.
4. Re-run the `AZURE.md` weekend checklist; re-deploy once from memory.

All files below are uncommitted — review, then commit when ready.
