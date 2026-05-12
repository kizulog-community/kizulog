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

import io.github.kizulog_community.kizulog.domain.systemconfig.service.OidcProviderService;
import io.github.kizulog_community.kizulog.domain.systemoidc.model.DecryptedOidcProvider;
import io.github.kizulog_community.kizulog.domain.systemoidc.service.SystemOidcProviderService;

/**
 * DynamicSystemClientRegistrationRepositoryの単体テスト
 *
 * @author Jun Kobayashi
 */
class DynamicSystemClientRegistrationRepositoryTest {

    private static final String MASTER_PROVIDER_ID = "master";
    private static final String GOOGLE_PROVIDER_ID = "google";
    private static final String ISSUER = "https://auth.example/realms/master";
    private static final String GOOGLE_ISSUER = "https://accounts.google.com";
    private static final OffsetDateTime VERSION_1 =
            OffsetDateTime.of(2026, 5, 1, 12, 0, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime VERSION_2 =
            OffsetDateTime.of(2026, 5, 2, 12, 0, 0, 0, ZoneOffset.UTC);

    private SystemOidcProviderService systemOidcProviderService;
    private OidcProviderService oidcProviderService;
    private DynamicSystemClientRegistrationRepository repository;

    @BeforeEach
    void setUp() {
        systemOidcProviderService = mock(SystemOidcProviderService.class);
        oidcProviderService = mock(OidcProviderService.class);
        repository = new DynamicSystemClientRegistrationRepository(
                systemOidcProviderService, oidcProviderService);
    }

    /**
     * OIDC Discoveryメタデータの最小セットを生成する。
     */
    private Map<String, Object> sampleMetadata(String issuer) {
        return Map.of(
                "issuer", issuer,
                "authorization_endpoint", issuer + "/oauth2/authorize",
                "token_endpoint", issuer + "/oauth2/token",
                "userinfo_endpoint", issuer + "/oauth2/userinfo",
                "jwks_uri", issuer + "/oauth2/jwks",
                "subject_types_supported", List.of("public"),
                "id_token_signing_alg_values_supported", List.of("RS256"),
                "response_types_supported", List.of("code"),
                "scopes_supported", List.of("openid"));
    }

    /**
     * テスト用の DecryptedOidcProvider を生成する。
     */
    private DecryptedOidcProvider providerOf(
            String providerId, String issuer, OffsetDateTime version) {
        return new DecryptedOidcProvider(
                providerId, version,
                "Display " + providerId, issuer,
                "kizulog-" + providerId, "decrypted-secret-" + providerId);
    }

    @Test
    @DisplayName("findByRegistrationId: nullを渡すとnullが返る")
    void findByRegistrationId_withNull_returnsNull() {
        ClientRegistration result = repository.findByRegistrationId(null);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("findByRegistrationId: 該当プロバイダーがない場合はnull")
    void findByRegistrationId_whenNoProvider_returnsNull() {
        when(systemOidcProviderService.findEnabledForAuthentication("not-exist"))
                .thenReturn(Optional.empty());

        ClientRegistration result = repository.findByRegistrationId("not-exist");

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("findByRegistrationId: 正常系でClientRegistrationが返される")
    void findByRegistrationId_returnsClientRegistration() {
        when(systemOidcProviderService.findEnabledForAuthentication(MASTER_PROVIDER_ID))
                .thenReturn(Optional.of(providerOf(MASTER_PROVIDER_ID, ISSUER, VERSION_1)));
        when(oidcProviderService.getMetadata(ISSUER)).thenReturn(sampleMetadata(ISSUER));

        ClientRegistration result = repository.findByRegistrationId(MASTER_PROVIDER_ID);

        assertThat(result).isNotNull();
        assertThat(result.getRegistrationId()).isEqualTo(MASTER_PROVIDER_ID);
        assertThat(result.getClientId()).isEqualTo("kizulog-master");
        assertThat(result.getClientSecret()).isEqualTo("decrypted-secret-master");
        assertThat(result.getScopes()).contains("openid");
        assertThat(result.getRedirectUri())
                .isEqualTo("{baseUrl}/login/oauth2/code/{registrationId}");
        assertThat(result.getProviderDetails().getAuthorizationUri())
                .isEqualTo(ISSUER + "/oauth2/authorize");
        assertThat(result.getProviderDetails().getTokenUri())
                .isEqualTo(ISSUER + "/oauth2/token");
    }

    @Test
    @DisplayName("findByRegistrationId: 同じバージョンで2回呼ぶとキャッシュが効きgetMetadataは1回のみ")
    void findByRegistrationId_cachesByVersion() {
        when(systemOidcProviderService.findEnabledForAuthentication(MASTER_PROVIDER_ID))
                .thenReturn(Optional.of(providerOf(MASTER_PROVIDER_ID, ISSUER, VERSION_1)));
        when(oidcProviderService.getMetadata(ISSUER)).thenReturn(sampleMetadata(ISSUER));

        ClientRegistration first = repository.findByRegistrationId(MASTER_PROVIDER_ID);
        ClientRegistration second = repository.findByRegistrationId(MASTER_PROVIDER_ID);

        assertThat(first).isSameAs(second);
        verify(oidcProviderService, times(1)).getMetadata(anyString());
    }

    @Test
    @DisplayName("findByRegistrationId: 同一provider_idでもバージョンが変わると再構築される")
    void findByRegistrationId_rebuildsWhenVersionChanges() {
        when(oidcProviderService.getMetadata(ISSUER)).thenReturn(sampleMetadata(ISSUER));

        // 1回目: VERSION_1
        when(systemOidcProviderService.findEnabledForAuthentication(MASTER_PROVIDER_ID))
                .thenReturn(Optional.of(providerOf(MASTER_PROVIDER_ID, ISSUER, VERSION_1)));
        ClientRegistration first = repository.findByRegistrationId(MASTER_PROVIDER_ID);

        // 2回目: VERSION_2 (同一provider_idだがversion変更)
        when(systemOidcProviderService.findEnabledForAuthentication(MASTER_PROVIDER_ID))
                .thenReturn(Optional.of(providerOf(MASTER_PROVIDER_ID, ISSUER, VERSION_2)));
        ClientRegistration second = repository.findByRegistrationId(MASTER_PROVIDER_ID);

        assertThat(first).isNotSameAs(second);
        verify(oidcProviderService, times(2)).getMetadata(anyString());
    }

    @Test
    @DisplayName("findByRegistrationId: 異なるprovider_idはそれぞれ独立にキャッシュされる")
    void findByRegistrationId_cachesPerProviderId() {
        when(systemOidcProviderService.findEnabledForAuthentication(MASTER_PROVIDER_ID))
                .thenReturn(Optional.of(providerOf(MASTER_PROVIDER_ID, ISSUER, VERSION_1)));
        when(systemOidcProviderService.findEnabledForAuthentication(GOOGLE_PROVIDER_ID))
                .thenReturn(Optional.of(providerOf(GOOGLE_PROVIDER_ID, GOOGLE_ISSUER, VERSION_1)));
        when(oidcProviderService.getMetadata(ISSUER)).thenReturn(sampleMetadata(ISSUER));
        when(oidcProviderService.getMetadata(GOOGLE_ISSUER)).thenReturn(sampleMetadata(GOOGLE_ISSUER));

        ClientRegistration master1 = repository.findByRegistrationId(MASTER_PROVIDER_ID);
        ClientRegistration google1 = repository.findByRegistrationId(GOOGLE_PROVIDER_ID);
        ClientRegistration master2 = repository.findByRegistrationId(MASTER_PROVIDER_ID);

        // 同一provider_idは同一インスタンス
        assertThat(master1).isSameAs(master2);
        // 異なるprovider_idは別インスタンス
        assertThat(master1).isNotSameAs(google1);
        // メタデータ取得は各provider_id 1回ずつ
        verify(oidcProviderService, times(1)).getMetadata(ISSUER);
        verify(oidcProviderService, times(1)).getMetadata(GOOGLE_ISSUER);
    }

    @Test
    @DisplayName("findByRegistrationId: getMetadataはプロバイダーのuriで呼ばれる")
    void findByRegistrationId_callsGetMetadataWithProviderUri() {
        when(systemOidcProviderService.findEnabledForAuthentication(MASTER_PROVIDER_ID))
                .thenReturn(Optional.of(providerOf(MASTER_PROVIDER_ID, ISSUER, VERSION_1)));
        when(oidcProviderService.getMetadata(ISSUER)).thenReturn(sampleMetadata(ISSUER));

        repository.findByRegistrationId(MASTER_PROVIDER_ID);

        verify(oidcProviderService, atLeastOnce()).getMetadata(ISSUER);
    }

}
