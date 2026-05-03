package io.github.kizulog_community.kizulog.infrastructure.security.client;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;
import org.springframework.stereotype.Component;

import io.github.kizulog_community.kizulog.domain.systemauth.model.SystemOidcSetting;
import io.github.kizulog_community.kizulog.domain.systemauth.model.SystemOidcSettings;
import io.github.kizulog_community.kizulog.domain.systemauth.service.SystemOidcSettingService;
import io.github.kizulog_community.kizulog.domain.systemconfig.service.OidcProviderService;
import lombok.RequiredArgsConstructor;

/**
 * システム管理用ClientRegistrationリポジトリ
 *
 * <p>Spring SecurityのOAuth2Loginが認可リクエストを発行する際に呼び出され、
 * system_configに保存されたOIDC設定をもとに ClientRegistration を動的に構築する。</p>
 *
 * <p>OIDCディスカバリは既存の OidcProviderService#getMetadata(String) 経由するため、
 * localプロファイルでの自己署名証明書サポート LocalRestClientConfig がそのまま適用される。</p>
 *
 * <p>system_config.versionをキーとしたキャッシュを保持し、設定変更時には自動的に再構築する。
 * バージョンが一致する間は同一の ClientRegistration インスタンスを返却する。</p>
 *
 * @author Jun Kobayashi
 */
@Component
@RequiredArgsConstructor
public class DynamicSystemClientRegistrationRepository
        implements ClientRegistrationRepository {

    /** システム管理OIDCに対応するregistrationId */
    public static final String SYSTEM_REGISTRATION_ID = "master";

    /** ロガー */
    private static final Logger log =
            LoggerFactory.getLogger(DynamicSystemClientRegistrationRepository.class);

    /** OIDC設定サービス */
    private final SystemOidcSettingService systemOidcSettingService;

    /** OIDCプロバイダーサービス（メタデータ取得） */
    private final OidcProviderService oidcProviderService;

    /** バージョン付きClientRegistrationキャッシュ */
    private final AtomicReference<CacheEntry> cacheRef = new AtomicReference<>();

    /**
     * registrationIdに対応するClientRegistrationを返す。
     *
     * <p>"master"以外、または設定が存在しない場合はnullを返す。
     * Spring Securityフレームワーク側でnullを受け取ると404相当のエラーとなる。</p>
     *
     * @param registrationId 登録ID
     * @return ClientRegistration、該当なしの場合はnull
     */
    @Override
    public ClientRegistration findByRegistrationId(String registrationId) {
        if (!SYSTEM_REGISTRATION_ID.equals(registrationId)) {
            return null;
        }

        Optional<SystemOidcSettings> opt = systemOidcSettingService.findLatest();
        if (opt.isEmpty()) {
            log.warn("システム管理OIDC設定が未登録です。セットアップが完了していない可能性があります。");
            return null;
        }

        SystemOidcSettings settings = opt.get();
        SystemOidcSetting target = findById(settings.getSettings(), SYSTEM_REGISTRATION_ID);
        if (target == null) {
            log.warn("システム管理OIDC設定にid='{}'の設定が見つかりません。", SYSTEM_REGISTRATION_ID);
            return null;
        }

        return getCachedOrBuild(settings.getVersion(), target);
    }

    /**
     * idに一致するOIDC設定を返す。
     *
     * @param settings OIDC設定リスト
     * @param id 探すid
     * @return 該当する設定、なければnull
     */
    private SystemOidcSetting findById(List<SystemOidcSetting> settings, String id) {
        for (SystemOidcSetting s : settings) {
            if (id.equals(s.getId())) {
                return s;
            }
        }
        return null;
    }

    /**
     * キャッシュから取得、なければ構築してキャッシュする。
     *
     * @param version バージョン
     * @param setting OIDC設定
     * @return ClientRegistration
     */
    private ClientRegistration getCachedOrBuild(
            OffsetDateTime version, SystemOidcSetting setting) {
        CacheEntry current = cacheRef.get();
        if (current != null && current.version.equals(version)) {
            return current.registration;
        }
        ClientRegistration registration = buildClientRegistration(setting);
        cacheRef.set(new CacheEntry(version, registration));
        log.info("システム管理ClientRegistrationを再構築しました。version={}", version);
        return registration;
    }

    /**
     * OIDC設定からClientRegistrationを構築する。
     *
     * <p>既存の OidcProviderService でメタデータを取得し、
     * Spring Securityの ClientRegistrations.fromOidcConfiguration で
     * ClientRegistrationを生成する。</p>
     *
     * @param setting OIDC設定
     * @return 構築済みClientRegistration
     */
    private ClientRegistration buildClientRegistration(SystemOidcSetting setting) {
        Map<String, Object> metadata = oidcProviderService.getMetadata(setting.getUri());
        return ClientRegistrations.fromOidcConfiguration(metadata)
                .registrationId(SYSTEM_REGISTRATION_ID)
                .clientId(setting.getClientId())
                .clientSecret(setting.getClientSecret())
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
