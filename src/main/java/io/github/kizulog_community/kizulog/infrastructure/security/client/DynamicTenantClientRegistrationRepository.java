package io.github.kizulog_community.kizulog.infrastructure.security.client;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;
import org.springframework.stereotype.Component;

import io.github.kizulog_community.kizulog.domain.systemconfig.service.OidcProviderService;
import io.github.kizulog_community.kizulog.domain.tenantoidc.model.DecryptedTenantOidcProvider;
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

    /** registrationId の固定プレフィックス */
    private static final String PREFIX = "tenant-";

    /** UUID の文字列長（8-4-4-4-12 = 36文字） */
    private static final int UUID_LENGTH = 36;

    /** UUID 形式の検証パターン */
    private static final Pattern UUID_PATTERN = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}"
            + "-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    /** providerId 形式の検証パターン */
    private static final Pattern PROVIDER_ID_PATTERN =
            Pattern.compile("^[a-z0-9-]{1,32}$");

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
        ParsedRegistrationId parsed = parse(registrationId);
        if (parsed == null) {
            return null;
        }

        Optional<DecryptedTenantOidcProvider> opt =
                tenantOidcProviderService.findEnabledForAuthentication(
                        parsed.tenantId, parsed.providerId);
        if (opt.isEmpty()) {
            log.warn("有効なテナントOIDCプロバイダーが見つかりません。"
                    + "tenantId={}, providerId={}", parsed.tenantId, parsed.providerId);
            return null;
        }

        return getCachedOrBuild(registrationId, opt.get());
    }

    /**
     * registrationId をパースして tenantId と providerId を取り出す。
     *
     * @param registrationId 登録ID
     * @return パース結果。不正な場合はnull
     */
    private ParsedRegistrationId parse(String registrationId) {
        if (registrationId == null || !registrationId.startsWith(PREFIX)) {
            return null;
        }
        // "tenant-" (7) + UUID(36) + "-" (1) + providerId(>=1)
        int minLength = PREFIX.length() + UUID_LENGTH + 1 + 1;
        if (registrationId.length() < minLength) {
            return null;
        }
        int uuidStart = PREFIX.length();
        int uuidEnd = uuidStart + UUID_LENGTH;
        String tenantId = registrationId.substring(uuidStart, uuidEnd);
        // UUID直後はハイフン区切りであること
        if (registrationId.charAt(uuidEnd) != '-') {
            return null;
        }
        String providerId = registrationId.substring(uuidEnd + 1);

        if (!UUID_PATTERN.matcher(tenantId).matches()) {
            return null;
        }
        if (!PROVIDER_ID_PATTERN.matcher(providerId).matches()) {
            return null;
        }
        return new ParsedRegistrationId(tenantId, providerId);
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
     * パース済みregistrationId
     */
    private static final class ParsedRegistrationId {

        private final String tenantId;
        private final String providerId;

        private ParsedRegistrationId(String tenantId, String providerId) {
            this.tenantId = tenantId;
            this.providerId = providerId;
        }

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
