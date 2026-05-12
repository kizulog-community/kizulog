package io.github.kizulog_community.kizulog.domain.systemoidc.model;

import java.time.OffsetDateTime;
import java.util.Objects;

import lombok.Getter;
import lombok.ToString;

/**
 * 復号済みOIDCプロバイダー（認証フロー用DTO）
 *
 * <p>SystemOidcProviderからclient_secretを復号した状態の不変オブジェクト。
 * 認証フロー（DynamicSystemClientRegistrationRepository）で
 * ClientRegistration構築の元として使用される。</p>
 *
 * <p>SystemOidcProviderモデル（永続化用、client_secretは暗号化済み）と
 * 型レベルで区別することで、復号忘れや誤った情報の表示を防ぐ。</p>
 *
 * <p>clientSecretは復号済みの平文を保持する。
 * 誤ってログ等に出力されないよう、toString()からは除外している。</p>
 *
 * @author Jun Kobayashi
 */
@Getter
@ToString(exclude = "clientSecret")
public final class DecryptedOidcProvider {

    /** プロバイダーID */
    private final String providerId;

    /** バージョン（キャッシュキーとして利用） */
    private final OffsetDateTime version;

    /** 表示名 */
    private final String displayName;

    /** OIDC Issuer URI */
    private final String uri;

    /** OAuth2 Client ID */
    private final String clientId;

    /** OAuth2 Client Secret（復号済みの平文） */
    private final String clientSecret;

    /**
     * コンストラクタ
     *
     * @param providerId プロバイダーID
     * @param version バージョン
     * @param displayName 表示名
     * @param uri Issuer URI
     * @param clientId Client ID
     * @param clientSecret 復号済みClient Secret
     */
    public DecryptedOidcProvider(
            String providerId,
            OffsetDateTime version,
            String displayName,
            String uri,
            String clientId,
            String clientSecret) {
        this.providerId = Objects.requireNonNull(providerId, "providerId must not be null");
        this.version = Objects.requireNonNull(version, "version must not be null");
        this.displayName = Objects.requireNonNull(displayName, "displayName must not be null");
        this.uri = Objects.requireNonNull(uri, "uri must not be null");
        this.clientId = Objects.requireNonNull(clientId, "clientId must not be null");
        this.clientSecret = Objects.requireNonNull(clientSecret, "clientSecret must not be null");
    }

}
