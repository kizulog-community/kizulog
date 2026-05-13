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
