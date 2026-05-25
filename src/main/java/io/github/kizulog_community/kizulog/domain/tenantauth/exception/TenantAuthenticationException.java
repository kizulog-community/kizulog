package io.github.kizulog_community.kizulog.domain.tenantauth.exception;

import org.springframework.security.core.AuthenticationException;

import lombok.Getter;

/**
 * テナント利用者認証例外
 *
 * @author Jun Kobayashi
 */
@Getter
public class TenantAuthenticationException extends AuthenticationException {

    private static final long serialVersionUID = 1L;

    /** エラー種別 */
    private final TenantAuthenticationErrorType errorType;

    /**
     * コンストラクタ
     *
     * @param errorType エラー種別
     */
    public TenantAuthenticationException(TenantAuthenticationErrorType errorType) {
        super(errorType == null ? "errorType=null" : errorType.name());
        this.errorType = errorType;
    }

    /**
     * コンストラクタ（原因例外あり）
     *
     * @param errorType エラー種別
     * @param cause 原因例外
     */
    public TenantAuthenticationException(
            TenantAuthenticationErrorType errorType, Throwable cause) {
        super(errorType == null ? "errorType=null" : errorType.name(), cause);
        this.errorType = errorType;
    }

}
