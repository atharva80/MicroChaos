# MicroChaos Backend

Java 21 control-plane backend for:

- service registry
- topology/dependency graph
- experiment creation and execution
- fault injection orchestration
- direct service control (`down`, `latency`, `recover`)
- metrics and scorecard endpoints
- live monitoring and per-service history

## Run

```bash
cd backend
./scripts/run-backend.sh
```

Backend starts on `http://localhost:8080`.

If demo services use a custom base port, start backend with the same `DEMO_BASE_PORT` so seeded service URLs match:

```bash
DEMO_BASE_PORT=9100 ./scripts/run-backend.sh
```

## Key API Endpoints

- `GET /api/health`
- `GET|POST /api/services`
- `GET|PUT|DELETE /api/services/{id}`
- `POST /api/services/{id}/faults/inject?type=DEPENDENCY_UNAVAILABLE|LATENCY|...&intensity=...`
- `POST /api/services/{id}/faults/reset`
- `GET|POST /api/topology/dependencies`
- `GET /api/topology/graph`
- `GET /api/topology/services/{id}/upstream`
- `GET /api/topology/services/{id}/downstream`
- `GET|POST /api/experiments`
- `POST /api/experiments/{id}/run`
- `POST /api/experiments/{id}/stop`
- `GET /api/runs/{id}/metrics`
- `GET /api/runs/{id}/scorecard`
- `GET /api/dashboard/overview`
- `GET /api/monitoring/overview`
- `GET /api/monitoring/services`
- `GET /api/monitoring/services/{serviceId}/history?limit=30`

## API Usage Note

This baseline uses query parameters for write endpoints (for example `POST /api/services?name=order-service&baseUrl=http://localhost:9001`) to keep the implementation dependency-free.

## Database

PostgreSQL schema and seed files:

- `db/schema.sql`
- `db/seed.sql`

The current backend implementation uses in-memory storage for fast local demo runs while preserving full SQL schema for DB migration.
