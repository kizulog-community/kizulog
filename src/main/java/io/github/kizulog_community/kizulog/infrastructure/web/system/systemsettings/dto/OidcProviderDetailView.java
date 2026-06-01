package io.github.kizulog_community.kizulog.infrastructure.web.system.systemsettings.dto;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Map;

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

    /** クレームマッピング設定 */
    private final Map<String, String> claimsMapping;

    /** 作成日時 */
    private final OffsetDateTime createdAt;

    /** 作成者 */
    private final String createdBy;

    /**
     * 有効状態かを判定する。
     *
     * @return ENABLEDの場合true
     */
    public boolean isEnabled() {
        return status == OidcProviderStatusValue.ENABLED;
    }

    /**
     * 指定キーのマッピング値を取得する。
     *
     * @param key ClaimsMappingTarget#getKey の戻り値
     * @return マッピング値、または未設定の場合空文字列
     */
    public String getClaimMapping(String key) {
        if (claimsMapping == null || key == null) {
            return "";
        }
        String value = claimsMapping.get(key);
        return value == null ? "" : value;
    }

    /**
     * クレームマッピングのコピーを返す。
     *
     * @return マッピングのコピー、または null の場合は空Map
     */
    public Map<String, String> getClaimsMappingView() {
        return claimsMapping == null ? Collections.emptyMap() : claimsMapping;
    }

}
