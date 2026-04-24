-- ============================================================
-- KizuLog 初期スキーマ
-- バージョン : 1.000001.1
-- 作成日     : 2026-04-24
-- 説明       : システム設定テーブルの初期定義
-- ============================================================

-- ------------------------------------------------------------
-- system_config
-- 説明 : セットアップウィザードで入力するシステム設定を管理する
--        設定はグループ単位でJSONB形式で保持する
--        例）key='OIDC', value={"URI":"...","CLIENT_ID":"..."}
--        機密情報（CLIENT_SECRET等）はAES-256-GCMで暗号化して格納する
--        Immutableテーブル：INSERTのみ許可・UPDATE/DELETE禁止
--        バージョン管理：同一keyの最新version（TIMESTAMPTZ最大値）が有効値
-- ------------------------------------------------------------
CREATE TABLE system_config (
    key        TEXT        NOT NULL, -- 設定グループ名（例：OIDC、SYSTEM）
    version    TIMESTAMPTZ NOT NULL, -- バージョン（更新日時、最大値が有効）
    value      JSONB       NOT NULL, -- 設定値（JSON形式）
    created_at TIMESTAMPTZ NOT NULL, -- 作成日時（UTC）
    created_by TEXT        NOT NULL, -- 作成者（例：user:uuid、batch:xxx、system:xxx）
    CONSTRAINT system_config_pk PRIMARY KEY (key, version)
);

ALTER TABLE system_config ENABLE ROW LEVEL SECURITY;
CREATE POLICY no_update ON system_config FOR UPDATE USING (false);
CREATE POLICY no_delete ON system_config FOR DELETE USING (false);
-- ------------------------------------------------------------

