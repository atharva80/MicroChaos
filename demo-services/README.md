# Demo Microservice Stack

This launcher starts 6 demo services on separate ports:

- `api-gateway` (`9000`)
- `order-service` (`9001`)
- `payment-service` (`9002`)
- `inventory-service` (`9003`)
- `notification-service` (`9004`)
- `auth-service` (`9005`)

## Run

```bash
cd demo-services
./scripts/run-demo-stack.sh
```

If default ports are busy, set a custom base:

```bash
DEMO_BASE_PORT=9100 ./scripts/run-demo-stack.sh
```

That maps services to `9100` through `9105`.

## Common Endpoints Per Service

- `GET /health`
- `POST /faults/configure?type=LATENCY|ERROR_RESPONSE|TIMEOUT|DATABASE_CONNECTION|DEPENDENCY_UNAVAILABLE|CPU_SPIKE|THREAD_POOL_EXHAUSTION&intensity=40`
- `POST /faults/reset`
- `POST /admin/restart`
- `POST /admin/fallback/enable`
- `POST /admin/traffic/reduce`

## Demo Flow Endpoint

`api-gateway` exposes:

- `GET /api/checkout?userId=1&itemId=SKU-1`

This triggers auth validation plus order flow through payment and inventory, then notification.
