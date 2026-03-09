CREATE TABLE departments (
                             id          UUID            NOT NULL DEFAULT gen_random_uuid(),
                             name        VARCHAR(100)    NOT NULL,
                             description TEXT,
                             created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

                             CONSTRAINT pk_departments PRIMARY KEY (id)
);

CREATE TABLE users (
                       id            UUID            NOT NULL DEFAULT gen_random_uuid(),
                       email         VARCHAR(255)    NOT NULL,
                       name          VARCHAR(255)    NOT NULL,
                       role          VARCHAR(20)     NOT NULL,
                       department_id UUID,
                       is_active     BOOLEAN         NOT NULL DEFAULT TRUE,
                       created_at    TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

                       CONSTRAINT pk_users             PRIMARY KEY (id),
                       CONSTRAINT uq_users_email       UNIQUE (email),
                       CONSTRAINT fk_users_department  FOREIGN KEY (department_id)
                           REFERENCES departments (id)
                           ON DELETE SET NULL
);

CREATE TABLE service_requests (
                                  id                UUID            NOT NULL DEFAULT gen_random_uuid(),
                                  title             VARCHAR(255)    NOT NULL,
                                  description       TEXT,
                                  category          VARCHAR(20)     NOT NULL,
                                  priority          VARCHAR(10)     NOT NULL,
                                  status            VARCHAR(15)     NOT NULL DEFAULT 'OPEN',
                                  requester_id      UUID            NOT NULL,
                                  assigned_to_id    UUID,
                                  department_id     UUID,
                                  created_at        TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
                                  updated_at        TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
                                  resolved_at       TIMESTAMPTZ,
                                  closed_at         TIMESTAMPTZ,
                                  first_response_at TIMESTAMPTZ,
                                  sla_deadline      TIMESTAMPTZ,
                                  is_sla_breached   BOOLEAN         NOT NULL DEFAULT FALSE,

                                  CONSTRAINT pk_service_requests          PRIMARY KEY (id),
                                  CONSTRAINT fk_sr_requester              FOREIGN KEY (requester_id)
                                      REFERENCES users (id)
                                      ON DELETE RESTRICT,
                                  CONSTRAINT fk_sr_assigned_to            FOREIGN KEY (assigned_to_id)
                                      REFERENCES users (id)
                                      ON DELETE SET NULL,
                                  CONSTRAINT fk_sr_department             FOREIGN KEY (department_id)
                                      REFERENCES departments (id)
                                      ON DELETE SET NULL
);

CREATE TABLE sla_policies (
                              id                      UUID        NOT NULL DEFAULT gen_random_uuid(),
                              category                VARCHAR(20) NOT NULL,
                              priority                VARCHAR(10) NOT NULL,
                              response_time_hours     INTEGER     NOT NULL,
                              resolution_time_hours   INTEGER     NOT NULL,
                              created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),

                              CONSTRAINT pk_sla_policies              PRIMARY KEY (id),
                              CONSTRAINT uq_sla_policies_cat_pri      UNIQUE (category, priority)
);

CREATE INDEX idx_sr_status          ON service_requests (status);
CREATE INDEX idx_sr_category        ON service_requests (category);
CREATE INDEX idx_sr_priority        ON service_requests (priority);
CREATE INDEX idx_sr_requester_id    ON service_requests (requester_id);
CREATE INDEX idx_sr_assigned_to_id  ON service_requests (assigned_to_id);
CREATE INDEX idx_sr_department_id   ON service_requests (department_id);
CREATE INDEX idx_sr_created_at      ON service_requests (created_at DESC);
CREATE INDEX idx_sr_sla_deadline    ON service_requests (sla_deadline)
    WHERE sla_deadline IS NOT NULL;

CREATE INDEX idx_users_email        ON users (email);
CREATE INDEX idx_users_role         ON users (role);
CREATE INDEX idx_users_department   ON users (department_id);

INSERT INTO sla_policies (category, priority, response_time_hours, resolution_time_hours) VALUES

    ('IT_SUPPORT',  'CRITICAL',  1,   4),
    ('IT_SUPPORT',  'HIGH',      2,   8),
    ('IT_SUPPORT',  'MEDIUM',    4,   24),
    ('IT_SUPPORT',  'LOW',       8,   48),

    ('FACILITIES',  'CRITICAL',  1,   8),
    ('FACILITIES',  'HIGH',      2,   16),
    ('FACILITIES',  'MEDIUM',    4,   48),
    ('FACILITIES',  'LOW',       8,   96),

    ('HR_REQUEST',  'CRITICAL',  2,   8),
    ('HR_REQUEST',  'HIGH',      4,   24),
    ('HR_REQUEST',  'MEDIUM',    8,   72),
    ('HR_REQUEST',  'LOW',       24,  168);