package io.github.kizulog_community.kizulog.infrastructure.web.system.systemsettings.dto;

import io.github.kizulog_community.kizulog.domain.systemoidc.model.OidcProviderStatusValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * OIDCプロバイダー一覧画面用ViewDTO
 *
 * <p>テンプレートで参照する1行分のデータを表現する不変オブジェクト。</p>
 *
 * @author Jun Kobayashi
 */
@Getter
@RequiredArgsConstructor
public final class OidcProviderListItem {

    /** プロバイダーID */
    private final String providerId;

    /** 表示名 */
    private final String displayName;

    /** ステータス */
    private final OidcProviderStatusValue status;

    /** 連携中のACTIVE identity数 */
    private final int activeIdentityCount;

    /**
     * 有効状態かを判定する。
     *
     * @return ENABLEDの場合true
     */
    public boolean isEnabled() {
        return status == OidcProviderStatusValue.ENABLED;
    }

}
