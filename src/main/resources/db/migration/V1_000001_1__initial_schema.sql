-- ============================================================
-- KizuLog 初期スキーマ
-- ============================================================

-- ============================================================
-- system_config（システム全体設定）
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
-- system_accounts（システム管理アカウント・master専用）
-- ============================================================
CREATE TABLE system_accounts (
    account_id   TEXT        NOT NULL,
    version      TIMESTAMPTZ NOT NULL,
    iss          TEXT        NOT NULL,
    aud          TEXT        NOT NULL,
    sub          TEXT        NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL,
    created_by   TEXT        NOT NULL,
    CONSTRAINT system_accounts_pk PRIMARY KEY (account_id, version)
);

CREATE INDEX system_accounts_idx_01
    ON system_accounts (iss, aud, sub, version DESC);

-- ============================================================
-- system_account_roles（システム管理ロール）
-- ============================================================
CREATE TABLE system_account_roles (
    account_id   TEXT        NOT NULL,
    role         TEXT        NOT NULL, -- SYSTEM_ADMIN
    version      TIMESTAMPTZ NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL,
    created_by   TEXT        NOT NULL,
    CONSTRAINT system_account_roles_pk PRIMARY KEY (account_id, role, version)
);

CREATE INDEX system_account_roles_idx_01
    ON system_account_roles (account_id, version DESC);

-- ============================================================
-- system_account_status（システム管理アカウントステータス）
-- ============================================================
CREATE TABLE system_account_status (
    account_id   TEXT        NOT NULL,
    version      TIMESTAMPTZ NOT NULL,
    status       TEXT        NOT NULL, -- ACTIVE / INACTIVE / SUSPENDED
    reason       TEXT,
    created_at   TIMESTAMPTZ NOT NULL,
    created_by   TEXT        NOT NULL,
    CONSTRAINT system_account_status_pk PRIMARY KEY (account_id, version)
);

CREATE INDEX system_account_status_idx_01
    ON system_account_status (account_id, version DESC);

CREATE INDEX system_account_status_idx_02
    ON system_account_status (status, version DESC);

-- ============================================================
-- tenants(業務テナント基本情報)
-- ============================================================
CREATE TABLE tenants (
    tenant_id    TEXT        NOT NULL,
    version      TIMESTAMPTZ NOT NULL,
    name         TEXT        NOT NULL,
    host         TEXT        NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL,
    created_by   TEXT        NOT NULL,
    CONSTRAINT tenants_pk PRIMARY KEY (tenant_id, version)
);

CREATE INDEX tenants_idx_01
    ON tenants (host, version DESC);

-- ============================================================
-- tenant_status（業務テナントステータス）
-- ============================================================
CREATE TABLE tenant_status (
    tenant_id    TEXT        NOT NULL,
    version      TIMESTAMPTZ NOT NULL,
    status       TEXT        NOT NULL, -- ACTIVE / INACTIVE / SUSPENDED
    reason       TEXT,
    created_at   TIMESTAMPTZ NOT NULL,
    created_by   TEXT        NOT NULL,
    CONSTRAINT tenant_status_pk PRIMARY KEY (tenant_id, version)
);

CREATE INDEX tenant_status_idx_01
    ON tenant_status (tenant_id, version DESC);

CREATE INDEX tenant_status_idx_02
    ON tenant_status (status, version DESC);

-- ============================================================
-- tenant_oidc_configs（業務テナントOIDC設定）
-- ============================================================
CREATE TABLE tenant_oidc_configs (
    tenant_id    TEXT        NOT NULL,
    iss          TEXT        NOT NULL,
    aud          TEXT        NOT NULL,
    version      TIMESTAMPTZ NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL,
    created_by   TEXT        NOT NULL,
    CONSTRAINT tenant_oidc_configs_pk PRIMARY KEY (tenant_id, iss, aud, version)
);

