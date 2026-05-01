package io.github.kizulog_community.kizulog.domain.systemconfig.exception;

/**
 * OIDC接続例外
 *
 * <p>OIDCプロバイダーへの接続処理で発生した異常を表現する。
 * エラーの種別は{@link OidcConnectionError}で識別する。</p>
 *
 * @author Jun Kobayashi
 */
public class OidcConnectionException extends RuntimeException {

    private static final long serialVersionUID = 1L;

/** エラー種別. */
    private final OidcConnectionError errorType;

    /**
     * コンストラクタ
     *
     * @param errorType エラー種別
     */
    public OidcConnectionException(OidcConnectionError errorType) {
        super(errorType.name());
        this.errorType = errorType;
    }

    /**
     * コンストラクタ（原因例外あり）
     *
     * @param errorType エラー種別
     * @param cause 原因例外
     */
    public OidcConnectionException(
    OidcConnectionError errorType, Throwable cause) {
        super(errorType.name(), cause);
        this.errorType = errorType;
    }

    /**
     * エラー種別を取得する。
     *
     * @return エラー種別
     */
    public OidcConnectionError getErrorType() {
        return errorType;
    }

}