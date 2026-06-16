-- ============================================================
-- KizuLog 機能追加スキーマ
-- ============================================================

-- ============================================================
-- tenant_oidc_providers: claims_mapping 列追加
-- ============================================================
ALTER TABLE tenant_oidc_providers
    ADD COLUMN claims_mapping JSONB NOT NULL DEFAULT '{
      "familyName": "family_name",
      "givenName": "given_name",
      "middleName": "middle_name",
      "organization": "organization",
      "email": "email"
    }'::jsonb;

-- ============================================================
-- tenant_account_profiles（テナント利用者プロファイルキャッシュ）
-- ============================================================
CREATE TABLE tenant_account_profiles (
    identity_id  TEXT        NOT NULL,
    version      TIMESTAMPTZ NOT NULL,
    claims       JSONB       NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL,
    created_by   TEXT        NOT NULL,
    CONSTRAINT tenant_account_profiles_pk PRIMARY KEY (identity_id, version)
);

CREATE INDEX tenant_account_profiles_idx_01
    ON tenant_account_profiles (identity_id, version DESC);
