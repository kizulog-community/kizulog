package io.github.kizulog_community.kizulog.domain.tenantoidc.model;

import java.time.OffsetDateTime;
import java.util.Objects;

import lombok.Getter;
import lombok.ToString;

/**
 * 復号済み業務テナントOIDCプロバイダー
 *
 * @author Jun Kobayashi
 */
@Getter
@ToString(exclude = "clientSecret")
public final class DecryptedTenantOidcProvider {

    /** テナントID */
    private final String tenantId;

    /** プロバイダー識別子 */
    private final String providerId;

    /** バージョン（キャッシュキーとして利用） */
    private final OffsetDateTime version;

    /** 表示名 */
    private final String displayName;

    /** OIDC Issuer URI */
    private final String iss;

    /** Audience */
    private final String aud;

    /** OAuth2 Client ID */
    private final String clientId;

    /** OAuth2 Client Secret（復号済みの平文） */
    private final String clientSecret;

    /**
     * コンストラクタ
     *
     * @param tenantId テナントID
     * @param providerId プロバイダー識別子
     * @param version バージョン
     * @param displayName 表示名
     * @param iss Issuer URI
     * @param aud Audience
     * @param clientId Client ID
     * @param clientSecret 復号済みClient Secret
     */
    public DecryptedTenantOidcProvider(
            String tenantId,
            String providerId,
            OffsetDateTime version,
            String displayName,
            String iss,
            String aud,
            String clientId,
            String clientSecret) {
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId must not be null");
        this.providerId = Objects.requireNonNull(providerId, "providerId must not be null");
        this.version = Objects.requireNonNull(version, "version must not be null");
        this.displayName = Objects.requireNonNull(displayName, "displayName must not be null");
        this.iss = Objects.requireNonNull(iss, "iss must not be null");
        this.aud = Objects.requireNonNull(aud, "aud must not be null");
        this.clientId = Objects.requireNonNull(clientId, "clientId must not be null");
        this.clientSecret = Objects.requireNonNull(clientSecret, "clientSecret must not be null");
    }

}
