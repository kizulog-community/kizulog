package io.github.kizulog_community.kizulog.domain.tenantoidc.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * テナントログイン画面表示用のOIDCプロバイダービュー
 *
 * @author Jun Kobayashi
 */
@Getter
@AllArgsConstructor
public class EnabledTenantOidcProviderView {

    /** プロバイダー識別子 */
    private final String providerId;

    /** 表示名（ボタンのラベル） */
    private final String displayName;

}
