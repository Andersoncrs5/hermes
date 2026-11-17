-- =========================================================
-- HERMES PAYMENT - IAM (IDENTITY AND ACCESS MANAGEMENT)
-- V1.0.0 — SCHEMA MIGRATIONS (POSTGRESQL)
-- =========================================================

-- =========================================================
-- 1. USERS
-- =========================================================
CREATE TABLE users (
    id BIGINT PRIMARY KEY,
    email VARCHAR(320) NOT NULL,
    username VARCHAR(100) NOT NULL,
    full_name VARCHAR(150),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    blocked_at TIMESTAMPTZ,
    failed_login_attempts INTEGER NOT NULL DEFAULT 0,
    last_login_at TIMESTAMPTZ,

    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,

    CONSTRAINT ck_users_email_not_empty CHECK (TRIM(email) <> ''),
    CONSTRAINT ck_users_email_lowercase CHECK (email = LOWER(email)),
    CONSTRAINT ck_users_username_not_empty CHECK (TRIM(username) <> ''),
    CONSTRAINT ck_users_username_no_spaces CHECK (username !~ '\s'),
    CONSTRAINT ck_users_failed_login_attempts CHECK (failed_login_attempts >= 0),
    CONSTRAINT ck_users_version CHECK (version >= 0)
);

CREATE UNIQUE INDEX uk_users_active_email
    ON users (email)
    WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX uk_users_active_username
    ON users (username)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_users_created_at
    ON users (created_at DESC);

-- =========================================================
-- 2. USER_PASSWORDS
-- =========================================================
CREATE TABLE user_passwords (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    type_hash VARCHAR(50) NOT NULL DEFAULT 'argon2id',
    should_update BOOLEAN NOT NULL DEFAULT FALSE,

    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,

    CONSTRAINT fk_user_passwords_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,
    CONSTRAINT uk_user_passwords_user_id
        UNIQUE (user_id),
    CONSTRAINT ck_user_passwords_hash_not_empty
        CHECK (TRIM(password_hash) <> ''),
    CONSTRAINT ck_user_passwords_version
        CHECK (version >= 0)
);

CREATE INDEX idx_user_passwords_user_id
    ON user_passwords (user_id);

-- =========================================================
-- 3. TENANTS (ORGANIZAÇÕES CONTRATANTES)
-- =========================================================
CREATE TABLE tenants (
    id BIGINT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    slug VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    country_code VARCHAR(2) NOT NULL DEFAULT 'BR',

    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,

    CONSTRAINT ck_tenants_name_not_empty CHECK (TRIM(name) <> ''),
    CONSTRAINT ck_tenants_slug_not_empty CHECK (TRIM(slug) <> ''),
    CONSTRAINT ck_tenants_slug_format CHECK (slug ~ '^[a-z0-9-]+$'),
    CONSTRAINT ck_tenants_version CHECK (version >= 0)
);

CREATE UNIQUE INDEX uk_tenants_active_slug
    ON tenants (slug)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_tenants_status
    ON tenants (status)
    WHERE deleted_at IS NULL;

