# MicroChaos

MicroChaos is a chaos-engineering demo platform for a Java microservice stack. It includes:

- a Java backend control plane in `backend/`
- PostgreSQL persistence in Docker
- a browser frontend in `frontend/`
- a Java Swing desktop frontend in `frontend-swing/`
- demo microservices in `demo-services/`

## Quick Start

Follow these steps to run the complete project:

### Step 1: Start PostgreSQL (in WSL)

```bash
cd /mnt/d/MicroChaos
docker-compose up -d postgres
```

### Step 2: Start Backend (in WSL)

```bash
cd /mnt/d/MicroChaos/backend
mvn compile
mvn exec:java -Dexec.mainClass="com.microchaos.backend.MicroChaosBackendApplication"
```

Verify backend is running:
```bash
curl http://localhost:8080/api/health
```

### Step 3: Start Swing Frontend (in Windows PowerShell)

```powershell
cd D:\MicroChaos\frontend-swing
mvn clean compile exec:java -Dexec.mainClass=com.microchaos.swing.MicroChaosSwingApp -Dapi.base=http://localhost:8080/api
```

---

## Project URLs

| Component | URL | Port |
|-----------|-----|------|
| Backend API | http://localhost:8080/api | 8080 |
| PostgreSQL | localhost:5432/microchaos | 5432 |
| Swing UI | (desktop app) | — |

## Architecture

```text
Frontend (browser or Swing)
-> Backend API (http://localhost:8080/api)
-> PostgreSQL
```

The frontend does not write to PostgreSQL directly. When you add or update data in the UI:

1. the UI sends an HTTP request to the backend
2. the backend processes the request
3. the backend writes to PostgreSQL
4. later refreshes read the data back through the backend API

## What Is Persisted

These backend areas are stored in PostgreSQL:

- services -> `target_services`
- service dependencies -> `service_dependencies`
- experiments -> `experiments`
- experiment runs -> `experiment_runs`
- generated metrics -> `metric_snapshots`

These areas are still in memory only:

- monitoring history
- remediation policies, rules, and execution history

## Prerequisites

- Java 21
- Maven 3.6+
- Docker / Docker Compose
- WSL for backend and Docker workflow
- Windows PowerShell for the Swing UI if WSL has no GUI support

## Project Structure

- `backend/`: Java backend API and JDBC persistence
- `backend/db/schema.sql`: PostgreSQL schema
- `backend/db/seed.sql`: seed data
- `frontend/`: browser UI
- `frontend-swing/`: Java Swing desktop UI
- `demo-services/`: sample microservices used by the platform
- `docker-compose.yml`: PostgreSQL service definition

## First-Time Database Setup

From WSL:

```bash
cd /mnt/d/MicroChaos
docker-compose up -d postgres
docker-compose exec postgres psql -U microchaos -d microchaos -f /docker-entrypoint-initdb.d/10-schema.sql
docker-compose exec postgres psql -U microchaos -d microchaos -f /docker-entrypoint-initdb.d/20-seed.sql
```

Database connection settings:

- URL: `jdbc:postgresql://localhost:5432/microchaos`
- user: `microchaos`
- password: `microchaos`

## Recommended Start Order

Use this order when running the full project:

1. start PostgreSQL
2. start demo services
3. start the backend
4. start either the browser frontend or the Swing frontend

## Start PostgreSQL

From WSL:

```bash
cd /mnt/d/MicroChaos
docker-compose up -d postgres
```

## Start Demo Services

From WSL:

```bash
cd /mnt/d/MicroChaos/demo-services
./scripts/run-demo-stack.sh
```

If ports `9000-9005` are busy:

```bash
cd /mnt/d/MicroChaos/demo-services
DEMO_BASE_PORT=9100 ./scripts/run-demo-stack.sh
```

If you use a custom `DEMO_BASE_PORT`, use the same value when starting the backend.

## Start Backend

From WSL:

```bash
cd /mnt/d/MicroChaos/backend
mvn compile
mvn exec:java -Dexec.mainClass="com.microchaos.backend.MicroChaosBackendApplication"
```

Expected startup lines:

