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
-- system_accounts（システム管理アカウント）
-- ============================================================
CREATE TABLE system_accounts (
    account_id   TEXT        NOT NULL,
    version      TIMESTAMPTZ NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL,
    created_by   TEXT        NOT NULL,
    CONSTRAINT system_accounts_pk PRIMARY KEY (account_id, version)
);

-- ============================================================
-- system_account_status（システム管理アカウントステータス）
-- ============================================================
CREATE TABLE system_account_status (
    account_id   TEXT        NOT NULL,
    version      TIMESTAMPTZ NOT NULL,
    status       TEXT        NOT NULL,
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
-- system_account_identities（システム管理アカウント認証方法）
-- ============================================================
CREATE TABLE system_account_identities (
    identity_id  TEXT        NOT NULL,
    version      TIMESTAMPTZ NOT NULL,
    account_id   TEXT        NOT NULL,
    iss          TEXT        NOT NULL,
    aud          TEXT        NOT NULL,
    sub          TEXT        NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL,
    created_by   TEXT        NOT NULL,
    CONSTRAINT system_account_identities_pk PRIMARY KEY (identity_id, version)
);

CREATE INDEX system_account_identities_idx_01
    ON system_account_identities (iss, aud, sub, version DESC);

CREATE INDEX system_account_identities_idx_02
    ON system_account_identities (account_id, version DESC);

-- ============================================================
-- system_account_identity_status（システム管理アカウント認証方法ステータス）
-- ============================================================
CREATE TABLE system_account_identity_status (
    identity_id  TEXT        NOT NULL,
    version      TIMESTAMPTZ NOT NULL,
    status       TEXT        NOT NULL, -- ACTIVE / INACTIVE
    reason       TEXT,
    created_at   TIMESTAMPTZ NOT NULL,
    created_by   TEXT        NOT NULL,
    CONSTRAINT system_account_identity_status_pk PRIMARY KEY (identity_id, version)
);

CREATE INDEX system_account_identity_status_idx_01
    ON system_account_identity_status (identity_id, version DESC);

CREATE INDEX system_account_identity_status_idx_02
    ON system_account_identity_status (status, version DESC);

-- ============================================================
-- system_account_roles（システム管理ロール）
-- ============================================================
CREATE TABLE system_account_roles (
    role_id      TEXT        NOT NULL,
    version      TIMESTAMPTZ NOT NULL,
    account_id   TEXT        NOT NULL,
    role         TEXT        NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL,
    created_by   TEXT        NOT NULL,
    CONSTRAINT system_account_roles_pk PRIMARY KEY (role_id, version)
);

CREATE INDEX system_account_roles_idx_01
    ON system_account_roles (account_id, version DESC);

-- ============================================================
-- system_account_role_status（システム管理ロールステータス）
-- ============================================================
CREATE TABLE system_account_role_status (
    role_id      TEXT        NOT NULL,
    version      TIMESTAMPTZ NOT NULL,
    status       TEXT        NOT NULL, -- ACTIVE / INACTIVE
    reason       TEXT,
    created_at   TIMESTAMPTZ NOT NULL,
    created_by   TEXT        NOT NULL,
    CONSTRAINT system_account_role_status_pk PRIMARY KEY (role_id, version)
);

CREATE INDEX system_account_role_status_idx_01
    ON system_account_role_status (role_id, version DESC);

CREATE INDEX system_account_role_status_idx_02
    ON system_account_role_status (status, version DESC);

-- ============================================================
-- system_oidc_providers（システムOIDCプロバイダー設定）
-- ============================================================
CREATE TABLE system_oidc_providers (
    provider_id    TEXT        NOT NULL,
    version        TIMESTAMPTZ NOT NULL,
    display_name   TEXT        NOT NULL,
    uri            TEXT        NOT NULL,
    client_id      TEXT        NOT NULL,
    client_secret  TEXT        NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL,
    created_by     TEXT        NOT NULL,
    CONSTRAINT system_oidc_providers_pk PRIMARY KEY (provider_id, version)
);

-- ============================================================
-- system_oidc_provider_status（システムOIDCプロバイダーステータス）
-- ============================================================
CREATE TABLE system_oidc_provider_status (
    provider_id    TEXT        NOT NULL,
    version        TIMESTAMPTZ NOT NULL,
    status         TEXT        NOT NULL, -- ENABLED / DISABLED
    reason         TEXT,
    created_at     TIMESTAMPTZ NOT NULL,
    created_by     TEXT        NOT NULL,
    CONSTRAINT system_oidc_provider_status_pk PRIMARY KEY (provider_id, version)
);

CREATE INDEX system_oidc_provider_status_idx_01
    ON system_oidc_provider_status (provider_id, version DESC);

CREATE INDEX system_oidc_provider_status_idx_02
    ON system_oidc_provider_status (status, version DESC);

-- ============================================================
-- tenants（業務テナント基本情報）
-- ============================================================
CREATE TABLE tenants (
    tenant_id    TEXT        NOT NULL,
    version      TIMESTAMPTZ NOT NULL,
    name         TEXT        NOT NULL,
    slug         TEXT        NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL,
    created_by   TEXT        NOT NULL,
    CONSTRAINT tenants_pk PRIMARY KEY (tenant_id, version)
);

CREATE INDEX tenants_idx_01
    ON tenants (slug, version DESC);

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
-- tenant_hosts（業務テナント識別ホスト）
-- ============================================================
CREATE TABLE tenant_hosts (
    tenant_id    TEXT        NOT NULL,
    host         TEXT        NOT NULL,
    version      TIMESTAMPTZ NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL,
    created_by   TEXT        NOT NULL,
    CONSTRAINT tenant_hosts_pk PRIMARY KEY (tenant_id, host, version)
);

CREATE INDEX tenant_hosts_idx_01
    ON tenant_hosts (host, version DESC);

CREATE INDEX tenant_hosts_idx_02
    ON tenant_hosts (tenant_id, version DESC);

-- ============================================================
-- tenant_host_status（業務テナント識別ホストステータス）
-- ============================================================
CREATE TABLE tenant_host_status (
    tenant_id    TEXT        NOT NULL,
    host         TEXT        NOT NULL,
    version      TIMESTAMPTZ NOT NULL,
    status       TEXT        NOT NULL, -- ACTIVE / INACTIVE
    reason       TEXT,
    created_at   TIMESTAMPTZ NOT NULL,
    created_by   TEXT        NOT NULL,
    CONSTRAINT tenant_host_status_pk PRIMARY KEY (tenant_id, host, version)
);

CREATE INDEX tenant_host_status_idx_01
    ON tenant_host_status (tenant_id, host, version DESC);

CREATE INDEX tenant_host_status_idx_02
    ON tenant_host_status (status, version DESC);

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
-- tenant_oidc_config_status（業務テナントOIDC設定ステータス）
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
-- tenant_accounts（業務テナントアカウント本体）
-- ============================================================
CREATE TABLE tenant_accounts (
    account_id   TEXT        NOT NULL,
    version      TIMESTAMPTZ NOT NULL,
    tenant_id    TEXT        NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL,
    created_by   TEXT        NOT NULL,
    CONSTRAINT tenant_accounts_pk PRIMARY KEY (account_id, version)
);

CREATE INDEX tenant_accounts_idx_01
    ON tenant_accounts (tenant_id, version DESC);

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

-- ============================================================
-- tenant_account_identities（業務テナントアカウント認証方法）
-- ============================================================
CREATE TABLE tenant_account_identities (
    identity_id  TEXT        NOT NULL,
    version      TIMESTAMPTZ NOT NULL,
    account_id   TEXT        NOT NULL,
    tenant_id    TEXT        NOT NULL,
    iss          TEXT        NOT NULL,
    aud          TEXT        NOT NULL,
    sub          TEXT        NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL,
    created_by   TEXT        NOT NULL,
    CONSTRAINT tenant_account_identities_pk PRIMARY KEY (identity_id, version)
);

CREATE INDEX tenant_account_identities_idx_01
    ON tenant_account_identities (tenant_id, iss, aud, sub, version DESC);

CREATE INDEX tenant_account_identities_idx_02
    ON tenant_account_identities (account_id, version DESC);

-- ============================================================
-- tenant_account_identity_status（業務テナントアカウント認証方法ステータス）
-- ============================================================
CREATE TABLE tenant_account_identity_status (
    identity_id  TEXT        NOT NULL,
    version      TIMESTAMPTZ NOT NULL,
    status       TEXT        NOT NULL, -- ACTIVE / INACTIVE
    reason       TEXT,
    created_at   TIMESTAMPTZ NOT NULL,
    created_by   TEXT        NOT NULL,
    CONSTRAINT tenant_account_identity_status_pk PRIMARY KEY (identity_id, version)
);

CREATE INDEX tenant_account_identity_status_idx_01
    ON tenant_account_identity_status (identity_id, version DESC);

CREATE INDEX tenant_account_identity_status_idx_02
    ON tenant_account_identity_status (status, version DESC);

-- ============================================================
-- tenant_account_roles（業務テナントアカウントロール）
-- ============================================================
CREATE TABLE tenant_account_roles (
    role_id      TEXT        NOT NULL,
    version      TIMESTAMPTZ NOT NULL,
    account_id   TEXT        NOT NULL,
    role         TEXT        NOT NULL, -- TENANT_ADMIN / EMPLOYEE
    created_at   TIMESTAMPTZ NOT NULL,
    created_by   TEXT        NOT NULL,
    CONSTRAINT tenant_account_roles_pk PRIMARY KEY (role_id, version)
);

CREATE INDEX tenant_account_roles_idx_01
    ON tenant_account_roles (account_id, version DESC);

CREATE INDEX tenant_account_roles_idx_02
    ON tenant_account_roles (role, version DESC);

-- ============================================================
-- tenant_account_role_status（業務テナントアカウントロールステータス）
-- ============================================================
CREATE TABLE tenant_account_role_status (
    role_id      TEXT        NOT NULL,
    version      TIMESTAMPTZ NOT NULL,
    status       TEXT        NOT NULL, -- ACTIVE / INACTIVE
    reason       TEXT,
    created_at   TIMESTAMPTZ NOT NULL,
    created_by   TEXT        NOT NULL,
    CONSTRAINT tenant_account_role_status_pk PRIMARY KEY (role_id, version)
);

CREATE INDEX tenant_account_role_status_idx_01
    ON tenant_account_role_status (role_id, version DESC);

CREATE INDEX tenant_account_role_status_idx_02
    ON tenant_account_role_status (status, version DESC);

-- ============================================================
-- system_admin_invitations（システム管理者招待）
-- ============================================================
CREATE TABLE system_admin_invitations (
    invitation_id  TEXT        NOT NULL,
    version        TIMESTAMPTZ NOT NULL,
    token_hash     TEXT        NOT NULL,
    expires_at     TIMESTAMPTZ NOT NULL,
    display_name   TEXT        NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL,
    created_by     TEXT        NOT NULL,
    CONSTRAINT system_admin_invitations_pk PRIMARY KEY (invitation_id, version)
);

CREATE UNIQUE INDEX system_admin_invitations_uk_01
    ON system_admin_invitations (token_hash);

-- ============================================================
-- system_admin_invitation_status（システム管理者招待ステータス）
-- ============================================================
CREATE TABLE system_admin_invitation_status (
    invitation_id  TEXT        NOT NULL,
    version        TIMESTAMPTZ NOT NULL,
    status         TEXT        NOT NULL,
    reason         TEXT,
    created_at     TIMESTAMPTZ NOT NULL,
    created_by     TEXT        NOT NULL,
    CONSTRAINT system_admin_invitation_status_pk PRIMARY KEY (invitation_id, version)
);

CREATE INDEX system_admin_invitation_status_idx_01
    ON system_admin_invitation_status (invitation_id, version DESC);

    CREATE INDEX system_admin_invitation_status_idx_02
    ON system_admin_invitation_status (status, version DESC);

-- ============================================================
-- system_account_localization（システム管理アカウント言語・タイムゾーン設定）
-- ============================================================
CREATE TABLE system_account_localization (
    account_id    TEXT        NOT NULL,
    version       TIMESTAMPTZ NOT NULL,
    language_code TEXT        NOT NULL,
    timezone_id   TEXT        NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL,
    created_by    TEXT        NOT NULL,
    CONSTRAINT system_account_localization_pk PRIMARY KEY (account_id, version)
);

CREATE INDEX system_account_localization_idx_01
    ON system_account_localization (account_id, version DESC);

CREATE INDEX system_account_localization_idx_02
    ON system_account_localization (language_code, version DESC);

CREATE INDEX system_account_localization_idx_03
    ON system_account_localization (timezone_id, version DESC);
    