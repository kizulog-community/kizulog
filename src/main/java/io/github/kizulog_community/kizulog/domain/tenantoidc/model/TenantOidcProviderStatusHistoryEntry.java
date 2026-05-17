package io.github.kizulog_community.kizulog.domain.tenantoidc.model;

import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 業務テナントOIDCプロバイダーのステータス変更履歴エントリ
 *
 * @author Jun Kobayashi
 */
@Getter
@AllArgsConstructor
public class TenantOidcProviderStatusHistoryEntry {

    /** バージョン（変更日時として表示） */
    private final OffsetDateTime version;

    /** ステータス値 */
    private final TenantOidcProviderStatusValue status;

    /** 変更理由 */
    private final String reason;

    /** 実行者 */
    private final String createdBy;

}
