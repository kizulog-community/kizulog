package io.github.kizulog_community.kizulog.infrastructure.web.system.systemsettings.dto;

import java.time.OffsetDateTime;

import io.github.kizulog_community.kizulog.domain.systemoidc.model.OidcProviderStatusValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * OIDCプロバイダー詳細画面用ViewDTO
 *
 * @author Jun Kobayashi
 */
@Getter
@RequiredArgsConstructor
public final class OidcProviderDetailView {

    /** プロバイダーID */
    private final String providerId;

    /** 表示名 */
    private final String displayName;

    /** OIDC Issuer URI */
    private final String uri;

    /** OAuth2 Client ID */
    private final String clientId;

    /** OAuth2 Client Secret マスク表示文字列 */
    private final String clientSecretMasked;

    /** ステータス */
    private final OidcProviderStatusValue status;

    /** 理由 */
    private final String statusReason;

    /** 連携中のACTIVE identity数 */
    private final int activeIdentityCount;

    /** 作成日時 */
    private final OffsetDateTime createdAt;

    /** 作成者 */
    private final String createdBy;

    /**
     * 有効状態かを判定する（テンプレート用）。
     *
     * @return ENABLEDの場合true
     */
    public boolean isEnabled() {
        return status == OidcProviderStatusValue.ENABLED;
    }

}
