package io.github.kizulog_community.kizulog.domain.tenantoidc.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * テナント管理者 招待受諾画面のプロバイダー選択肢ビュー
 *
 * @author Jun Kobayashi
 */
@Getter
@AllArgsConstructor
public class TenantInviteProviderChoiceView {

    /** OAuth2 認可開始用の registrationId（tenant-{tenantId}-{providerId}） */
    private final String registrationId;

    /** ボタンの表示名 */
    private final String displayName;

}