- `[DB] Connection status: CONNECTED`
- `MicroChaos backend running on http://localhost:8080`

If demo services use a custom base port:

```bash
cd /mnt/d/MicroChaos/backend
DEMO_BASE_PORT=9100 mvn exec:java -Dexec.mainClass="com.microchaos.backend.MicroChaosBackendApplication"
```

## Verify Backend

From a new WSL terminal:

```bash
curl http://localhost:8080/api/health
```

Expected response:

```json
{"status":"ok"}
```

## Start Browser Frontend

From WSL:

```bash
cd /mnt/d/MicroChaos/frontend
python3 -m http.server 5173
```

Open:

- `http://localhost:5173`

If the backend runs on a custom port, open:

- `http://localhost:5173/?api=http://localhost:18080/api`

## Start Swing Frontend

If WSL does not have GUI support, run the Swing frontend from Windows PowerShell while keeping the backend running in WSL.

From Windows PowerShell:

```powershell
cd D:\MicroChaos\frontend-swing
mvn --% clean compile exec:java -Dexec.mainClass=com.microchaos.swing.MicroChaosSwingApp -Dapi.base=http://localhost:8080/api
```

Notes:

- `--%` is needed so PowerShell does not misparse `-Dapi.base=...`
- if you run Swing inside WSL without GUI support, you may get `No X11 DISPLAY variable was set`

## Useful Backend API Endpoints

- `GET /api/health`
- `GET|POST /api/services`
- `GET|PUT|DELETE /api/services/{id}`
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
- `GET /api/monitoring/services`

## Where UI Data Is Stored

When you use the UI, data is stored in PostgreSQL tables like these:

- adding a service -> `target_services`
- adding a dependency -> `service_dependencies`
- creating an experiment -> `experiments`
- running an experiment -> `experiment_runs`
- generated run metrics -> `metric_snapshots`

So if you add a service in the UI, the new row should appear in `target_services`.

## View The Database In SQL

Open PostgreSQL from WSL:

```bash
cd /mnt/d/MicroChaos
docker-compose exec postgres psql -U microchaos -d microchaos
```

Useful `psql` commands:

```sql
\dt
```

```sql
SELECT id, project_id, name, base_url, environment, status
FROM target_services
ORDER BY id;
```

```sql
SELECT id, source_service_id, target_service_id, dependency_type, protocol, communication_mode
FROM service_dependencies
ORDER BY id;
```

```sql
SELECT id, name, target_service_id, fault_type, status, created_at
FROM experiments
ORDER BY id;
```

```sql
SELECT id, experiment_id, status, mttr_seconds, resilience_score, started_at, ended_at
FROM experiment_runs
ORDER BY id;
```

```sql
SELECT id, run_id, response_time_ms, error_rate, throughput, p95_latency_ms, availability_percent
FROM metric_snapshots
ORDER BY id;
```

Exit PostgreSQL:

```sql
\q
```

## Example: Add Data And Verify It

Create a service through the backend:

```bash
curl -X POST "http://localhost:8080/api/services?name=test-service&baseUrl=http://localhost:9999&environment=dev&projectId=1"
```

Then check it in PostgreSQL:

```sql
SELECT id, name, base_url, environment
FROM target_services
ORDER BY id;
```

## Common Issues

`Address already in use`

- another process is already using port `8080`
- find it with `ss -ltnp | grep :8080`
- stop it with `kill <PID>` or `kill -9 <PID>`

`No X11 DISPLAY variable was set`

- Swing was started inside WSL without GUI support
- run Swing from Windows PowerShell instead

PowerShell misreads `-Dapi.base=...`

- use:

```powershell
mvn --% clean compile exec:java -Dexec.mainClass=com.microchaos.swing.MicroChaosSwingApp -Dapi.base=http://localhost:8080/api
```

`psql: command not found`

- use Docker instead:

```bash
docker-compose exec postgres psql -U microchaos -d microchaos
```

## More SQL Examples

Additional SQL inspection examples are in [docs/POSTGRESQL_QUERIES.md](/d:/MicroChaos/docs/POSTGRESQL_QUERIES.md).
