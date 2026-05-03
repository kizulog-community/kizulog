package io.github.kizulog_community.kizulog.infrastructure.security.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.registration.ClientRegistration;

import io.github.kizulog_community.kizulog.domain.systemauth.model.SystemOidcSetting;
import io.github.kizulog_community.kizulog.domain.systemauth.model.SystemOidcSettings;
import io.github.kizulog_community.kizulog.domain.systemauth.service.SystemOidcSettingService;
import io.github.kizulog_community.kizulog.domain.systemconfig.service.OidcProviderService;

/**
 * DynamicSystemClientRegistrationRepositoryの単体テスト
 *
 * @author Jun Kobayashi
 */
class DynamicSystemClientRegistrationRepositoryTest {

    private static final String ISSUER = "https://auth.example/realms/master";
    private static final OffsetDateTime VERSION_1 =
            OffsetDateTime.of(2026, 5, 1, 12, 0, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime VERSION_2 =
            OffsetDateTime.of(2026, 5, 2, 12, 0, 0, 0, ZoneOffset.UTC);

    private SystemOidcSettingService systemOidcSettingService;
    private OidcProviderService oidcProviderService;
    private DynamicSystemClientRegistrationRepository repository;

    @BeforeEach
    void setUp() {
        systemOidcSettingService = mock(SystemOidcSettingService.class);
        oidcProviderService = mock(OidcProviderService.class);
        repository = new DynamicSystemClientRegistrationRepository(
                systemOidcSettingService, oidcProviderService);
    }

    /**
     * OIDC Discoveryメタデータの最小セットを生成する。
     *
     * <p>ClientRegistrations.fromOidcConfigurationが要求する必須フィールドを含む。
     * いずれのOIDCプロバイダーであっても準拠すべき
     * OpenID Connect Discovery 1.0仕様に基づく。</p>
     */
    private Map<String, Object> sampleMetadata() {
        return Map.of(
                "issuer", ISSUER,
                "authorization_endpoint", ISSUER + "/oauth2/authorize",
                "token_endpoint", ISSUER + "/oauth2/token",
                "userinfo_endpoint", ISSUER + "/oauth2/userinfo",
                "jwks_uri", ISSUER + "/oauth2/jwks",
                "subject_types_supported", List.of("public"),
                "id_token_signing_alg_values_supported", List.of("RS256"),
                "response_types_supported", List.of("code"),
                "scopes_supported", List.of("openid"));
    }

    /**
     * テスト用のSystemOidcSettingsを生成する。
     */
    private SystemOidcSettings settingsWith(OffsetDateTime version, String id) {
        SystemOidcSetting setting = new SystemOidcSetting(
                id, ISSUER, "kizulog-master", "decrypted-secret");
        return new SystemOidcSettings(version, List.of(setting));
    }

    @Test
    @DisplayName("findByRegistrationId：nullを渡すとnullが返る")
    void findByRegistrationId_withNull_returnsNull() {
        ClientRegistration result = repository.findByRegistrationId(null);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("findByRegistrationId：master以外のIDではnullが返る")
    void findByRegistrationId_withOtherId_returnsNull() {
        ClientRegistration result = repository.findByRegistrationId("other-id");

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("findByRegistrationId：OIDC設定がない場合はnull")
    void findByRegistrationId_whenNoSettings_returnsNull() {
        when(systemOidcSettingService.findLatest()).thenReturn(Optional.empty());

        ClientRegistration result = repository.findByRegistrationId("master");

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("findByRegistrationId：master IDの設定がない場合はnull")
    void findByRegistrationId_whenNoMasterSetting_returnsNull() {
        // master以外のIDだけがある状態
        when(systemOidcSettingService.findLatest())
                .thenReturn(Optional.of(settingsWith(VERSION_1, "other")));

        ClientRegistration result = repository.findByRegistrationId("master");

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("findByRegistrationId：正常系でClientRegistrationが返される")
    void findByRegistrationId_returnsClientRegistration() {
        when(systemOidcSettingService.findLatest())
                .thenReturn(Optional.of(settingsWith(VERSION_1, "master")));
        when(oidcProviderService.getMetadata(ISSUER)).thenReturn(sampleMetadata());

        ClientRegistration result = repository.findByRegistrationId("master");

        assertThat(result).isNotNull();
        assertThat(result.getRegistrationId()).isEqualTo("master");
        assertThat(result.getClientId()).isEqualTo("kizulog-master");
        assertThat(result.getClientSecret()).isEqualTo("decrypted-secret");
        assertThat(result.getScopes()).contains("openid");
        assertThat(result.getRedirectUri())
                .isEqualTo("{baseUrl}/login/oauth2/code/{registrationId}");
        assertThat(result.getProviderDetails().getAuthorizationUri())
                .isEqualTo(ISSUER + "/oauth2/authorize");
        assertThat(result.getProviderDetails().getTokenUri())
                .isEqualTo(ISSUER + "/oauth2/token");
    }

    @Test
    @DisplayName("findByRegistrationId：同じバージョンで2回呼ぶとキャッシュが効きgetMetadataは1回のみ")
    void findByRegistrationId_cachesByVersion() {
        when(systemOidcSettingService.findLatest())
                .thenReturn(Optional.of(settingsWith(VERSION_1, "master")));
        when(oidcProviderService.getMetadata(ISSUER)).thenReturn(sampleMetadata());

        ClientRegistration first = repository.findByRegistrationId("master");
        ClientRegistration second = repository.findByRegistrationId("master");

        assertThat(first).isSameAs(second);
        verify(oidcProviderService, times(1)).getMetadata(anyString());
    }

    @Test
    @DisplayName("findByRegistrationId：バージョンが変わると再構築される")
    void findByRegistrationId_rebuildsWhenVersionChanges() {
        when(oidcProviderService.getMetadata(ISSUER)).thenReturn(sampleMetadata());

        // 1回目：VERSION_1
        when(systemOidcSettingService.findLatest())
                .thenReturn(Optional.of(settingsWith(VERSION_1, "master")));
        ClientRegistration first = repository.findByRegistrationId("master");

        // 2回目：VERSION_2に変わった
        when(systemOidcSettingService.findLatest())
                .thenReturn(Optional.of(settingsWith(VERSION_2, "master")));
        ClientRegistration second = repository.findByRegistrationId("master");

        assertThat(first).isNotSameAs(second);
        verify(oidcProviderService, times(2)).getMetadata(anyString());
    }

    @Test
    @DisplayName("findByRegistrationId：getMetadataは設定のuriで呼ばれる")
    void findByRegistrationId_callsGetMetadataWithSettingUri() {
        when(systemOidcSettingService.findLatest())
                .thenReturn(Optional.of(settingsWith(VERSION_1, "master")));
        when(oidcProviderService.getMetadata(ISSUER)).thenReturn(sampleMetadata());

        repository.findByRegistrationId("master");

        verify(oidcProviderService, atLeastOnce()).getMetadata(ISSUER);
    }

}
