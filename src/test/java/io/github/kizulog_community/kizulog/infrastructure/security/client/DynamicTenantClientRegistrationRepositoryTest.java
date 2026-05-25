package io.github.kizulog_community.kizulog.infrastructure.security.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
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
import io.github.kizulog_community.kizulog.domain.tenantoidc.model.DecryptedTenantOidcProvider;
import io.github.kizulog_community.kizulog.domain.tenantoidc.service.TenantOidcProviderService;

/**
 * DynamicTenantClientRegistrationRepositoryの単体テスト
 *
 * @author Jun Kobayashi
 */
class DynamicTenantClientRegistrationRepositoryTest {

    private static final String TENANT_ID = "550e8400-e29b-41d4-a716-446655440000";
    private static final String PROVIDER_ID = "keycloak";
    private static final String REG_ID = "tenant-" + TENANT_ID + "-" + PROVIDER_ID;
    private static final String ISSUER = "https://auth.acme.example/realms/acme";
    private static final String ISSUER_2 = "https://auth.beta.example/realms/beta";
    private static final OffsetDateTime VERSION_1 =
            OffsetDateTime.of(2026, 5, 1, 12, 0, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime VERSION_2 =
            OffsetDateTime.of(2026, 5, 2, 12, 0, 0, 0, ZoneOffset.UTC);

    private TenantOidcProviderService tenantOidcProviderService;
    private OidcProviderService oidcProviderService;
    private DynamicTenantClientRegistrationRepository repository;

    @BeforeEach
    void setUp() {
        tenantOidcProviderService = mock(TenantOidcProviderService.class);
        oidcProviderService = mock(OidcProviderService.class);
        repository = new DynamicTenantClientRegistrationRepository(
                tenantOidcProviderService, oidcProviderService);
    }

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

    private DecryptedTenantOidcProvider providerOf(
            String tenantId, String providerId, String iss, OffsetDateTime version) {
        return new DecryptedTenantOidcProvider(
                tenantId, providerId, version,
                "Display " + providerId, iss,
                "kizulog-" + providerId,
                "kizulog-" + providerId + "-client",
                "decrypted-secret-" + providerId);
    }

    @Test
    @DisplayName("findByRegistrationId: nullを渡すとnull")
    void findByRegistrationId_withNull_returnsNull() {
        assertThat(repository.findByRegistrationId(null)).isNull();
    }

    @Test
    @DisplayName("findByRegistrationId: プレフィックスが違う場合はnull")
    void findByRegistrationId_withWrongPrefix_returnsNull() {
        assertThat(repository.findByRegistrationId(
                "system-" + TENANT_ID + "-" + PROVIDER_ID)).isNull();
    }

    @Test
    @DisplayName("findByRegistrationId: UUID部分が不正な形式の場合はnull")
    void findByRegistrationId_withInvalidUuid_returnsNull() {
        // UUIDの代わりに同じ長さ(36)だが形式不正な文字列
        String badUuid = "zzzzzzzz-zzzz-zzzz-zzzz-zzzzzzzzzzzz";
        assertThat(repository.findByRegistrationId(
                "tenant-" + badUuid + "-" + PROVIDER_ID)).isNull();
    }

    @Test
    @DisplayName("findByRegistrationId: UUID直後が区切りハイフンでない場合はnull")
    void findByRegistrationId_withoutSeparator_returnsNull() {
        // "tenant-" + UUID + "x..." （UUID直後がハイフンでない）
        assertThat(repository.findByRegistrationId(
                "tenant-" + TENANT_ID + "x" + PROVIDER_ID)).isNull();
    }

    @Test
    @DisplayName("findByRegistrationId: providerId部分が空の場合はnull")
    void findByRegistrationId_withEmptyProviderId_returnsNull() {
        // "tenant-" + UUID + "-" で終わる（providerIdなし）
        assertThat(repository.findByRegistrationId(
                "tenant-" + TENANT_ID + "-")).isNull();
    }

    @Test
    @DisplayName("findByRegistrationId: providerId部分が形式不正(大文字)の場合はnull")
    void findByRegistrationId_withInvalidProviderId_returnsNull() {
        assertThat(repository.findByRegistrationId(
                "tenant-" + TENANT_ID + "-INVALID")).isNull();
    }

    @Test
    @DisplayName("findByRegistrationId: 全体が短すぎる場合はnull")
    void findByRegistrationId_tooShort_returnsNull() {
        assertThat(repository.findByRegistrationId("tenant-short")).isNull();
    }

    @Test
    @DisplayName("findByRegistrationId: ENABLEDなプロバイダーがない場合はnull")
    void findByRegistrationId_whenNotEnabled_returnsNull() {
        when(tenantOidcProviderService.findEnabledForAuthentication(TENANT_ID, PROVIDER_ID))
                .thenReturn(Optional.empty());

        assertThat(repository.findByRegistrationId(REG_ID)).isNull();
    }

    @Test
    @DisplayName("findByRegistrationId: 正常系でClientRegistrationが返される")
    void findByRegistrationId_returnsClientRegistration() {
        when(tenantOidcProviderService.findEnabledForAuthentication(TENANT_ID, PROVIDER_ID))
                .thenReturn(Optional.of(providerOf(TENANT_ID, PROVIDER_ID, ISSUER, VERSION_1)));
        when(oidcProviderService.getMetadata(ISSUER)).thenReturn(sampleMetadata(ISSUER));

        ClientRegistration result = repository.findByRegistrationId(REG_ID);

        assertThat(result).isNotNull();
        assertThat(result.getRegistrationId()).isEqualTo(REG_ID);
        assertThat(result.getClientId()).isEqualTo("kizulog-keycloak-client");
        assertThat(result.getClientSecret()).isEqualTo("decrypted-secret-keycloak");
        assertThat(result.getScopes()).contains("openid");
        assertThat(result.getRedirectUri())
                .isEqualTo("{baseUrl}/login/oauth2/code/{registrationId}");
        assertThat(result.getProviderDetails().getAuthorizationUri())
                .isEqualTo(ISSUER + "/oauth2/authorize");
    }

    @Test
    @DisplayName("findByRegistrationId: パース済みtenantId/providerIdでfindEnabledForAuthenticationが呼ばれる")
    void findByRegistrationId_callsServiceWithParsedIds() {
        when(tenantOidcProviderService.findEnabledForAuthentication(TENANT_ID, PROVIDER_ID))
                .thenReturn(Optional.of(providerOf(TENANT_ID, PROVIDER_ID, ISSUER, VERSION_1)));
        when(oidcProviderService.getMetadata(ISSUER)).thenReturn(sampleMetadata(ISSUER));

        repository.findByRegistrationId(REG_ID);

        verify(tenantOidcProviderService)
                .findEnabledForAuthentication(TENANT_ID, PROVIDER_ID);
    }

    @Test
    @DisplayName("findByRegistrationId: getMetadataはプロバイダーのissで呼ばれる")
    void findByRegistrationId_callsGetMetadataWithIss() {
        when(tenantOidcProviderService.findEnabledForAuthentication(TENANT_ID, PROVIDER_ID))
                .thenReturn(Optional.of(providerOf(TENANT_ID, PROVIDER_ID, ISSUER, VERSION_1)));
        when(oidcProviderService.getMetadata(ISSUER)).thenReturn(sampleMetadata(ISSUER));

        repository.findByRegistrationId(REG_ID);

        verify(oidcProviderService).getMetadata(ISSUER);
    }

    @Test
    @DisplayName("findByRegistrationId: 同じバージョンで2回呼ぶとキャッシュが効きgetMetadataは1回のみ")
    void findByRegistrationId_cachesByVersion() {
        when(tenantOidcProviderService.findEnabledForAuthentication(TENANT_ID, PROVIDER_ID))
                .thenReturn(Optional.of(providerOf(TENANT_ID, PROVIDER_ID, ISSUER, VERSION_1)));
        when(oidcProviderService.getMetadata(ISSUER)).thenReturn(sampleMetadata(ISSUER));

        ClientRegistration first = repository.findByRegistrationId(REG_ID);
        ClientRegistration second = repository.findByRegistrationId(REG_ID);

        assertThat(first).isSameAs(second);
        verify(oidcProviderService, times(1)).getMetadata(anyString());
    }

    @Test
    @DisplayName("findByRegistrationId: バージョンが変わると再構築される")
    void findByRegistrationId_rebuildsWhenVersionChanges() {
        when(oidcProviderService.getMetadata(ISSUER)).thenReturn(sampleMetadata(ISSUER));

        when(tenantOidcProviderService.findEnabledForAuthentication(TENANT_ID, PROVIDER_ID))
                .thenReturn(Optional.of(providerOf(TENANT_ID, PROVIDER_ID, ISSUER, VERSION_1)));
        ClientRegistration first = repository.findByRegistrationId(REG_ID);

        when(tenantOidcProviderService.findEnabledForAuthentication(TENANT_ID, PROVIDER_ID))
                .thenReturn(Optional.of(providerOf(TENANT_ID, PROVIDER_ID, ISSUER, VERSION_2)));
        ClientRegistration second = repository.findByRegistrationId(REG_ID);

        assertThat(first).isNotSameAs(second);
        verify(oidcProviderService, times(2)).getMetadata(anyString());
    }

    @Test
    @DisplayName("findByRegistrationId: 異なるテナントのregistrationIdは独立にキャッシュされる")
    void findByRegistrationId_cachesPerRegistrationId() {
        String tenant2 = "660e8400-e29b-41d4-a716-446655440001";
        String regId2 = "tenant-" + tenant2 + "-" + PROVIDER_ID;

        when(tenantOidcProviderService.findEnabledForAuthentication(TENANT_ID, PROVIDER_ID))
                .thenReturn(Optional.of(providerOf(TENANT_ID, PROVIDER_ID, ISSUER, VERSION_1)));
        when(tenantOidcProviderService.findEnabledForAuthentication(tenant2, PROVIDER_ID))
                .thenReturn(Optional.of(providerOf(tenant2, PROVIDER_ID, ISSUER_2, VERSION_1)));
        when(oidcProviderService.getMetadata(ISSUER)).thenReturn(sampleMetadata(ISSUER));
        when(oidcProviderService.getMetadata(ISSUER_2)).thenReturn(sampleMetadata(ISSUER_2));

        ClientRegistration reg1a = repository.findByRegistrationId(REG_ID);
        ClientRegistration reg2 = repository.findByRegistrationId(regId2);
        ClientRegistration reg1b = repository.findByRegistrationId(REG_ID);

        assertThat(reg1a).isSameAs(reg1b);
        assertThat(reg1a).isNotSameAs(reg2);
        verify(oidcProviderService, times(1)).getMetadata(ISSUER);
        verify(oidcProviderService, times(1)).getMetadata(ISSUER_2);
    }

}
