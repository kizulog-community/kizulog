package io.github.kizulog_community.kizulog.domain.tenantoidc.exception;

/**
 * 業務テナントOIDCプロバイダー更新（display_name / client_id / client_secret）のエラー種別
 *
 * @author Jun Kobayashi
 */
public enum TenantOidcProviderUpdateError {

    /** 親テナントが存在しない */
    TENANT_NOT_FOUND,

    /** 対象プロバイダーが存在しない */
    PROVIDER_NOT_FOUND,

    /** 表示名が空、または100文字超過 */
    DISPLAY_NAME_INVALID,

    /** client_id が空、または255文字超過 */
    CLIENT_ID_INVALID,

    /** client_secret が不正（入力時のみ検証、空文字なら現在値維持） */
    CLIENT_SECRET_INVALID,

    /** 変更理由が空、または1000文字超過 */
    REASON_INVALID,

}
