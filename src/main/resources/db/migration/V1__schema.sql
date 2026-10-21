-- Field Dispatch Operations schema

CREATE TABLE regions (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    code VARCHAR(20) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    role VARCHAR(30) NOT NULL CHECK (role IN ('OPS_ADMIN','DISPATCHER','FIELD_WORKER','CUSTOMER')),
    phone VARCHAR(30),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    region_id BIGINT REFERENCES regions(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE dispatcher_regions (
    dispatcher_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    region_id BIGINT NOT NULL REFERENCES regions(id) ON DELETE CASCADE,
    PRIMARY KEY (dispatcher_id, region_id)
);

CREATE TABLE customers (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id),
    name VARCHAR(150) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(30),
    region_id BIGINT NOT NULL REFERENCES regions(id),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE service_locations (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    label VARCHAR(100) NOT NULL,
    address_line VARCHAR(255) NOT NULL,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(50) NOT NULL,
    postal_code VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE service_types (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255),
    requires_proof BOOLEAN NOT NULL DEFAULT TRUE,
    base_rate NUMERIC(10,2) NOT NULL DEFAULT 0.00
);

CREATE TABLE workers (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    region_id BIGINT NOT NULL REFERENCES regions(id),
    availability VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE' CHECK (availability IN ('AVAILABLE','ON_JOB','OFF_DUTY','UNAVAILABLE')),
    certifications VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE jobs (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES customers(id),
    location_id BIGINT NOT NULL REFERENCES service_locations(id),
    service_type_id BIGINT NOT NULL REFERENCES service_types(id),
    region_id BIGINT NOT NULL REFERENCES regions(id),
    assigned_worker_id BIGINT REFERENCES workers(id),
    status VARCHAR(20) NOT NULL DEFAULT 'unassigned'
        CHECK (status IN ('unassigned','assigned','en_route','arrived','in_progress','completed','cancelled','failed')),
    priority VARCHAR(10) NOT NULL DEFAULT 'normal' CHECK (priority IN ('low','normal','high','urgent')),
    scheduled_start TIMESTAMPTZ NOT NULL,
    scheduled_end TIMESTAMPTZ NOT NULL,
    description VARCHAR(500),
    reschedule_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_jobs_region ON jobs(region_id);
CREATE INDEX idx_jobs_status ON jobs(status);
CREATE INDEX idx_jobs_worker ON jobs(assigned_worker_id);
CREATE INDEX idx_jobs_customer ON jobs(customer_id);
CREATE INDEX idx_jobs_scheduled_start ON jobs(scheduled_start);

CREATE TABLE routes (
    id BIGSERIAL PRIMARY KEY,
    worker_id BIGINT NOT NULL REFERENCES workers(id),
    region_id BIGINT NOT NULL REFERENCES regions(id),
    route_date DATE NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (worker_id, route_date)
);

CREATE TABLE route_stops (
    id BIGSERIAL PRIMARY KEY,
    route_id BIGINT NOT NULL REFERENCES routes(id) ON DELETE CASCADE,
    job_id BIGINT NOT NULL REFERENCES jobs(id),
    sequence INT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (route_id, sequence)
);

CREATE TABLE proofs (
    id BIGSERIAL PRIMARY KEY,
    job_id BIGINT NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    uploaded_by BIGINT NOT NULL REFERENCES users(id),
    category VARCHAR(20) NOT NULL CHECK (category IN ('SIGNATURE','PHOTO','FORM','COMPLETION_DOC')),
    visibility VARCHAR(20) NOT NULL DEFAULT 'INTERNAL' CHECK (visibility IN ('CUSTOMER_VISIBLE','INTERNAL')),
    stored_filename VARCHAR(255) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE incidents (
    id BIGSERIAL PRIMARY KEY,
    job_id BIGINT NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    reported_by BIGINT NOT NULL REFERENCES users(id),
    assigned_to BIGINT REFERENCES users(id),
    severity VARCHAR(10) NOT NULL CHECK (severity IN ('low','medium','high','critical')),
    status VARCHAR(15) NOT NULL DEFAULT 'open' CHECK (status IN ('open','triaged','assigned','resolved','closed')),
    internal_notes TEXT,
    customer_summary VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    resolved_at TIMESTAMPTZ
);

CREATE TABLE job_messages (
    id BIGSERIAL PRIMARY KEY,
    job_id BIGINT NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    sender_id BIGINT NOT NULL REFERENCES users(id),
    sender_role VARCHAR(30) NOT NULL,
    body VARCHAR(2000) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE audit_log (
    id BIGSERIAL PRIMARY KEY,
    actor_id BIGINT REFERENCES users(id),
    actor_email VARCHAR(255) NOT NULL,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id VARCHAR(50),
    before_value TEXT,
    after_value TEXT,
    authorization_basis VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
-- Append-only: no application code path ever issues UPDATE/DELETE against audit_log.

CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(150) NOT NULL,
    body VARCHAR(500) NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE webhooks (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    target_url VARCHAR(500) NOT NULL,
    secret VARCHAR(100) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by BIGINT REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE webhook_deliveries (
    id BIGSERIAL PRIMARY KEY,
    webhook_id BIGINT NOT NULL REFERENCES webhooks(id) ON DELETE CASCADE,
    event_type VARCHAR(50) NOT NULL,
    payload TEXT NOT NULL,
    response_status INT,
    success BOOLEAN NOT NULL DEFAULT FALSE,
    attempted_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE outbox_emails (
    id BIGSERIAL PRIMARY KEY,
    to_email VARCHAR(255) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    sent_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE password_reset_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(100) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE import_batches (
    id BIGSERIAL PRIMARY KEY,
    imported_by BIGINT NOT NULL REFERENCES users(id),
    entity_type VARCHAR(30) NOT NULL,
    row_count INT NOT NULL,
    success_count INT NOT NULL,
    error_count INT NOT NULL,
    error_detail TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
