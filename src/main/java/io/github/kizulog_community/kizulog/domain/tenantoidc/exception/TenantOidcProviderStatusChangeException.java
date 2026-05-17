package io.github.kizulog_community.kizulog.domain.tenantoidc.exception;

import lombok.Getter;

/**
 * 業務テナントOIDCプロバイダー ステータス変更の業務例外
 *
 * @author Jun Kobayashi
 */
@Getter
public class TenantOidcProviderStatusChangeException extends RuntimeException {

    private static final long serialVersionUID = 1L;

	/** エラー種別 */
    private final TenantOidcProviderStatusChangeError error;

    /**
     * コンストラクタ
     *
     * @param error エラー種別
     */
    public TenantOidcProviderStatusChangeException(
            TenantOidcProviderStatusChangeError error) {
        super("Tenant OIDC provider status change failed: " + error);
        this.error = error;
    }

}
