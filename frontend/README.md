# MicroChaos Frontend

No-build frontend dashboard (HTML/CSS/JS) for:

- service registration
- dependency mapping
- topology visualization (with edge type and sync/async mode)
- experiment execution
- run metrics and scorecard lookup
- live monitoring table with service health state and fault telemetry
- one-click `Down`, `Latency`, `Recover` service controls
- metrics-first view (summary cards + snapshot table) with optional raw payload panel

## Run

```bash
cd frontend
./scripts/run-frontend.sh
```

Open `http://localhost:5173`.

Ensure backend is running on `http://localhost:8080`.

If backend runs on a custom port:

- open `http://localhost:5173/?api=http://localhost:18080/api`
