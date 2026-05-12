package io.github.kizulog_community.kizulog.infrastructure.security.client;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;
import org.springframework.stereotype.Component;

import io.github.kizulog_community.kizulog.domain.systemconfig.service.OidcProviderService;
import io.github.kizulog_community.kizulog.domain.systemoidc.model.DecryptedOidcProvider;
import io.github.kizulog_community.kizulog.domain.systemoidc.service.SystemOidcProviderService;
import lombok.RequiredArgsConstructor;

/**
 * システム管理用ClientRegistrationリポジトリ
 *
 * <p>Spring SecurityのOAuth2Loginが認可リクエストを発行する際に呼び出され、
 * system_oidc_providersに登録されたOIDC設定をもとに ClientRegistration を動的に構築する。</p>
 *
 * <p>OIDCディスカバリは既存の OidcProviderService#getMetadata(String) 経由するため、
 * localプロファイルでの自己署名証明書サポート LocalRestClientConfig がそのまま適用される。</p>
 *
 * <p>provider_id毎のキャッシュを保持し、設定のversionが変わった際には自動的に再構築する。
 * 同一provider_id・同一versionの間は同一の ClientRegistration インスタンスを返却する。</p>
 *
 * @author Jun Kobayashi
 */
@Component
@RequiredArgsConstructor
public class DynamicSystemClientRegistrationRepository
        implements ClientRegistrationRepository {

    /** ロガー */
    private static final Logger log =
            LoggerFactory.getLogger(DynamicSystemClientRegistrationRepository.class);

    /** OIDCプロバイダーサービス */
    private final SystemOidcProviderService systemOidcProviderService;

    /** OIDCプロバイダーメタデータサービス */
    private final OidcProviderService oidcProviderService;

    /** provider_id毎のキャッシュ (キー: provider_id, 値: バージョン付きキャッシュエントリ) */
    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

    /**
     * registrationIdに対応するClientRegistrationを返す。
     *
     * <p>registrationIdがprovider_idとして扱われ、ENABLEDな設定が見つかった場合のみ
     * ClientRegistrationを返す。見つからなかった場合（未登録・DISABLED含む）はnullを返す。
     * Spring Securityフレームワーク側でnullを受け取ると404相当のエラーとなる。</p>
     *
     * @param registrationId 登録ID（provider_idとして扱う）
     * @return ClientRegistration、該当なしの場合はnull
     */
    @Override
    public ClientRegistration findByRegistrationId(String registrationId) {
        if (registrationId == null) {
            return null;
        }

        Optional<DecryptedOidcProvider> opt =
                systemOidcProviderService.findEnabledForAuthentication(registrationId);
        if (opt.isEmpty()) {
            log.warn("有効なシステムOIDCプロバイダーが見つかりません。registrationId={}",
                    registrationId);
            return null;
        }

        return getCachedOrBuild(opt.get());
    }

    /**
     * キャッシュから取得、なければ構築してキャッシュする。
     *
     * <p>provider_id毎にキャッシュし、versionが一致する間は同一インスタンスを返す。
     * versionが変わったら再構築してキャッシュを置き換える。</p>
     *
     * @param provider 復号済みOIDCプロバイダー
     * @return ClientRegistration
     */
    private ClientRegistration getCachedOrBuild(DecryptedOidcProvider provider) {
        CacheEntry current = cache.get(provider.getProviderId());
        if (current != null && current.version.equals(provider.getVersion())) {
            return current.registration;
        }

        ClientRegistration registration = buildClientRegistration(provider);
        cache.put(provider.getProviderId(), new CacheEntry(provider.getVersion(), registration));
        log.info("システムClientRegistrationを構築しました。providerId={}, version={}",
                provider.getProviderId(), provider.getVersion());
        return registration;
    }

    /**
     * 復号済みOIDCプロバイダーからClientRegistrationを構築する。
     *
     * <p>既存の OidcProviderService でメタデータを取得し、
     * Spring Securityの ClientRegistrations.fromOidcConfiguration で
     * ClientRegistrationを生成する。</p>
     *
     * @param provider 復号済みOIDCプロバイダー
     * @return 構築済みClientRegistration
     */
    private ClientRegistration buildClientRegistration(DecryptedOidcProvider provider) {
        Map<String, Object> metadata = oidcProviderService.getMetadata(provider.getUri());
        return ClientRegistrations.fromOidcConfiguration(metadata)
                .registrationId(provider.getProviderId())
                .clientId(provider.getClientId())
                .clientSecret(provider.getClientSecret())
                .scope("openid")
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .build();
    }

    /**
     * キャッシュエントリ
     */
    private static final class CacheEntry {

        private final OffsetDateTime version;
        private final ClientRegistration registration;

        private CacheEntry(OffsetDateTime version, ClientRegistration registration) {
            this.version = Objects.requireNonNull(version);
            this.registration = Objects.requireNonNull(registration);
        }

    }

}
