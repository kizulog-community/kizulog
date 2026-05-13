package io.github.kizulog_community.kizulog.domain.systemadmininvitation.exception;

/**
 * 管理者招待機能の例外
 *
 * @author Jun Kobayashi
 */
public class InvitationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final InvitationError error;

    /**
     * コンストラクタ
     *
     * @param error エラー種別
     */
    public InvitationException(InvitationError error) {
        super("InvitationException: " + error.name());
        this.error = error;
    }

    /**
     * エラー種別を取得する
     *
     * @return エラー種別
     */
    public InvitationError getError() {
        return error;
    }

}
