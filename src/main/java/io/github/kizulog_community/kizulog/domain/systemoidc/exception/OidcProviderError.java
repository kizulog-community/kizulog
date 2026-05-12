package io.github.kizulog_community.kizulog.domain.systemoidc.exception;

/**
 * OIDCプロバイダー管理機能のエラー種別
 *
 * @author Jun Kobayashi
 */
public enum OidcProviderError {

    /** プロバイダーIDの形式が不正（[a-z0-9-]+ 1-32文字以外） */
    PROVIDER_ID_INVALID_FORMAT,

    /** プロバイダーIDが既に使用されている（過去含む） */
    PROVIDER_ID_DUPLICATE,

    /** プロバイダーが見つからない */
    PROVIDER_NOT_FOUND,

    /** 最低1つのENABLEDプロバイダーが必要（無効化時の制約違反） */
    LAST_ENABLED_REQUIRED,

    /** 既にENABLED状態（有効化操作時） */
    ALREADY_ENABLED,

    /** 既にDISABLED状態（無効化操作時） */
    ALREADY_DISABLED

}
