# HRV Backend API

Heart Rate Variability REST API — Spring Boot 3.4 · Java 21 · PostgreSQL · JWT auth · Swagger UI

## Quick Start (IntelliJ)

1. **Open project**: `File → Open` → select `pom.xml` → "Open as Project"
2. **Start PostgreSQL** (or use Docker: `docker run -e POSTGRES_DB=hrvdb -e POSTGRES_PASSWORD=postgres -p 5432:5432 postgres:16`)
3. **Run config**: Right-click `HrvBackendApplication` → Run
   4. **Swagger UI**: http://localhost:8080/swagger-ui.html

## Environment Variables

| Variable            | Default                        | Description              |
|---------------------|--------------------------------|--------------------------|
| `DATABASE_URL`      | `jdbc:postgresql://...`        | PostgreSQL JDBC URL      |
| `DATABASE_USERNAME` | `postgres`                     | DB username              |
| `DATABASE_PASSWORD` | `postgres`                     | DB password              |
| `JWT_SECRET`        | (change this!)                 | Base64 256-bit secret    |
| `JWT_EXPIRATION_MS` | `86400000`                     | Token lifetime (24h)     |
| `PORT`              | `8080`                         | Server port              |

## Deploy to Railway (free — CV link)

1. Push this repo to GitHub
2. Go to [railway.app](https://railway.app) → New Project → Deploy from GitHub
3. Add a **PostgreSQL** plugin (one click)
4. Railway auto-sets `DATABASE_URL` — just add `JWT_SECRET` in Variables
5. Your app gets a public HTTPS URL like `https://hrv-backend-production.up.railway.app`

## API Overview

| Method | Path                          | Description                    |
|--------|-------------------------------|--------------------------------|
| POST   | `/api/auth/register`          | Register new user              |
| POST   | `/api/auth/login`             | Login → JWT token              |
| POST   | `/api/hrv`                    | Submit HRV reading             |
| GET    | `/api/hrv`                    | List all readings              |
| GET    | `/api/hrv/{id}`               | Get reading by ID              |
| PUT    | `/api/hrv/{id}`               | Update reading                 |
| DELETE | `/api/hrv/{id}`               | Delete reading                 |
| GET    | `/api/hrv/analytics/summary`  | Aggregated stats               |
| GET    | `/api/hrv/analytics/trend`    | Daily averages (chart data)    |
| POST   | `/api/devices`                | Register a device              |
| GET    | `/api/devices`                | List devices                   |
| DELETE | `/api/devices/{id}`           | Remove device                  |

## HRV Metrics Tracked

- **RMSSD** — Root Mean Square of Successive Differences (ms) — most important
- **SDNN** — Std deviation of NN intervals (ms) — overall HRV
- **pNN50** — % successive NN intervals differing >50 ms — parasympathetic activity
- **Mean RR** — Mean RR interval (ms)
- **Mean HR** — Mean heart rate (bpm)
- **LF/HF Power** — Frequency domain analysis (optional)

## iOS / watchOS Integration

**Sync endpoint:** `POST /api/sync/healthkit`

```swift
// Swift — send HealthKit samples to the API
struct HrvReadingRequest: Encodable {
    let measuredAt: Date
    let rmssd: Double?
    let sdnn: Double?
    let meanHr: Double?
    // ... other fields
}

struct HealthKitSyncRequest: Encodable {
    let sourceName: String
    let appleDeviceId: String
    let readings: [HrvReadingRequest]
}

// POST to /api/sync/healthkit with Bearer token
// Response: { "received": 12, "saved": 10, "skippedDuplicates": 2 }
```

**Check last sync anchor:** `GET /api/sync/status`

## Running Tests + Coverage

```bash
mvn verify
# HTML coverage report → target/site/jacoco/index.html
# Minimum 70% line coverage enforced
```

## Docker

```bash
# Start everything (app + PostgreSQL)
docker compose up --build

# API: http://localhost:8080
# Swagger: http://localhost:8080/swagger-ui.html
```

## Deploy to Railway (free HTTPS URL for CV)

```bash
# 1. Push to GitHub
git init && git add . && git commit -m "Initial commit"
gh repo create hrv-backend --public --push

# 2. railway.app → New Project → Deploy from GitHub repo
# 3. Add PostgreSQL plugin (one click)
# 4. Set JWT_SECRET in Variables tab
# 5. Your URL: https://hrv-backend-production.up.railway.app
```
