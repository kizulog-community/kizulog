-- ============================================================
-- KizuLog 機能追加スキーマ
-- ============================================================

-- ============================================================
-- system_account_profiles（システム管理アカウントプロファイルキャッシュ）
-- ============================================================
CREATE TABLE system_account_profiles (
    identity_id  TEXT        NOT NULL,
    version      TIMESTAMPTZ NOT NULL,
    claims       JSONB       NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL,
    created_by   TEXT        NOT NULL,
    CONSTRAINT system_account_profiles_pk PRIMARY KEY (identity_id, version)
);

CREATE INDEX system_account_profiles_idx_01
    ON system_account_profiles (identity_id, version DESC);

-- ============================================================
-- system_oidc_providers（再作成）
-- ============================================================
DROP TABLE IF EXISTS system_oidc_providers CASCADE;

CREATE TABLE system_oidc_providers (
    provider_id     TEXT        NOT NULL,
    version         TIMESTAMPTZ NOT NULL,
    display_name    TEXT        NOT NULL,
    uri             TEXT        NOT NULL,
    client_id       TEXT        NOT NULL,
    client_secret   TEXT        NOT NULL,
    claims_mapping  JSONB       NOT NULL DEFAULT '{
      "familyName": "family_name",
      "givenName": "given_name",
      "middleName": "middle_name",
      "organization": "organization",
      "email": "email"
    }'::jsonb,
    created_at      TIMESTAMPTZ NOT NULL,
    created_by      TEXT        NOT NULL,
    CONSTRAINT system_oidc_providers_pk PRIMARY KEY (provider_id, version)
);

CREATE INDEX system_oidc_providers_idx_01
    ON system_oidc_providers (provider_id, version DESC);
