package io.github.kizulog_community.kizulog.domain.tenantoidc.model;

import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 業務テナントOIDCプロバイダーのステータス変更履歴
 *
 * @author Jun Kobayashi
 */
@Getter
@AllArgsConstructor
public class TenantOidcProviderStatus {

    /** テナントID */
    private final String tenantId;

    /** プロバイダー識別子 */
    private final String providerId;

    /** バージョン */
    private final OffsetDateTime version;

    /** ステータス値 */
    private final TenantOidcProviderStatusValue status;

    /** 変更理由 */
    private final String reason;

    /** 作成日時 */
    private final OffsetDateTime createdAt;

    /** 作成者 */
    private final String createdBy;

}
