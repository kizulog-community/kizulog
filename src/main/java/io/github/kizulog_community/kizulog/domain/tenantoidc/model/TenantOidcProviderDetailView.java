package io.github.kizulog_community.kizulog.domain.tenantoidc.model;

import java.time.OffsetDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 業務テナントOIDCプロバイダー詳細画面用のビュー
 *
 * @author Jun Kobayashi
 */
@Getter
@AllArgsConstructor
public class TenantOidcProviderDetailView {

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

    /** クライアントID */
    private final String clientId;

    /** プロバイダー本体の最新バージョン */
    private final OffsetDateTime providerVersion;

    /** プロバイダー本体の作成日時 */
    private final OffsetDateTime createdAt;

    /** プロバイダー本体の作成者 */
    private final String createdBy;

    /** 現在のステータス値 */
    private final TenantOidcProviderStatusValue currentStatus;

    /** 現在ステータスのreason */
    private final String currentStatusReason;

    /** 現在ステータスのversion */
    private final OffsetDateTime currentStatusVersion;

    /** 現在ステータスの実行者 */
    private final String currentStatusUpdatedBy;

    /** ステータス変更履歴（新しい順） */
    private final List<TenantOidcProviderStatusHistoryEntry> statusHistory;

}
