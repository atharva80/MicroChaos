# PostgreSQL Database Queries for MicroChaos

This file contains useful SQL commands to view and inspect the database.

## Connect to Database

```bash
cd /mnt/d/MicroChaos
docker-compose exec postgres psql -U microchaos -d microchaos
```

Or inside container:
```bash
docker exec -it microchaos-postgres psql -U microchaos -d microchaos
```

---

## View All Tables

```sql
\dt
```

---

## Target Services (Services Panel)

```sql
SELECT id, project_id, name, base_url, environment, health_endpoint, status, timeout_threshold_ms, created_at
FROM target_services
ORDER BY id;
```

---

## Service Dependencies (Topology)

```sql
SELECT id, source_service_id, target_service_id, dependency_type, protocol, communication_mode, criticality, fallback_available
FROM service_dependencies
ORDER BY id;
```

---

## Experiments

```sql
SELECT id, project_id, name, description, target_service_id, fault_type, stress_type, duration_seconds, intensity, remediation_mode, blast_radius_limit, status, created_by, created_at
FROM experiments
ORDER BY id;
```

---

## Experiment Runs

```sql
SELECT id, experiment_id, started_at, ended_at, status, mttr_seconds, resilience_score, summary
FROM experiment_runs
ORDER BY id;
```

---

## Metric Snapshots

```sql
SELECT id, run_id, ts, response_time_ms, error_rate, throughput, p95_latency_ms, availability_percent
FROM metric_snapshots
ORDER BY id DESC
LIMIT 100;
```

---

## Remediation Policies

```sql
SELECT id, name, description, enabled, created_at
FROM remediation_policies
ORDER BY id;
```

---

## Remediation Rules

```sql
SELECT id, policy_id, name, condition_json, action_type, action_config_json, priority, enabled
FROM remediation_rules
ORDER BY id;
```

---

## Remediation Executions

```sql
SELECT id, run_id, rule_id, executed_at, status, result_json
FROM remediation_executions
ORDER BY id DESC
LIMIT 50;
```

---

## Count Records in Each Table

```sql
SELECT 
    'target_services' AS table_name, COUNT(*) AS count FROM target_services
UNION ALL
SELECT 'service_dependencies', COUNT(*) FROM service_dependencies
UNION ALL
SELECT 'experiments', COUNT(*) FROM experiments
UNION ALL
SELECT 'experiment_runs', COUNT(*) FROM experiment_runs
UNION ALL
SELECT 'metric_snapshots', COUNT(*) FROM metric_snapshots
UNION ALL
SELECT 'remediation_policies', COUNT(*) FROM remediation_policies
UNION ALL
SELECT 'remediation_rules', COUNT(*) FROM remediation_rules
UNION ALL
SELECT 'remediation_executions', COUNT(*) FROM remediation_executions;
```

---

## View Latest Experiment Runs with Details

```sql
SELECT 
    er.id AS run_id,
    er.experiment_id,
    e.name AS experiment_name,
    er.status,
    er.resilience_score,
    er.mttr_seconds,
    er.started_at,
    er.ended_at
FROM experiment_runs er
JOIN experiments e ON er.experiment_id = e.id
ORDER BY er.started_at DESC
LIMIT 20;
```

---

## View Services with Their Dependencies

```sql
SELECT 
    ts.name AS service_name,
    ts.environment,
    ts.status,
    COUNT(sd.id) AS dependency_count
FROM target_services ts
LEFT JOIN service_dependencies sd ON ts.id = sd.source_service_id
GROUP BY ts.id, ts.name, ts.environment, ts.status
ORDER BY ts.id;
```

---

## Exit PostgreSQL

```sql
\q
```

---

## Quick Reference

| Table | Purpose | Key Columns |
|-------|---------|-------------|
| `target_services` | Registered services | name, base_url, environment, status |
| `service_dependencies` | Service relationships | source_service_id, target_service_id, dependency_type |
| `experiments` | Chaos experiments | name, fault_type, stress_type, status |
| `experiment_runs` | Experiment executions | experiment_id, status, resilience_score, mttr_seconds |
| `metric_snapshots` | Run metrics | run_id, response_time_ms, error_rate, throughput |

## Metric Snapshots

```sql
SELECT id, run_id, ts, response_time_ms, error_rate, throughput, p95_latency_ms, availability_percent
FROM metric_snapshots
ORDER BY id;
```

## Counts Per Table

```sql
SELECT COUNT(*) AS services_count FROM target_services;
SELECT COUNT(*) AS dependencies_count FROM service_dependencies;
SELECT COUNT(*) AS experiments_count FROM experiments;
SELECT COUNT(*) AS runs_count FROM experiment_runs;
SELECT COUNT(*) AS metrics_count FROM metric_snapshots;
```

## Most Recent Rows

```sql
SELECT * FROM target_services ORDER BY id DESC LIMIT 5;
SELECT * FROM service_dependencies ORDER BY id DESC LIMIT 5;
SELECT * FROM experiments ORDER BY id DESC LIMIT 5;
SELECT * FROM experiment_runs ORDER BY id DESC LIMIT 5;
SELECT * FROM metric_snapshots ORDER BY id DESC LIMIT 10;
```

## Exit

```sql
\q
```
