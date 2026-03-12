# MicroChaos UI/Monitoring Refactor TODO

## Scope

Refactor current app for a cleaner dark UI, stronger service down simulation, and metrics-first output.  
Temporarily remove remediation workflow from the product surface.

## Tasks

- [x] Verify Docker runtime status for existing MicroChaos containers.
- [x] Remove remediation APIs from active backend router flow.
- [x] Remove remediation UI panels and actions from frontend.
- [x] Add service control APIs for direct fault injection/reset per service.
- [x] Add one-click UI actions to force service down and recover.
- [x] Revamp frontend to a dark, polished dashboard style.
- [x] Replace raw metrics-first output with structured metrics tables/cards.
- [x] Keep optional raw payload viewing for debugging.
- [x] Update docs to match no-remediation temporary scope.
- [ ] Optional next: dockerize backend/demo services (currently only Postgres compose exists).
