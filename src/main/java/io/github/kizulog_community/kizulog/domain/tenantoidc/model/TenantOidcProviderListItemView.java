package io.github.kizulog_community.kizulog.domain.tenantoidc.model;

import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 業務テナントOIDCプロバイダー一覧画面用のビュー
 *
 * @author Jun Kobayashi
 */
@Getter
@AllArgsConstructor
public class TenantOidcProviderListItemView {

    /** テナントID */
    private final String tenantId;

    /** プロバイダー識別子 */
    private final String providerId;

    /** 表示名 */
    private final String displayName;

    /** OIDC Issuer URI */
    private final String iss;

    /** Audience */
    private final String aud;

    /** 現在のステータス（最新version） */
    private final TenantOidcProviderStatusValue currentStatus;

    /** 作成日時 */
    private final OffsetDateTime createdAt;

}
