INSERT INTO users (id, name, email, password_hash, role)
VALUES (1, 'Demo Admin', 'admin@microchaos.local', 'demo_hash', 'ADMIN')
ON CONFLICT (id) DO NOTHING;

INSERT INTO projects (id, name, description, owner_id)
VALUES (1, 'MicroChaos Demo Project', 'E-commerce resilience demo', 1)
ON CONFLICT (id) DO NOTHING;
