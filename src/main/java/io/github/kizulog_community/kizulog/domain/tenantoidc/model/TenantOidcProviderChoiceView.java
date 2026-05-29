package io.github.kizulog_community.kizulog.domain.tenantoidc.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * テナント OIDC プロバイダー選択肢ビュー
 *
 * @author Jun Kobayashi
 */
@Getter
@AllArgsConstructor
public class TenantOidcProviderChoiceView {

    /** OAuth2 認可開始用の registrationId（tenant-{tenantId}-{providerId}） */
    private final String registrationId;

    /** ボタンの表示名 */
    private final String displayName;

}