CREATE INDEX tenant_oidc_configs_idx_01
    ON tenant_oidc_configs (tenant_id, version DESC);

-- ============================================================
-- tenant_oidc_config_status（テナントOIDC設定ステータス）
-- ============================================================
CREATE TABLE tenant_oidc_config_status (
    tenant_id    TEXT        NOT NULL,
    iss          TEXT        NOT NULL,
    aud          TEXT        NOT NULL,
    version      TIMESTAMPTZ NOT NULL,
    status       TEXT        NOT NULL, -- ACTIVE / INACTIVE
    reason       TEXT,
    created_at   TIMESTAMPTZ NOT NULL,
    created_by   TEXT        NOT NULL,
    CONSTRAINT tenant_oidc_config_status_pk PRIMARY KEY (tenant_id, iss, aud, version)
);

CREATE INDEX tenant_oidc_config_status_idx_01
    ON tenant_oidc_config_status (tenant_id, iss, aud, version DESC);

CREATE INDEX tenant_oidc_config_status_idx_02
    ON tenant_oidc_config_status (status, version DESC);

-- ============================================================
-- tenant_settings（業務テナント拡張設定・JSON）
-- ============================================================
CREATE TABLE tenant_settings (
    tenant_id    TEXT        NOT NULL,
    key          TEXT        NOT NULL,
    version      TIMESTAMPTZ NOT NULL,
    value        JSONB       NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL,
    created_by   TEXT        NOT NULL,
    CONSTRAINT tenant_settings_pk PRIMARY KEY (tenant_id, key, version)
);

CREATE INDEX tenant_settings_idx_01
    ON tenant_settings (tenant_id, key, version DESC);

-- ============================================================
-- tenant_accounts（業務テナントアカウント）
-- ============================================================
CREATE TABLE tenant_accounts (
    account_id   TEXT        NOT NULL,
    version      TIMESTAMPTZ NOT NULL,
    tenant_id    TEXT        NOT NULL,
    iss          TEXT        NOT NULL,
    aud          TEXT        NOT NULL,
    sub          TEXT        NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL,
    created_by   TEXT        NOT NULL,
    CONSTRAINT tenant_accounts_pk PRIMARY KEY (account_id, version)
);

CREATE INDEX tenant_accounts_idx_01
    ON tenant_accounts (tenant_id, iss, aud, sub, version DESC);

CREATE INDEX tenant_accounts_idx_02
    ON tenant_accounts (tenant_id, version DESC);

-- ============================================================
-- tenant_account_roles（業務テナントアカウントロール）
-- ============================================================
CREATE TABLE tenant_account_roles (
    account_id   TEXT        NOT NULL,
    role         TEXT        NOT NULL, -- TENANT_ADMIN / EMPLOYEE
    version      TIMESTAMPTZ NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL,
    created_by   TEXT        NOT NULL,
    CONSTRAINT tenant_account_roles_pk PRIMARY KEY (account_id, role, version)
);

CREATE INDEX tenant_account_roles_idx_01
    ON tenant_account_roles (account_id, version DESC);

CREATE INDEX tenant_account_roles_idx_02
    ON tenant_account_roles (role, version DESC);

-- ============================================================
-- tenant_account_status（業務テナントアカウントステータス）
-- ============================================================
CREATE TABLE tenant_account_status (
    account_id   TEXT        NOT NULL,
    version      TIMESTAMPTZ NOT NULL,
    status       TEXT        NOT NULL, -- ACTIVE / INACTIVE / SUSPENDED
    reason       TEXT,
    created_at   TIMESTAMPTZ NOT NULL,
    created_by   TEXT        NOT NULL,
    CONSTRAINT tenant_account_status_pk PRIMARY KEY (account_id, version)
);

CREATE INDEX tenant_account_status_idx_01
    ON tenant_account_status (account_id, version DESC);

CREATE INDEX tenant_account_status_idx_02
    ON tenant_account_status (status, version DESC);
