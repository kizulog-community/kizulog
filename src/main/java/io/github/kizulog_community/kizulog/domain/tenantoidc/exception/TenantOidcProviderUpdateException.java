package io.github.kizulog_community.kizulog.domain.tenantoidc.exception;

import lombok.Getter;

/**
 * 業務テナントOIDCプロバイダー更新の業務例外
 *
 * @author Jun Kobayashi
 */
@Getter
public class TenantOidcProviderUpdateException extends RuntimeException {

    private static final long serialVersionUID = 1L;

	/** エラー種別 */
    private final TenantOidcProviderUpdateError error;

    /**
     * コンストラクタ
     *
     * @param error エラー種別
     */
    public TenantOidcProviderUpdateException(
            TenantOidcProviderUpdateError error) {
        super("Tenant OIDC provider update failed: " + error);
        this.error = error;
    }

}
