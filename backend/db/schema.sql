CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(180) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(40) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS projects (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    description TEXT,
    owner_id BIGINT REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS target_services (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT REFERENCES projects(id),
    name VARCHAR(120) NOT NULL,
    base_url VARCHAR(255) NOT NULL,
    health_endpoint VARCHAR(120) NOT NULL DEFAULT '/health',
    environment VARCHAR(40) NOT NULL DEFAULT 'dev',
    status VARCHAR(30) NOT NULL DEFAULT 'HEALTHY',
    timeout_threshold_ms INT NOT NULL DEFAULT 2000
);

CREATE TABLE IF NOT EXISTS service_dependencies (
    id BIGSERIAL PRIMARY KEY,
    source_service_id BIGINT REFERENCES target_services(id),
    target_service_id BIGINT REFERENCES target_services(id),
    dependency_type VARCHAR(50) NOT NULL,
    protocol VARCHAR(50) NOT NULL,
    communication_mode VARCHAR(30) NOT NULL,
    criticality VARCHAR(30) NOT NULL,
    fallback_available BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS experiments (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT REFERENCES projects(id),
    name VARCHAR(180) NOT NULL,
    description TEXT,
    target_service_id BIGINT REFERENCES target_services(id),
    fault_type VARCHAR(60) NOT NULL,
    stress_type VARCHAR(60) NOT NULL,
    duration_seconds INT NOT NULL,
    intensity INT NOT NULL,
    remediation_mode VARCHAR(40) NOT NULL,
    blast_radius_limit INT NOT NULL,
    status VARCHAR(40) NOT NULL,
    created_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS experiment_runs (
    id BIGSERIAL PRIMARY KEY,
    experiment_id BIGINT REFERENCES experiments(id),
    started_at TIMESTAMP NOT NULL DEFAULT NOW(),
    ended_at TIMESTAMP,
    status VARCHAR(40) NOT NULL,
    mttr_seconds BIGINT NOT NULL DEFAULT 0,
    resilience_score DECIMAL(5,2) NOT NULL DEFAULT 0,
    summary TEXT
);

CREATE TABLE IF NOT EXISTS fault_configs (
    id BIGSERIAL PRIMARY KEY,
    experiment_id BIGINT REFERENCES experiments(id),
    config_json TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS metric_snapshots (
    id BIGSERIAL PRIMARY KEY,
    run_id BIGINT REFERENCES experiment_runs(id),
    ts TIMESTAMP NOT NULL DEFAULT NOW(),
    response_time_ms DECIMAL(10,2) NOT NULL,
    error_rate DECIMAL(8,2) NOT NULL,
    throughput DECIMAL(10,2) NOT NULL,
    p95_latency_ms DECIMAL(10,2) NOT NULL,
    availability_percent DECIMAL(8,2) NOT NULL
);

CREATE TABLE IF NOT EXISTS alerts (
    id BIGSERIAL PRIMARY KEY,
    run_id BIGINT REFERENCES experiment_runs(id),
    severity VARCHAR(20) NOT NULL,
    message TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS remediation_policies (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT REFERENCES projects(id),
    name VARCHAR(120) NOT NULL,
    mode VARCHAR(40) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS remediation_rules (
    id BIGSERIAL PRIMARY KEY,
    policy_id BIGINT REFERENCES remediation_policies(id),
    metric_name VARCHAR(80) NOT NULL,
    operator VARCHAR(8) NOT NULL,
    threshold_value DECIMAL(10,2) NOT NULL,
    action_type VARCHAR(80) NOT NULL,
    priority INT NOT NULL DEFAULT 10
);

CREATE TABLE IF NOT EXISTS remediation_executions (
    id BIGSERIAL PRIMARY KEY,
    run_id BIGINT REFERENCES experiment_runs(id),
    rule_id BIGINT REFERENCES remediation_rules(id),
    action_type VARCHAR(80) NOT NULL,
    status VARCHAR(40) NOT NULL,
    started_at TIMESTAMP NOT NULL DEFAULT NOW(),
    ended_at TIMESTAMP,
    result_summary TEXT
);

CREATE TABLE IF NOT EXISTS audit_events (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT REFERENCES projects(id),
    run_id BIGINT REFERENCES experiment_runs(id),
    event_type VARCHAR(80) NOT NULL,
    actor VARCHAR(120),
    details TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_target_services_project ON target_services(project_id);
CREATE INDEX IF NOT EXISTS idx_dependencies_source ON service_dependencies(source_service_id);
CREATE INDEX IF NOT EXISTS idx_dependencies_target ON service_dependencies(target_service_id);
CREATE INDEX IF NOT EXISTS idx_experiments_target ON experiments(target_service_id);
CREATE INDEX IF NOT EXISTS idx_runs_experiment ON experiment_runs(experiment_id);
CREATE INDEX IF NOT EXISTS idx_metrics_run ON metric_snapshots(run_id);
CREATE INDEX IF NOT EXISTS idx_remediation_exec_run ON remediation_executions(run_id);
