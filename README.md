# MicroChaos

Production-inspired chaos engineering platform for a Java demo microservice stack.

## What Is Implemented

- Java backend control plane (`backend/`)
- PostgreSQL schema + docker compose (`backend/db/`, `docker-compose.yml`)
- frontend dashboard with topology graph (`frontend/`)
- 6 demo microservices with fault toggles (`demo-services/`)
- live monitoring layer with per-service health history and fault visibility
- direct service controls for forcing `DOWN` and recovering services

## Start Order

1. From the project root, start demo services:

```bash
cd demo-services
./scripts/run-demo-stack.sh
```

If `9000-9005` are occupied:

```bash
cd demo-services
DEMO_BASE_PORT=9100 ./scripts/run-demo-stack.sh
```

2. Start backend:

```bash
cd backend
./scripts/run-backend.sh
```

Use the same base port when customized:

```bash
cd backend
DEMO_BASE_PORT=9100 ./scripts/run-backend.sh
```

3. Start frontend:

```bash
cd frontend
./scripts/run-frontend.sh
```

4. Open:

- `http://localhost:5173`
- if backend runs on a custom port, pass API base as query param
- example: `http://localhost:5173/?api=http://localhost:18080/api`

## Backend API Highlights

- Service registry and topology graph
- Experiment create/run/stop
- Fault injection orchestration against demo services
- Direct service down/recover control endpoints
- Run metrics and resilience scorecard
- Monitoring APIs for live health, fault state, and history

## DB Setup

Run PostgreSQL with schema + seed:

```bash
docker compose up -d postgres
```

Current backend persistence is in-memory for zero-dependency local runs, but SQL schema is complete and ready for JDBC migration.

## Docker Status Note

Only PostgreSQL is defined in compose. Backend, frontend, and demo services currently run via local scripts (not containers).  
If `docker ps` is empty, this is expected until you start compose manually.
