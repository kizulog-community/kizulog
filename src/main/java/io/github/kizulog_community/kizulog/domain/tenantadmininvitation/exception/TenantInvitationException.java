package io.github.kizulog_community.kizulog.domain.tenantadmininvitation.exception;

/**
 * テナント管理者招待機能の例外
 *
 * @author Jun Kobayashi
 */
public class TenantInvitationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final TenantInvitationError error;

    /**
     * コンストラクタ
     *
     * @param error エラー種別
     */
    public TenantInvitationException(TenantInvitationError error) {
        super("TenantInvitationException: " + error.name());
        this.error = error;
    }

    /**
     * エラー種別を取得する
     *
     * @return エラー種別
     */
    public TenantInvitationError getError() {
        return error;
    }

}