-- =========================================================
-- 4. USER_TENANTS (VÍNCULO USUÁRIO x TENANT)
-- =========================================================
CREATE TABLE user_tenants (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    joined_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    invited_by BIGINT,

    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,

    CONSTRAINT fk_user_tenants_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_user_tenants_tenant
        FOREIGN KEY (tenant_id)
        REFERENCES tenants(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_user_tenants_invited_by
        FOREIGN KEY (invited_by)
        REFERENCES users(id)
        ON DELETE SET NULL,
    CONSTRAINT ck_user_tenants_version
        CHECK (version >= 0)
);

CREATE UNIQUE INDEX uk_user_tenants_active_link
    ON user_tenants (user_id, tenant_id)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_user_tenants_tenant_id
    ON user_tenants (tenant_id);

CREATE INDEX idx_user_tenants_user_id
    ON user_tenants (user_id);

-- =========================================================
-- 5. ROLES
-- =========================================================
CREATE TABLE roles (
    id BIGINT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(100) NOT NULL,
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_system BOOLEAN NOT NULL DEFAULT FALSE,

    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,

    CONSTRAINT ck_roles_name_not_empty CHECK (TRIM(name) <> ''),
    CONSTRAINT ck_roles_slug_not_empty CHECK (TRIM(slug) <> ''),
    CONSTRAINT ck_roles_slug_format CHECK (slug ~ '^[a-z0-9_-]+$'),
    CONSTRAINT ck_roles_version CHECK (version >= 0)
);

CREATE UNIQUE INDEX uk_roles_active_slug
    ON roles (slug)
    WHERE deleted_at IS NULL;

-- =========================================================
-- 6. PERMISSIONS
-- =========================================================
CREATE TABLE permissions (
    id BIGINT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(150) NOT NULL,
    module VARCHAR(50) NOT NULL,
    action VARCHAR(50) NOT NULL,
    description TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    is_system BOOLEAN NOT NULL DEFAULT FALSE,

    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,

    CONSTRAINT ck_permissions_slug_not_empty CHECK (TRIM(slug) <> ''),
    CONSTRAINT ck_permissions_name_not_empty CHECK (TRIM(name) <> ''),
    CONSTRAINT ck_permissions_version CHECK (version >= 0)
);

CREATE UNIQUE INDEX uk_permissions_active_slug
    ON permissions (slug)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_permissions_module
    ON permissions (module);

-- =========================================================
-- 7. USER_TENANT_ROLES
-- =========================================================
CREATE TABLE user_tenant_roles (
    id BIGINT PRIMARY KEY,
    user_tenant_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,

    CONSTRAINT fk_utr_user_tenant
        FOREIGN KEY (user_tenant_id)
        REFERENCES user_tenants(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_utr_role
        FOREIGN KEY (role_id)
        REFERENCES roles(id)
        ON DELETE CASCADE,
    CONSTRAINT ck_utr_version
        CHECK (version >= 0)
);

CREATE UNIQUE INDEX uk_utr_active_link
    ON user_tenant_roles (user_tenant_id, role_id)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_utr_role_id
    ON user_tenant_roles (role_id);

-- =========================================================
-- 8. ROLE_PERMISSIONS
-- =========================================================
CREATE TABLE role_permissions (
    id BIGINT PRIMARY KEY,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,

    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,

    CONSTRAINT fk_rp_role
        FOREIGN KEY (role_id)
        REFERENCES roles(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_rp_permission
        FOREIGN KEY (permission_id)
        REFERENCES permissions(id)
        ON DELETE CASCADE,
    CONSTRAINT ck_rp_version
        CHECK (version >= 0)
);

CREATE UNIQUE INDEX uk_rp_active_link
    ON role_permissions (role_id, permission_id)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_rp_permission_id
    ON role_permissions (permission_id);

-- =========================================================
-- 9. USER_PERMISSIONS (CONCESSÃO DIRETA OPCIONAL)
-- =========================================================
CREATE TABLE user_permissions (
    id BIGINT PRIMARY KEY,
    user_tenant_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,

    CONSTRAINT fk_up_user_tenant
        FOREIGN KEY (user_tenant_id)
        REFERENCES user_tenants(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_up_permission
        FOREIGN KEY (permission_id)
        REFERENCES permissions(id)
        ON DELETE CASCADE,
    CONSTRAINT ck_up_version
        CHECK (version >= 0)
);

CREATE UNIQUE INDEX uk_up_active_link
    ON user_permissions (user_tenant_id, permission_id)
    WHERE deleted_at IS NULL;

-- =========================================================
-- 10. SESSIONS
-- =========================================================
CREATE TABLE sessions (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    device_id VARCHAR(100),
    ip_address VARCHAR(45),
    user_agent TEXT,
    last_activity_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    revocation_reason VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,

    CONSTRAINT fk_sessions_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,
    CONSTRAINT ck_sessions_expires_after_created
        CHECK (expires_at > created_at),
    CONSTRAINT ck_sessions_version
        CHECK (version >= 0)
);

CREATE INDEX idx_sessions_user_id
    ON sessions (user_id);

CREATE INDEX idx_sessions_active
    ON sessions (user_id)
    WHERE is_active = TRUE AND deleted_at IS NULL;

-- =========================================================
-- 11. REFRESH_TOKENS
-- =========================================================
CREATE TABLE refresh_tokens (
    id BIGINT PRIMARY KEY,
    session_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    is_used BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    replaced_by_token_id BIGINT,
    expired_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,

    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,

    CONSTRAINT uk_refresh_tokens_hash
        UNIQUE (token_hash),
    CONSTRAINT fk_refresh_tokens_session
        FOREIGN KEY (session_id)
        REFERENCES sessions(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_refresh_tokens_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,
    CONSTRAINT ck_refresh_tokens_version
        CHECK (version >= 0)
);

CREATE INDEX idx_refresh_tokens_session_id
    ON refresh_tokens (session_id);

CREATE INDEX idx_refresh_tokens_user_id
    ON refresh_tokens (user_id);

CREATE INDEX idx_refresh_tokens_active
    ON refresh_tokens (user_id)
    WHERE is_active = TRUE AND is_used = FALSE AND revoked_at IS NULL;

-- =========================================================
-- 12. LOGIN_ATTEMPTS
-- =========================================================
CREATE TABLE login_attempts (
    id BIGINT PRIMARY KEY,
    user_id BIGINT,
    username VARCHAR(100) NOT NULL,
    ip_address VARCHAR(45) NOT NULL,
    user_agent TEXT,
    device_id VARCHAR(100),
    attempt_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    success BOOLEAN NOT NULL DEFAULT FALSE,
    failure_reason VARCHAR(255),
    trace_id VARCHAR(100),

    CONSTRAINT fk_login_attempts_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE SET NULL
);

CREATE INDEX idx_login_attempts_ip
    ON login_attempts (ip_address, attempt_at DESC);

CREATE INDEX idx_login_attempts_user
    ON login_attempts (username, attempt_at DESC);

-- =========================================================
-- 13. API_KEYS
-- =========================================================
CREATE TABLE api_keys (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    key_prefix VARCHAR(16) NOT NULL,
    secret_hash VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    environment VARCHAR(20) NOT NULL DEFAULT 'TEST',
    expired_at TIMESTAMPTZ,
    created_by BIGINT,
    last_used_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ,

    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,

    CONSTRAINT fk_api_keys_tenant
        FOREIGN KEY (tenant_id)
        REFERENCES tenants(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_api_keys_creator
        FOREIGN KEY (created_by)
        REFERENCES users(id)
        ON DELETE SET NULL,
    CONSTRAINT uk_api_keys_prefix
        UNIQUE (key_prefix),
    CONSTRAINT ck_api_keys_environment
        CHECK (environment IN ('TEST', 'LIVE')),
    CONSTRAINT ck_api_keys_version
        CHECK (version >= 0)
);

CREATE INDEX idx_api_keys_tenant_id
    ON api_keys (tenant_id);

CREATE INDEX idx_api_keys_lookup
    ON api_keys (key_prefix)
    WHERE deleted_at IS NULL AND revoked_at IS NULL;

-- =========================================================
-- 14. API_KEY_PERMISSIONS
-- =========================================================
CREATE TABLE api_key_permissions (
    id BIGINT PRIMARY KEY,
    api_key_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,

    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,

    CONSTRAINT fk_akp_api_key
        FOREIGN KEY (api_key_id)
        REFERENCES api_keys(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_akp_permission
        FOREIGN KEY (permission_id)
        REFERENCES permissions(id)
        ON DELETE CASCADE,
    CONSTRAINT ck_akp_version
        CHECK (version >= 0)
);

CREATE UNIQUE INDEX uk_akp_active_link
    ON api_key_permissions (api_key_id, permission_id)
    WHERE deleted_at IS NULL;

-- =========================================================
-- 15. AUDIT_LOGS (APPEND-ONLY)
-- =========================================================
CREATE TABLE audit_logs (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT,
    actor_type VARCHAR(50) NOT NULL,
    actor_id BIGINT NOT NULL,
    action VARCHAR(100) NOT NULL,
    resource_type VARCHAR(100) NOT NULL,
    resource_id VARCHAR(100) NOT NULL,
    before JSONB,
    after JSONB,
    ip_address VARCHAR(45),
    user_agent TEXT,
    trace_id VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_logs_tenant_id
    ON audit_logs (tenant_id, created_at DESC);

CREATE INDEX idx_audit_logs_actor
    ON audit_logs (actor_type, actor_id, created_at DESC);

CREATE INDEX idx_audit_logs_resource
    ON audit_logs (resource_type, resource_id);


