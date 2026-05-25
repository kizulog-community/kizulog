package io.github.kizulog_community.kizulog.domain.tenantaccount.model;

import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 業務テナントアカウントロールドメインモデル
 *
 * @author Jun Kobayashi
 */
@Getter
@AllArgsConstructor
public class TenantAccountRole {

    /** ロール紐付けID */
    private final String roleId;

    /** バージョン */
    private final OffsetDateTime version;

    /** 紐付くアカウントID */
    private final String accountId;

    /** ロール */
    private final TenantRole role;

    /** 作成日時（UTC） */
    private final OffsetDateTime createdAt;

    /** 作成者 */
    private final String createdBy;

}
