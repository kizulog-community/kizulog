package io.github.kizulog_community.kizulog.domain.tenantoidc.model;

import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 業務テナントOIDCプロバイダー
 *
 * @author Jun Kobayashi
 */
@Getter
@AllArgsConstructor
public class TenantOidcProvider {

    /** テナントID */
    private final String tenantId;

    /** プロバイダー識別子（テナント内ユニーク、[a-z0-9-]+ 1-32文字、不変） */
    private final String providerId;

    /** バージョン */
    private final OffsetDateTime version;

    /** 表示名（管理画面とテナントログイン画面で表示） */
    private final String displayName;

    /** OIDC Issuer URI（不変） */
    private final String iss;

    /** Audience（不変） */
    private final String aud;

    /** クライアントID */
    private final String clientId;

    /** クライアントシークレット（AES暗号化済み） */
    private final String clientSecret;

    /** 作成日時 */
    private final OffsetDateTime createdAt;

    /** 作成者 */
    private final String createdBy;

}
