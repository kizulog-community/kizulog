package io.github.kizulog_community.kizulog.domain.tenantaccount.model;

import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 業務テナントアカウントステータスドメインモデル
 *
 * @author Jun Kobayashi
 */
@Getter
@AllArgsConstructor
public class TenantAccountStatus {

    /** アカウントID */
    private final String accountId;

    /** バージョン */
    private final OffsetDateTime version;

    /** ステータス（ACTIVE / INACTIVE / SUSPENDED） */
    private final TenantAccountStatusValue status;

    /** 変更理由（任意） */
    private final String reason;

    /** 作成日時（UTC） */
    private final OffsetDateTime createdAt;

    /** 作成者 */
    private final String createdBy;

}
