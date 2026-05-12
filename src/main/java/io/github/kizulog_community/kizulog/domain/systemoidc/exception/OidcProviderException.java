package io.github.kizulog_community.kizulog.domain.systemoidc.exception;

/**
 * OIDCプロバイダー管理機能の例外
 *
 * @author Jun Kobayashi
 */
public class OidcProviderException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final OidcProviderError error;

    /**
     * コンストラクタ
     *
     * @param error エラー種別
     */
    public OidcProviderException(OidcProviderError error) {
        super("OidcProviderException: " + error.name());
        this.error = error;
    }

    /**
     * エラー種別を取得する
     *
     * @return エラー種別
     */
    public OidcProviderError getError() {
        return error;
    }

}
