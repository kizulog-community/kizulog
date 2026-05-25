package io.github.kizulog_community.kizulog.domain.tenantaccount.model;

import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 業務テナントアカウントドメインモデル
 *
 * @author Jun Kobayashi
 */
@Getter
@AllArgsConstructor
public class TenantAccount {

    /** アカウントID（UUID） */
    private final String accountId;

    /** バージョン */
    private final OffsetDateTime version;

    /** 所属テナントID */
    private final String tenantId;

    /** 作成日時（UTC） */
    private final OffsetDateTime createdAt;

    /** 作成者 */
    private final String createdBy;

}
