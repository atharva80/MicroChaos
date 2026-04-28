# MicroChaos Backend

Java 21 backend control plane for MicroChaos with PostgreSQL JDBC persistence.

## Prerequisites

- Java 21
- Maven 3.6+
- PostgreSQL running in Docker

## Start PostgreSQL (if not running)

```bash
cd /mnt/d/MicroChaos
docker-compose up -d postgres
```

## Run

```bash
cd /mnt/d/MicroChaos/backend
mvn compile
mvn exec:java -Dexec.mainClass="com.microchaos.backend.MicroChaosBackendApplication"
```

Verify:
```bash
curl http://localhost:8080/api/health
```

## Backend URLs

- Base: `http://localhost:8080`
- API: `http://localhost:8080/api`

If demo services use a custom base port, start the backend with the same `DEMO_BASE_PORT`:

```bash
cd /mnt/d/MicroChaos/backend
DEMO_BASE_PORT=9100 mvn exec:java -Dexec.mainClass="com.microchaos.backend.MicroChaosBackendApplication"
```

## Persistence

Stored in PostgreSQL:

- services
- service dependencies
- experiments
- experiment runs
- metric snapshots

Still in memory:

- monitoring history
- remediation policy/rule/execution state

## API Usage Note

Write endpoints use query parameters, for example:

```text
POST /api/services?name=order-service&baseUrl=http://localhost:9001
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
- `GET /api/runs`
- `GET /api/runs/{id}`
- `GET /api/runs/{id}/metrics`
- `GET /api/runs/{id}/scorecard`
- `GET /api/dashboard/overview`
- `GET /api/monitoring/overview`
- `GET /api/monitoring/services`
- `GET /api/monitoring/services/{serviceId}/history?limit=30`

## Database Files

- `db/schema.sql`
- `db/seed.sql`

For full project run steps and SQL inspection examples, see the repo root [README.md](/d:/MicroChaos/README.md).
