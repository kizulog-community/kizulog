package io.github.kizulog_community.kizulog.domain.tenantoidc.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
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
import org.mockito.ArgumentCaptor;

import io.github.kizulog_community.kizulog.domain.port.CryptoPort;
import io.github.kizulog_community.kizulog.domain.tenant.model.Tenant;
import io.github.kizulog_community.kizulog.domain.tenant.port.TenantRepository;
import io.github.kizulog_community.kizulog.domain.tenantoidc.model.ClaimsMappingTarget;
import io.github.kizulog_community.kizulog.domain.tenantoidc.model.TenantOidcProvider;
import io.github.kizulog_community.kizulog.domain.tenantoidc.port.TenantOidcProviderRepository;
import io.github.kizulog_community.kizulog.domain.tenantoidc.port.TenantOidcProviderStatusRepository;

/**
 * TenantOidcProviderService の claims_mapping 反映に関する単体テスト
 *
 * @author Jun Kobayashi
 */
class TenantOidcProviderClaimsMappingTest {

    private static final OffsetDateTime BASE_TIME = OffsetDateTime.of(2026, 5, 1, 12, 0, 0, 0, ZoneOffset.UTC);
    private static final String TENANT_ID = "tenant-A";
    private static final String PROVIDER_ID = "google";
    private static final String ISS = "https://auth.example/realms/main";
    private static final String AUD = "aud";

    private TenantRepository tenantRepository;
    private TenantOidcProviderRepository providerRepository;
    private TenantOidcProviderStatusRepository statusRepository;
    private CryptoPort cryptoPort;
    private TenantOidcProviderService service;

    @BeforeEach
    void setUp() {
        tenantRepository = mock(TenantRepository.class);
        providerRepository = mock(TenantOidcProviderRepository.class);
        statusRepository = mock(TenantOidcProviderStatusRepository.class);
        cryptoPort = mock(CryptoPort.class);
        service = new TenantOidcProviderService(
                tenantRepository, providerRepository, statusRepository, cryptoPort);
    }

    private void mockTenantExists() {
        when(tenantRepository.findLatestByTenantId(TENANT_ID)).thenReturn(Optional.of(
                new Tenant(TENANT_ID, BASE_TIME, "Tenant", "slug", BASE_TIME, "creator")));
    }

    private void mockRegisterPreconditions() {
        mockTenantExists();
        when(providerRepository.existsByTenantIdAndProviderId(TENANT_ID, PROVIDER_ID))
                .thenReturn(false);
        when(providerRepository.findAllLatestByIssAndAud(ISS, AUD)).thenReturn(List.of());
        when(cryptoPort.encrypt("plain-secret")).thenReturn("encrypted-secret");
    }

    private TenantOidcProvider currentProvider(Map<String, String> mapping) {
        return new TenantOidcProvider(
                TENANT_ID, PROVIDER_ID, BASE_TIME,
                "Display", ISS, AUD, "client", "old-encrypted",
                mapping, BASE_TIME, "creator");
    }

    private TenantOidcProvider captureSavedProvider() {
        ArgumentCaptor<TenantOidcProvider> captor =
                ArgumentCaptor.forClass(TenantOidcProvider.class);
        verify(providerRepository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("registerProvider(10引数): 明示マッピングはトリム＋空欄除外で正規化保存される")
    void registerProvider_withExplicitMapping_savesNormalized() {
        mockRegisterPreconditions();

        service.registerProvider(
                TENANT_ID, PROVIDER_ID, "Display",
                ISS, AUD, "client-id", "plain-secret",
                Map.of("familyName", "  sn  ", "givenName", "   ", "email", "mail"),
                "reason", "operator-1");

        assertThat(captureSavedProvider().getClaimsMappingView())
                .isEqualTo(Map.of("familyName", "sn", "email", "mail"));
    }

    @Test
    @DisplayName("registerProvider(10引数): 空マッピングはデフォルトマッピングで保存される")
    void registerProvider_withEmptyMapping_savesDefault() {
        mockRegisterPreconditions();

        service.registerProvider(
                TENANT_ID, PROVIDER_ID, "Display",
                ISS, AUD, "client-id", "plain-secret",
                Map.of(),
                "reason", "operator-1");

        assertThat(captureSavedProvider().getClaimsMappingView())
                .isEqualTo(ClaimsMappingTarget.defaultMapping());
    }

    @Test
    @DisplayName("registerProvider(9引数・従来版): マッピング未指定はデフォルトで保存される")
    void registerProvider_legacyOverload_savesDefault() {
        mockRegisterPreconditions();

        service.registerProvider(
                TENANT_ID, PROVIDER_ID, "Display",
                ISS, AUD, "client-id", "plain-secret",
                "reason", "operator-1");

        assertThat(captureSavedProvider().getClaimsMappingView())
                .isEqualTo(ClaimsMappingTarget.defaultMapping());
    }

    @Test
    @DisplayName("updateProvider(8引数): 明示マッピングは正規化して保存される")
    void updateProvider_withExplicitMapping_savesNormalized() {
        mockTenantExists();
        when(providerRepository.findLatestByTenantIdAndProviderId(TENANT_ID, PROVIDER_ID))
                .thenReturn(Optional.of(currentProvider(ClaimsMappingTarget.defaultMapping())));

        service.updateProvider(
                TENANT_ID, PROVIDER_ID, "New Display", "new-client-id", "",
                Map.of("organization", "dept"),
                "reason", "operator-1");

        assertThat(captureSavedProvider().getClaimsMappingView())
                .isEqualTo(Map.of("organization", "dept"));
    }

    @Test
    @DisplayName("updateProvider(8引数): 空マッピングは現在のマッピングを維持する")
    void updateProvider_withEmptyMapping_keepsCurrent() {
        mockTenantExists();
        Map<String, String> currentMapping = ClaimsMappingTarget.defaultMapping();
        when(providerRepository.findLatestByTenantIdAndProviderId(TENANT_ID, PROVIDER_ID))
                .thenReturn(Optional.of(currentProvider(currentMapping)));

        service.updateProvider(
                TENANT_ID, PROVIDER_ID, "New Display", "new-client-id", "",
                Map.of(),
                "reason", "operator-1");

        assertThat(captureSavedProvider().getClaimsMappingView()).isEqualTo(currentMapping);
    }

    @Test
    @DisplayName("updateProvider(7引数・従来版): マッピング未指定は現在のマッピングを維持する")
    void updateProvider_legacyOverload_keepsCurrent() {
        mockTenantExists();
        Map<String, String> currentMapping = Map.of("familyName", "sn", "email", "mail");
        when(providerRepository.findLatestByTenantIdAndProviderId(TENANT_ID, PROVIDER_ID))
                .thenReturn(Optional.of(currentProvider(currentMapping)));

        service.updateProvider(
                TENANT_ID, PROVIDER_ID, "New Display", "new-client-id", "",
                "reason", "operator-1");

        assertThat(captureSavedProvider().getClaimsMappingView()).isEqualTo(currentMapping);
    }

}
