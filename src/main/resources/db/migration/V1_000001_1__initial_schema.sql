-- ============================================================
-- KizuLog 初期スキーマ
-- バージョン : 1.000001.1
-- 作成日     : 2026-04-24
-- ============================================================

-- ============================================================
-- system_config（システム設定）
-- ============================================================
CREATE TABLE system_config (
    key        TEXT        NOT NULL,
    version    TIMESTAMPTZ NOT NULL,
    value      JSONB       NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    created_by TEXT        NOT NULL,
    CONSTRAINT system_config_pk PRIMARY KEY (key, version)
);

-- ============================================================
-- tenants（テナント）
-- ============================================================
CREATE TABLE tenants (
    tenant_id    TEXT        NOT NULL,
    version      TIMESTAMPTZ NOT NULL,
    tenant_type  TEXT        NOT NULL, -- SYSTEM / BUSINESS
    name         TEXT        NOT NULL,
    host         TEXT        NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL,
    created_by   TEXT        NOT NULL,
    CONSTRAINT tenants_pk PRIMARY KEY (tenant_id, version)
);

CREATE INDEX tenants_idx_01 ON tenants (host, version DESC);
CREATE INDEX tenants_idx_02 ON tenants (tenant_type, version DESC);

-- ============================================================
-- tenant_oidc_configs（テナントOIDC設定）
-- ============================================================
CREATE TABLE tenant_oidc_configs (
    config_id    TEXT        NOT NULL,
    version      TIMESTAMPTZ NOT NULL,
    tenant_id    TEXT        NOT NULL,
    iss          TEXT        NOT NULL,
    aud          TEXT        NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL,
    created_by   TEXT        NOT NULL,
    CONSTRAINT tenant_oidc_configs_pk PRIMARY KEY (config_id, version)
);

CREATE INDEX tenant_oidc_configs_idx_01 ON tenant_oidc_configs (tenant_id, iss, aud, version DESC);
CREATE INDEX tenant_oidc_configs_idx_02 ON tenant_oidc_configs (tenant_id, version DESC);

-- ============================================================
-- accounts（アカウント）
-- ============================================================
CREATE TABLE accounts (
    account_id   TEXT        NOT NULL,
    version      TIMESTAMPTZ NOT NULL,
    tenant_id    TEXT        NOT NULL,
    iss          TEXT        NOT NULL,
    aud          TEXT        NOT NULL,
    sub          TEXT        NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL,
    created_by   TEXT        NOT NULL,
    CONSTRAINT accounts_pk PRIMARY KEY (account_id, version)
);

CREATE INDEX accounts_idx_01 ON accounts (tenant_id, iss, aud, sub, version DESC);
CREATE INDEX accounts_idx_02 ON accounts (tenant_id, version DESC);

-- ============================================================
-- users（ユーザー個人情報・バージョン管理）
-- ============================================================
CREATE TABLE users (
    account_id   TEXT        NOT NULL,
    version      TIMESTAMPTZ NOT NULL,
    name         TEXT        NOT NULL,
    email        TEXT        NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL,
    created_by   TEXT        NOT NULL,
    CONSTRAINT users_pk PRIMARY KEY (account_id, version)
);

CREATE INDEX users_idx_01 ON users (account_id, version DESC);
CREATE INDEX users_idx_02 ON users (email, version DESC);

-- ============================================================
-- account_roles（アカウントロール・バージョン管理）
-- ============================================================
CREATE TABLE account_roles (
    account_id   TEXT        NOT NULL,
    role         TEXT        NOT NULL, -- SYSTEM_ADMIN / TENANT_ADMIN / EMPLOYEE
    version      TIMESTAMPTZ NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL,
    created_by   TEXT        NOT NULL,
    CONSTRAINT account_roles_pk PRIMARY KEY (account_id, role, version)
);

CREATE INDEX account_roles_idx_01 ON account_roles (account_id, version DESC);
CREATE INDEX account_roles_idx_02 ON account_roles (role, version DESC);

-- ============================================================
-- account_status（アカウントステータス・バージョン管理）
-- ============================================================
CREATE TABLE account_status (
    account_id   TEXT        NOT NULL,
    version      TIMESTAMPTZ NOT NULL,
    status       TEXT        NOT NULL, -- ACTIVE / INACTIVE / SUSPENDED
    reason       TEXT,
    created_at   TIMESTAMPTZ NOT NULL,
    created_by   TEXT        NOT NULL,
    CONSTRAINT account_status_pk PRIMARY KEY (account_id, version)
);

CREATE INDEX account_status_idx_01 ON account_status (account_id, version DESC);
CREATE INDEX account_status_idx_02 ON account_status (status, version DESC);

-- ============================================================
