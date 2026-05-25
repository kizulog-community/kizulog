package io.github.kizulog_community.kizulog.domain.tenantaccount.model;

import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 業務テナントアカウントロールステータスドメインモデル
 *
 * @author Jun Kobayashi
 */
@Getter
@AllArgsConstructor
public class TenantAccountRoleStatus {

    /** ロールID */
    private final String roleId;

    /** バージョン */
    private final OffsetDateTime version;

    /** ステータス */
    private final TenantAccountStatusValue status;

    /** 理由 */
    private final String reason;

    /** 作成日時（UTC） */
    private final OffsetDateTime createdAt;

    /** 作成者 */
    private final String createdBy;

}
