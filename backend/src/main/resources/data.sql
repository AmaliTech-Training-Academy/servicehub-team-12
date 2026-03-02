-- Seed departments
INSERT INTO departments (id, name, description) VALUES
  (1, 'IT Support', 'Technical support and infrastructure'),
  (2, 'HR', 'Human resources and people operations'),
  (3, 'Facilities', 'Office facilities and maintenance'),
  (4, 'Finance', 'Financial services and reimbursements')
ON CONFLICT (id) DO NOTHING;

-- Seed SLA policies
INSERT INTO sla_policies (id, category, priority, response_hours, resolution_hours) VALUES
  (1, 'IT_SUPPORT', 'HIGH', 1, 4),
  (2, 'IT_SUPPORT', 'MEDIUM', 4, 24),
  (3, 'IT_SUPPORT', 'LOW', 8, 48),
  (4, 'HR_REQUEST', 'HIGH', 2, 8),
  (5, 'HR_REQUEST', 'MEDIUM', 8, 48),
  (6, 'FACILITIES', 'HIGH', 1, 8),
  (7, 'FACILITIES', 'MEDIUM', 4, 24)
ON CONFLICT (id) DO NOTHING;

-- Seed admin user (password: password123)
INSERT INTO users (id, email, name, password, role, created_at) VALUES
  (1, 'admin@amalitech.com', 'Admin User', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ADMIN', NOW()),
  (2, 'agent@amalitech.com', 'Support Agent', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'AGENT', NOW()),
  (3, 'user@amalitech.com', 'Test User', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'USER', NOW())
ON CONFLICT (id) DO NOTHING;
