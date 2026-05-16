package io.github.kizulog_community.kizulog.domain.tenant.model;

import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 業務テナントステータスドメインモデル
 *
 * @author Jun Kobayashi
 */
@Getter
@AllArgsConstructor
public class TenantStatus {

    /** テナントID */
    private final String tenantId;

    /** バージョン */
    private final OffsetDateTime version;

    /** ステータス値 */
    private final TenantStatusValue status;

    /** 変更理由（1-1000文字、必須） */
    private final String reason;

    /** 作成日時（UTC） */
    private final OffsetDateTime createdAt;

    /** 作成者 */
    private final String createdBy;

}
