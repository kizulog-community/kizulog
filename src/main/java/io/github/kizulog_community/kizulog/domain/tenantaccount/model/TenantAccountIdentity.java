package io.github.kizulog_community.kizulog.domain.tenantaccount.model;

import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 業務テナントアカウント認証方法ドメインモデル
 *
 * @author Jun Kobayashi
 */
@Getter
@AllArgsConstructor
public class TenantAccountIdentity {

    /** アイデンティティID（UUID） */
    private final String identityId;

    /** バージョン */
    private final OffsetDateTime version;

    /** 紐付くアカウントID */
    private final String accountId;

    /** 所属テナントID */
    private final String tenantId;

    /** OIDC Issuer URI */
    private final String iss;

    /** OIDC Client ID（Audience） */
    private final String aud;

    /** OIDC Subject */
    private final String sub;

    /** 作成日時（UTC） */
    private final OffsetDateTime createdAt;

    /** 作成者 */
    private final String createdBy;

}
