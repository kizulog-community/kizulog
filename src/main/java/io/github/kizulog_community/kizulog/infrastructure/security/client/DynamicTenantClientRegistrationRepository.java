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
import io.github.kizulog_community.kizulog.domain.tenantoidc.model.DecryptedTenantOidcProvider;
import io.github.kizulog_community.kizulog.domain.tenantoidc.model.TenantOidcRegistrationId;
import io.github.kizulog_community.kizulog.domain.tenantoidc.service.TenantOidcProviderService;
import lombok.RequiredArgsConstructor;

/**
 * テナント利用者用ClientRegistrationリポジトリ
 *
 * @author Jun Kobayashi
 */
@Component
@RequiredArgsConstructor
public class DynamicTenantClientRegistrationRepository
        implements ClientRegistrationRepository {

    /** ロガー */
    private static final Logger log =
            LoggerFactory.getLogger(DynamicTenantClientRegistrationRepository.class);

    /** OIDCプロバイダーサービス（テナント側） */
    private final TenantOidcProviderService tenantOidcProviderService;

    /** OIDCプロバイダーメタデータサービス（discovery用、システム側と共用） */
    private final OidcProviderService oidcProviderService;

    /** (tenantId|providerId) 毎のキャッシュ */
    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

    /**
     * registrationIdに対応するClientRegistrationを返す。
     *
     * @param registrationId 登録ID（tenant-{tenantId}-{providerId} 形式）
     * @return ClientRegistration、該当なしの場合はnull
     */
    @Override
    public ClientRegistration findByRegistrationId(String registrationId) {
        Optional<TenantOidcRegistrationId> parsedOpt =
                TenantOidcRegistrationId.parse(registrationId);
        if (parsedOpt.isEmpty()) {
            return null;
        }
        TenantOidcRegistrationId parsed = parsedOpt.get();

        Optional<DecryptedTenantOidcProvider> opt =
                tenantOidcProviderService.findEnabledForAuthentication(
                        parsed.getTenantId(), parsed.getProviderId());
        if (opt.isEmpty()) {
            log.warn("有効なテナントOIDCプロバイダーが見つかりません。"
                    + "tenantId={}, providerId={}", parsed.getTenantId(), parsed.getProviderId());
            return null;
        }

        return getCachedOrBuild(registrationId, opt.get());
    }

    /**
     * キャッシュから取得、なければ構築してキャッシュする。
     *
     * @param registrationId 登録ID（キャッシュキー）
     * @param provider 復号済みOIDCプロバイダー
     * @return ClientRegistration
     */
    private ClientRegistration getCachedOrBuild(
            String registrationId, DecryptedTenantOidcProvider provider) {
        CacheEntry current = cache.get(registrationId);
        if (current != null && current.version.equals(provider.getVersion())) {
            return current.registration;
        }

        ClientRegistration registration = buildClientRegistration(registrationId, provider);
        cache.put(registrationId, new CacheEntry(provider.getVersion(), registration));
        log.info("テナントClientRegistrationを構築しました。"
                + "tenantId={}, providerId={}, version={}",
                provider.getTenantId(), provider.getProviderId(), provider.getVersion());
        return registration;
    }

    /**
     * 復号済みOIDCプロバイダーからClientRegistrationを構築する。
     *
     * @param registrationId 登録ID
     * @param provider 復号済みOIDCプロバイダー
     * @return 構築済みClientRegistration
     */
    private ClientRegistration buildClientRegistration(
            String registrationId, DecryptedTenantOidcProvider provider) {
        Map<String, Object> metadata = oidcProviderService.getMetadata(provider.getIss());
        return ClientRegistrations.fromOidcConfiguration(metadata)
                .registrationId(registrationId)
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
