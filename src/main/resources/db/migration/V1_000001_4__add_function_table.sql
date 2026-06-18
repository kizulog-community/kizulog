-- ============================================================
-- KizuLog 機能追加スキーマ
-- ============================================================

-- ============================================================
-- tenant_attendance_punches（テナント利用者 打刻イベント）
-- ============================================================
CREATE TABLE tenant_attendance_punches (
    punch_id     TEXT        NOT NULL,
    version      TIMESTAMPTZ NOT NULL,
    account_id   TEXT        NOT NULL,
    tenant_id    TEXT        NOT NULL,
    punch_type   TEXT        NOT NULL,
    punched_at   TIMESTAMPTZ NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL,
    created_by   TEXT        NOT NULL,
    CONSTRAINT tenant_attendance_punches_pk PRIMARY KEY (punch_id, version)
);

CREATE INDEX tenant_attendance_punches_idx_01
    ON tenant_attendance_punches (account_id, punched_at DESC);

CREATE INDEX tenant_attendance_punches_idx_02
    ON tenant_attendance_punches (punch_id, version DESC);
