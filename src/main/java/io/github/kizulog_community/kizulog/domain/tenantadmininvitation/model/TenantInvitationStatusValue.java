package io.github.kizulog_community.kizulog.domain.tenantadmininvitation.model;

/**
 * テナント管理者招待ステータス
 *
 * @author Jun Kobayashi
 */
public enum TenantInvitationStatusValue {

    /** 受諾待ち（招待発行直後の初期状態） */
    PENDING,

    /** 使用済み（受諾されてアカウント作成完了） */
    USED,

    /** 取消済み（発行者により取消された） */
    CANCELLED;

    /**
     * このステータスの招待が受諾可能かを判定する。
     *
     * <p>受諾可能なのは PENDING のみ。
     * USED/CANCELLED は受諾不可。</p>
     *
     * @return 受諾可能なら true
     */
    public boolean isAcceptable() {
        return this == PENDING;
    }

}
