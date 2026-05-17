package io.github.kizulog_community.kizulog.domain.tenantoidc.exception;

import lombok.Getter;

/**
 * 業務テナントOIDCプロバイダー新規登録の業務例外
 *
 * @author Jun Kobayashi
 */
@Getter
public class TenantOidcProviderRegistrationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

	/** エラー種別 */
    private final TenantOidcProviderRegistrationError error;

    /**
     * コンストラクタ
     *
     * @param error エラー種別
     */
    public TenantOidcProviderRegistrationException(
            TenantOidcProviderRegistrationError error) {
        super("Tenant OIDC provider registration failed: " + error);
        this.error = error;
    }

}
