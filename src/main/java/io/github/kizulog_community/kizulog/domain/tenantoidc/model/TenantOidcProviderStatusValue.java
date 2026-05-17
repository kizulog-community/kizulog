package io.github.kizulog_community.kizulog.domain.tenantoidc.model;

/**
 * 業務テナントOIDCプロバイダーのステータス値
 *
 * @author Jun Kobayashi
 */
public enum TenantOidcProviderStatusValue {

    /** 有効: テナントログイン画面に表示され、ログインに利用可能 */
    ENABLED,

    /** 無効: テナントログイン画面に表示されない */
    DISABLED,

}