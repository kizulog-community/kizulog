package io.github.kizulog_community.kizulog.domain.tenantoidc.exception;

/**
 * 業務テナントOIDCプロバイダーのステータス変更（ENABLED/DISABLED）のエラー種別
 *
 * @author Jun Kobayashi
 */
public enum TenantOidcProviderStatusChangeError {

    /** 親テナントが存在しない */
    TENANT_NOT_FOUND,

    /** 対象プロバイダーが存在しない */
    PROVIDER_NOT_FOUND,

    /** 既に指定のステータス（同一遷移を防ぐ） */
    ALREADY_IN_TARGET_STATUS,

    /** 変更理由が空、または1000文字超過 */
    REASON_INVALID,

}
