package io.github.kizulog_community.kizulog.domain.tenantoidc.exception;

/**
 * 業務テナントOIDCプロバイダー新規登録のエラー種別
 *
 * @author Jun Kobayashi
 */
public enum TenantOidcProviderRegistrationError {

    /** 親テナントが存在しない */
    TENANT_NOT_FOUND,

    /** provider_id が形式不正（[a-z0-9-]+ 1-32文字に違反） */
    PROVIDER_ID_INVALID,

    /** 同一テナント内で provider_id が重複（過去含む） */
    PROVIDER_ID_DUPLICATE,

    /** 表示名が空、または100文字超過 */
    DISPLAY_NAME_INVALID,

    /** iss が空、または不正形式 */
    ISS_INVALID,

    /** aud が空 */
    AUD_INVALID,

    /** 同一テナント内で (iss, aud) が重複（過去含む） */
    ISS_AUD_DUPLICATE,

    /** client_id が空、または255文字超過 */
    CLIENT_ID_INVALID,

    /** client_secret が空 */
    CLIENT_SECRET_INVALID,

    /** 変更理由が空、または1000文字超過 */
    REASON_INVALID,

}
