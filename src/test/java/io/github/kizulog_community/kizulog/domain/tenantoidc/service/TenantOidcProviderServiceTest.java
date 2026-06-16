package io.github.kizulog_community.kizulog.domain.tenantoidc.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import io.github.kizulog_community.kizulog.domain.port.CryptoPort;
import io.github.kizulog_community.kizulog.domain.tenant.model.Tenant;
import io.github.kizulog_community.kizulog.domain.tenant.port.TenantRepository;
import io.github.kizulog_community.kizulog.domain.tenantoidc.exception.TenantOidcProviderRegistrationError;
import io.github.kizulog_community.kizulog.domain.tenantoidc.exception.TenantOidcProviderRegistrationException;
import io.github.kizulog_community.kizulog.domain.tenantoidc.exception.TenantOidcProviderStatusChangeError;
import io.github.kizulog_community.kizulog.domain.tenantoidc.exception.TenantOidcProviderStatusChangeException;
import io.github.kizulog_community.kizulog.domain.tenantoidc.exception.TenantOidcProviderUpdateError;
import io.github.kizulog_community.kizulog.domain.tenantoidc.exception.TenantOidcProviderUpdateException;
import io.github.kizulog_community.kizulog.domain.tenantoidc.model.ClaimsMappingTarget;
import io.github.kizulog_community.kizulog.domain.tenantoidc.model.DecryptedTenantOidcProvider;
import io.github.kizulog_community.kizulog.domain.tenantoidc.model.EnabledTenantOidcProviderView;
import io.github.kizulog_community.kizulog.domain.tenantoidc.model.TenantOidcProvider;
import io.github.kizulog_community.kizulog.domain.tenantoidc.model.TenantOidcProviderDetailView;
import io.github.kizulog_community.kizulog.domain.tenantoidc.model.TenantOidcProviderListItemView;
import io.github.kizulog_community.kizulog.domain.tenantoidc.model.TenantOidcProviderStatus;
import io.github.kizulog_community.kizulog.domain.tenantoidc.model.TenantOidcProviderStatusValue;
import io.github.kizulog_community.kizulog.domain.tenantoidc.port.TenantOidcProviderRepository;
import io.github.kizulog_community.kizulog.domain.tenantoidc.port.TenantOidcProviderStatusRepository;

/**
 * TenantOidcProviderServiceの単体テスト
 *
 * @author Jun Kobayashi
 */
class TenantOidcProviderServiceTest {

    private static final OffsetDateTime BASE_TIME =
            OffsetDateTime.of(2026, 5, 1, 12, 0, 0, 0, ZoneOffset.UTC);

    private static final String TENANT_ID = "tenant-A";
    private static final String OTHER_TENANT_ID = "tenant-B";

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

    private Tenant tenantOf(String tenantId) {
        return new Tenant(
                tenantId, BASE_TIME, "Tenant " + tenantId, "slug-" + tenantId,
                BASE_TIME, "creator");
    }

    private void mockTenantExists(String tenantId) {
        when(tenantRepository.findLatestByTenantId(tenantId))
                .thenReturn(Optional.of(tenantOf(tenantId)));
    }

    private void mockTenantNotExists(String tenantId) {
        when(tenantRepository.findLatestByTenantId(tenantId))
                .thenReturn(Optional.empty());
    }

    private TenantOidcProvider providerOf(
            String tenantId, String providerId, OffsetDateTime version, String encryptedSecret) {
        return new TenantOidcProvider(
                tenantId, providerId, version,
                "Display " + providerId,
                "https://auth.example/realms/" + providerId,
                "aud-" + providerId,
                "client-" + providerId, encryptedSecret,
                ClaimsMappingTarget.defaultMapping(),
                version, "user:" + providerId);
    }

    private TenantOidcProviderStatus statusOf(
            String tenantId, String providerId, OffsetDateTime version,
            TenantOidcProviderStatusValue value) {
        return new TenantOidcProviderStatus(
                tenantId, providerId, version, value, "reason-" + providerId,
                version, "user:" + providerId);
    }

    @Test
    @DisplayName("listAllByTenantId: プロバイダーが存在する場合、ListItemViewのリストをdisplayName昇順で返す")
    void listAllByTenantId_returnsSortedList_whenProvidersExist() {
        // displayName が "Display zebra" → 後ろに来る、 "Display apple" → 先頭
        TenantOidcProvider provider1 = new TenantOidcProvider(
                TENANT_ID, "zebra", BASE_TIME, "Display zebra",
                "https://auth.example/zebra", "aud-zebra", "client", "secret",
                ClaimsMappingTarget.defaultMapping(),
                BASE_TIME, "creator");
        TenantOidcProvider provider2 = new TenantOidcProvider(
                TENANT_ID, "apple", BASE_TIME, "Display apple",
                "https://auth.example/apple", "aud-apple", "client", "secret",
                ClaimsMappingTarget.defaultMapping(),
                BASE_TIME, "creator");
        when(providerRepository.findAllLatestByTenantId(TENANT_ID))
                .thenReturn(List.of(provider1, provider2));
        when(statusRepository.findLatestByTenantIdAndProviderId(TENANT_ID, "zebra"))
                .thenReturn(Optional.of(statusOf(TENANT_ID, "zebra", BASE_TIME,
                        TenantOidcProviderStatusValue.ENABLED)));
        when(statusRepository.findLatestByTenantIdAndProviderId(TENANT_ID, "apple"))
                .thenReturn(Optional.of(statusOf(TENANT_ID, "apple", BASE_TIME,
                        TenantOidcProviderStatusValue.DISABLED)));

        List<TenantOidcProviderListItemView> result =
                service.listAllByTenantId(TENANT_ID);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getProviderId()).isEqualTo("apple");
        assertThat(result.get(0).getDisplayName()).isEqualTo("Display apple");
        assertThat(result.get(0).getCurrentStatus())
                .isEqualTo(TenantOidcProviderStatusValue.DISABLED);
        assertThat(result.get(1).getProviderId()).isEqualTo("zebra");
        assertThat(result.get(1).getDisplayName()).isEqualTo("Display zebra");
        assertThat(result.get(1).getCurrentStatus())
                .isEqualTo(TenantOidcProviderStatusValue.ENABLED);
    }

    @Test
    @DisplayName("listAllByTenantId: ステータスレコードが無いプロバイダーはDISABLED扱いとなる")
    void listAllByTenantId_treatsAsDisabled_whenStatusRecordMissing() {
        when(providerRepository.findAllLatestByTenantId(TENANT_ID))
                .thenReturn(List.of(providerOf(TENANT_ID, "orphan", BASE_TIME, "secret")));
        when(statusRepository.findLatestByTenantIdAndProviderId(TENANT_ID, "orphan"))
                .thenReturn(Optional.empty());

        List<TenantOidcProviderListItemView> result =
                service.listAllByTenantId(TENANT_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCurrentStatus())
                .isEqualTo(TenantOidcProviderStatusValue.DISABLED);
    }

    @Test
    @DisplayName("listAllByTenantId: プロバイダーが存在しない場合、空リストを返す")
    void listAllByTenantId_returnsEmptyList_whenNoProviders() {
        when(providerRepository.findAllLatestByTenantId(TENANT_ID))
                .thenReturn(List.of());

        List<TenantOidcProviderListItemView> result =
                service.listAllByTenantId(TENANT_ID);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findDetail: プロバイダーとステータスと履歴がある場合、DetailViewを返す")
    void findDetail_returnsDetailView_whenAllExist() {
        TenantOidcProvider provider = providerOf(TENANT_ID, "master", BASE_TIME, "encrypted");
        TenantOidcProviderStatus currentStatus =
                statusOf(TENANT_ID, "master", BASE_TIME, TenantOidcProviderStatusValue.ENABLED);
        TenantOidcProviderStatus prevStatus =
                statusOf(TENANT_ID, "master", BASE_TIME.minusDays(1),
                        TenantOidcProviderStatusValue.DISABLED);
        when(providerRepository.findLatestByTenantIdAndProviderId(TENANT_ID, "master"))
                .thenReturn(Optional.of(provider));
        when(statusRepository.findLatestByTenantIdAndProviderId(TENANT_ID, "master"))
                .thenReturn(Optional.of(currentStatus));
        when(statusRepository.findAllByTenantIdAndProviderIdOrderByVersionDesc(TENANT_ID, "master"))
                .thenReturn(List.of(currentStatus, prevStatus));

        Optional<TenantOidcProviderDetailView> result =
                service.findDetail(TENANT_ID, "master");

        assertThat(result).isPresent();
        TenantOidcProviderDetailView view = result.get();
        assertThat(view.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(view.getProviderId()).isEqualTo("master");
        assertThat(view.getDisplayName()).isEqualTo("Display master");
        assertThat(view.getIss()).isEqualTo("https://auth.example/realms/master");
        assertThat(view.getAud()).isEqualTo("aud-master");
        assertThat(view.getCurrentStatus()).isEqualTo(TenantOidcProviderStatusValue.ENABLED);
        assertThat(view.getStatusHistory()).hasSize(2);
    }

    @Test
    @DisplayName("findDetail: プロバイダーが存在しない場合、空Optionalを返す")
    void findDetail_returnsEmpty_whenProviderNotFound() {
        when(providerRepository.findLatestByTenantIdAndProviderId(TENANT_ID, "missing"))
                .thenReturn(Optional.empty());

        Optional<TenantOidcProviderDetailView> result =
                service.findDetail(TENANT_ID, "missing");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findDetail: ステータスレコードが無い場合、currentStatusをDISABLEDとして返す")
    void findDetail_currentStatusIsDisabled_whenStatusMissing() {
        when(providerRepository.findLatestByTenantIdAndProviderId(TENANT_ID, "master"))
                .thenReturn(Optional.of(providerOf(TENANT_ID, "master", BASE_TIME, "encrypted")));
        when(statusRepository.findLatestByTenantIdAndProviderId(TENANT_ID, "master"))
                .thenReturn(Optional.empty());
        when(statusRepository.findAllByTenantIdAndProviderIdOrderByVersionDesc(TENANT_ID, "master"))
                .thenReturn(List.of());

        Optional<TenantOidcProviderDetailView> result =
                service.findDetail(TENANT_ID, "master");

        assertThat(result).isPresent();
        assertThat(result.get().getCurrentStatus())
                .isEqualTo(TenantOidcProviderStatusValue.DISABLED);
        assertThat(result.get().getStatusHistory()).isEmpty();
    }

    @Test
    @DisplayName("findAllEnabledByTenantId: ENABLEDプロバイダーをdisplayName昇順で返す")
    void findAllEnabledByTenantId_returnsSortedEnabledList() {
        TenantOidcProviderStatus statusZebra =
                statusOf(TENANT_ID, "zebra", BASE_TIME, TenantOidcProviderStatusValue.ENABLED);
        TenantOidcProviderStatus statusApple =
                statusOf(TENANT_ID, "apple", BASE_TIME, TenantOidcProviderStatusValue.ENABLED);
        when(statusRepository.findAllLatestEnabledByTenantId(TENANT_ID))
                .thenReturn(List.of(statusZebra, statusApple));
        when(providerRepository.findLatestByTenantIdAndProviderId(TENANT_ID, "zebra"))
                .thenReturn(Optional.of(providerOf(TENANT_ID, "zebra", BASE_TIME, "secret")));
        when(providerRepository.findLatestByTenantIdAndProviderId(TENANT_ID, "apple"))
                .thenReturn(Optional.of(providerOf(TENANT_ID, "apple", BASE_TIME, "secret")));

        List<EnabledTenantOidcProviderView> result =
                service.findAllEnabledByTenantId(TENANT_ID);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getProviderId()).isEqualTo("apple");
        assertThat(result.get(1).getProviderId()).isEqualTo("zebra");
    }

    @Test
    @DisplayName("findAllEnabledByTenantId: ENABLEDが存在しない場合、空リストを返す")
    void findAllEnabledByTenantId_returnsEmptyList_whenNoEnabled() {
        when(statusRepository.findAllLatestEnabledByTenantId(TENANT_ID))
                .thenReturn(List.of());

        List<EnabledTenantOidcProviderView> result =
                service.findAllEnabledByTenantId(TENANT_ID);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findAllEnabledByTenantId: プロバイダー本体が存在しないステータスはスキップされる")
    void findAllEnabledByTenantId_skips_whenProviderBodyMissing() {
        TenantOidcProviderStatus orphanStatus =
                statusOf(TENANT_ID, "orphan", BASE_TIME, TenantOidcProviderStatusValue.ENABLED);
        when(statusRepository.findAllLatestEnabledByTenantId(TENANT_ID))
                .thenReturn(List.of(orphanStatus));
        when(providerRepository.findLatestByTenantIdAndProviderId(TENANT_ID, "orphan"))
                .thenReturn(Optional.empty());

        List<EnabledTenantOidcProviderView> result =
                service.findAllEnabledByTenantId(TENANT_ID);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findDecryptedByTenantIdAndProviderId: プロバイダーが存在する場合、復号済みDTOを返す")
    void findDecryptedByTenantIdAndProviderId_returnsDecrypted_whenExists() {
        when(providerRepository.findLatestByTenantIdAndProviderId(TENANT_ID, "master"))
                .thenReturn(Optional.of(providerOf(TENANT_ID, "master", BASE_TIME, "encrypted")));
        when(cryptoPort.decrypt("encrypted")).thenReturn("plain-secret");

        Optional<DecryptedTenantOidcProvider> result =
                service.findDecryptedByTenantIdAndProviderId(TENANT_ID, "master");

        assertThat(result).isPresent();
        assertThat(result.get().getTenantId()).isEqualTo(TENANT_ID);
        assertThat(result.get().getProviderId()).isEqualTo("master");
        assertThat(result.get().getClientSecret()).isEqualTo("plain-secret");
    }

    @Test
    @DisplayName("findDecryptedByTenantIdAndProviderId: プロバイダーが存在しない場合、空Optionalを返す")
    void findDecryptedByTenantIdAndProviderId_returnsEmpty_whenNotFound() {
        when(providerRepository.findLatestByTenantIdAndProviderId(TENANT_ID, "missing"))
                .thenReturn(Optional.empty());

        Optional<DecryptedTenantOidcProvider> result =
                service.findDecryptedByTenantIdAndProviderId(TENANT_ID, "missing");

        assertThat(result).isEmpty();
        verify(cryptoPort, never()).decrypt(anyString());
    }

    @Test
    @DisplayName("findEnabledForAuthentication: ENABLEDの場合、復号済みDTOを返す")
    void findEnabledForAuthentication_returnsDecrypted_whenEnabled() {
        when(statusRepository.findLatestByTenantIdAndProviderId(TENANT_ID, "master"))
                .thenReturn(Optional.of(statusOf(TENANT_ID, "master", BASE_TIME,
                        TenantOidcProviderStatusValue.ENABLED)));
        when(providerRepository.findLatestByTenantIdAndProviderId(TENANT_ID, "master"))
                .thenReturn(Optional.of(providerOf(TENANT_ID, "master", BASE_TIME, "encrypted")));
        when(cryptoPort.decrypt("encrypted")).thenReturn("plain-secret");

        Optional<DecryptedTenantOidcProvider> result =
                service.findEnabledForAuthentication(TENANT_ID, "master");

        assertThat(result).isPresent();
        assertThat(result.get().getProviderId()).isEqualTo("master");
        assertThat(result.get().getClientSecret()).isEqualTo("plain-secret");
    }

    @Test
    @DisplayName("findEnabledForAuthentication: DISABLEDの場合、空Optionalを返す")
    void findEnabledForAuthentication_returnsEmpty_whenDisabled() {
        when(statusRepository.findLatestByTenantIdAndProviderId(TENANT_ID, "master"))
                .thenReturn(Optional.of(statusOf(TENANT_ID, "master", BASE_TIME,
                        TenantOidcProviderStatusValue.DISABLED)));

        Optional<DecryptedTenantOidcProvider> result =
                service.findEnabledForAuthentication(TENANT_ID, "master");

        assertThat(result).isEmpty();
        verify(providerRepository, never())
                .findLatestByTenantIdAndProviderId(TENANT_ID, "master");
        verify(cryptoPort, never()).decrypt(anyString());
    }

    @Test
    @DisplayName("findEnabledForAuthentication: ステータス未登録の場合、空Optionalを返す")
    void findEnabledForAuthentication_returnsEmpty_whenNoStatus() {
        when(statusRepository.findLatestByTenantIdAndProviderId(TENANT_ID, "master"))
                .thenReturn(Optional.empty());

        Optional<DecryptedTenantOidcProvider> result =
                service.findEnabledForAuthentication(TENANT_ID, "master");

        assertThat(result).isEmpty();
        verify(providerRepository, never())
                .findLatestByTenantIdAndProviderId(TENANT_ID, "master");
    }

    @Test
    @DisplayName("findEnabledForAuthentication: ENABLEDだがプロバイダー本体がない場合、空Optionalを返す")
    void findEnabledForAuthentication_returnsEmpty_whenEnabledButNoProvider() {
        when(statusRepository.findLatestByTenantIdAndProviderId(TENANT_ID, "master"))
                .thenReturn(Optional.of(statusOf(TENANT_ID, "master", BASE_TIME,
                        TenantOidcProviderStatusValue.ENABLED)));
        when(providerRepository.findLatestByTenantIdAndProviderId(TENANT_ID, "master"))
                .thenReturn(Optional.empty());

        Optional<DecryptedTenantOidcProvider> result =
                service.findEnabledForAuthentication(TENANT_ID, "master");

        assertThat(result).isEmpty();
        verify(cryptoPort, never()).decrypt(anyString());
    }

    @Test
    @DisplayName("findDecryptedByIssAndAud: 該当プロバイダーが複数テナントに存在する場合、全てを返す")
    void findDecryptedByIssAndAud_returnsAllAcrossTenants() {
        TenantOidcProvider p1 = providerOf(TENANT_ID, "master", BASE_TIME, "enc1");
        TenantOidcProvider p2 = providerOf(OTHER_TENANT_ID, "master", BASE_TIME, "enc2");
        when(providerRepository.findAllLatestByIssAndAud("https://iss", "aud"))
                .thenReturn(List.of(p1, p2));
        when(cryptoPort.decrypt("enc1")).thenReturn("plain1");
        when(cryptoPort.decrypt("enc2")).thenReturn("plain2");

        List<DecryptedTenantOidcProvider> result =
                service.findDecryptedByIssAndAud("https://iss", "aud");

        assertThat(result).hasSize(2);
        assertThat(result).extracting(DecryptedTenantOidcProvider::getTenantId)
                .containsExactlyInAnyOrder(TENANT_ID, OTHER_TENANT_ID);
    }

    @Test
    @DisplayName("findDecryptedByIssAndAud: 該当が無い場合、空リストを返す")
    void findDecryptedByIssAndAud_returnsEmpty_whenNoMatch() {
        when(providerRepository.findAllLatestByIssAndAud(anyString(), anyString()))
                .thenReturn(List.of());

        List<DecryptedTenantOidcProvider> result =
                service.findDecryptedByIssAndAud("https://iss", "aud");

        assertThat(result).isEmpty();
        verify(cryptoPort, never()).decrypt(anyString());
    }

    @Test
    @DisplayName("findDecryptedByIssAndAud: 返却リストは不変")
    void findDecryptedByIssAndAud_returnsUnmodifiableList() {
        when(providerRepository.findAllLatestByIssAndAud("https://iss", "aud"))
                .thenReturn(List.of(providerOf(TENANT_ID, "master", BASE_TIME, "enc")));
        when(cryptoPort.decrypt("enc")).thenReturn("plain");

        List<DecryptedTenantOidcProvider> result =
                service.findDecryptedByIssAndAud("https://iss", "aud");

        assertThatThrownBy(() -> result.add(null))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("registerProvider: 正常系、プロバイダー本体とENABLEDステータスが保存される")
    void registerProvider_savesBothProviderAndStatus() {
        mockTenantExists(TENANT_ID);
        when(providerRepository.existsByTenantIdAndProviderId(TENANT_ID, "google"))
                .thenReturn(false);
        when(providerRepository.findAllLatestByIssAndAud("https://iss", "aud"))
                .thenReturn(List.of());
        when(cryptoPort.encrypt("plain-secret")).thenReturn("encrypted-secret");

        service.registerProvider(
                TENANT_ID, "google", "Google Workspace",
                "https://iss", "aud", "client-id", "plain-secret",
                "Initial setup", "operator-1");

        ArgumentCaptor<TenantOidcProvider> providerCaptor =
                ArgumentCaptor.forClass(TenantOidcProvider.class);
        verify(providerRepository).save(providerCaptor.capture());
        TenantOidcProvider saved = providerCaptor.getValue();
        assertThat(saved.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(saved.getProviderId()).isEqualTo("google");
        assertThat(saved.getDisplayName()).isEqualTo("Google Workspace");
        assertThat(saved.getIss()).isEqualTo("https://iss");
        assertThat(saved.getAud()).isEqualTo("aud");
        assertThat(saved.getClientId()).isEqualTo("client-id");
        assertThat(saved.getClientSecret()).isEqualTo("encrypted-secret");
        assertThat(saved.getCreatedBy()).contains("operator-1");

        ArgumentCaptor<TenantOidcProviderStatus> statusCaptor =
                ArgumentCaptor.forClass(TenantOidcProviderStatus.class);
        verify(statusRepository).save(statusCaptor.capture());
        TenantOidcProviderStatus savedStatus = statusCaptor.getValue();
        assertThat(savedStatus.getStatus()).isEqualTo(TenantOidcProviderStatusValue.ENABLED);
        assertThat(savedStatus.getReason()).isEqualTo("Initial setup");
    }

    @Test
    @DisplayName("registerProvider: テナント未存在の場合、TENANT_NOT_FOUNDをスロー")
    void registerProvider_throws_whenTenantNotFound() {
        mockTenantNotExists(TENANT_ID);

        assertThatThrownBy(() -> service.registerProvider(
                TENANT_ID, "google", "name", "https://iss", "aud", "cid", "secret",
                "reason", "op"))
                .isInstanceOf(TenantOidcProviderRegistrationException.class)
                .extracting("error")
                .isEqualTo(TenantOidcProviderRegistrationError.TENANT_NOT_FOUND);
        verify(providerRepository, never()).save(any());
    }

    @Test
    @DisplayName("registerProvider: providerIdが形式不正の場合、PROVIDER_ID_INVALIDをスロー")
    void registerProvider_throws_whenProviderIdInvalid() {
        mockTenantExists(TENANT_ID);

        assertThatThrownBy(() -> service.registerProvider(
                TENANT_ID, "INVALID_UPPERCASE",
                "name", "https://iss", "aud", "cid", "secret", "reason", "op"))
                .isInstanceOf(TenantOidcProviderRegistrationException.class)
                .extracting("error")
                .isEqualTo(TenantOidcProviderRegistrationError.PROVIDER_ID_INVALID);
    }

    @Test
    @DisplayName("registerProvider: providerIdが33文字以上の場合、PROVIDER_ID_INVALIDをスロー")
    void registerProvider_throws_whenProviderIdTooLong() {
        mockTenantExists(TENANT_ID);
        String longId = "a".repeat(33);

        assertThatThrownBy(() -> service.registerProvider(
                TENANT_ID, longId,
                "name", "https://iss", "aud", "cid", "secret", "reason", "op"))
                .isInstanceOf(TenantOidcProviderRegistrationException.class)
                .extracting("error")
                .isEqualTo(TenantOidcProviderRegistrationError.PROVIDER_ID_INVALID);
    }

    @Test
    @DisplayName("registerProvider: displayNameが空の場合、DISPLAY_NAME_INVALIDをスロー")
    void registerProvider_throws_whenDisplayNameEmpty() {
        mockTenantExists(TENANT_ID);

        assertThatThrownBy(() -> service.registerProvider(
                TENANT_ID, "google", "  ",
                "https://iss", "aud", "cid", "secret", "reason", "op"))
                .isInstanceOf(TenantOidcProviderRegistrationException.class)
                .extracting("error")
                .isEqualTo(TenantOidcProviderRegistrationError.DISPLAY_NAME_INVALID);
    }

    @Test
    @DisplayName("registerProvider: displayNameが101文字以上の場合、DISPLAY_NAME_INVALIDをスロー")
    void registerProvider_throws_whenDisplayNameTooLong() {
        mockTenantExists(TENANT_ID);

        assertThatThrownBy(() -> service.registerProvider(
                TENANT_ID, "google", "n".repeat(101),
                "https://iss", "aud", "cid", "secret", "reason", "op"))
                .isInstanceOf(TenantOidcProviderRegistrationException.class)
                .extracting("error")
                .isEqualTo(TenantOidcProviderRegistrationError.DISPLAY_NAME_INVALID);
    }

    @Test
    @DisplayName("registerProvider: issがhttp/https以外の場合、ISS_INVALIDをスロー")
    void registerProvider_throws_whenIssNotHttp() {
        mockTenantExists(TENANT_ID);

        assertThatThrownBy(() -> service.registerProvider(
                TENANT_ID, "google", "name",
                "ftp://example.com", "aud", "cid", "secret", "reason", "op"))
                .isInstanceOf(TenantOidcProviderRegistrationException.class)
                .extracting("error")
                .isEqualTo(TenantOidcProviderRegistrationError.ISS_INVALID);
    }

    @Test
    @DisplayName("registerProvider: issが空の場合、ISS_INVALIDをスロー")
    void registerProvider_throws_whenIssEmpty() {
        mockTenantExists(TENANT_ID);

        assertThatThrownBy(() -> service.registerProvider(
                TENANT_ID, "google", "name", "",
                "aud", "cid", "secret", "reason", "op"))
                .isInstanceOf(TenantOidcProviderRegistrationException.class)
                .extracting("error")
                .isEqualTo(TenantOidcProviderRegistrationError.ISS_INVALID);
    }

    @Test
    @DisplayName("registerProvider: audが空の場合、AUD_INVALIDをスロー")
    void registerProvider_throws_whenAudEmpty() {
        mockTenantExists(TENANT_ID);

        assertThatThrownBy(() -> service.registerProvider(
                TENANT_ID, "google", "name",
                "https://iss", "  ", "cid", "secret", "reason", "op"))
                .isInstanceOf(TenantOidcProviderRegistrationException.class)
                .extracting("error")
                .isEqualTo(TenantOidcProviderRegistrationError.AUD_INVALID);
    }

    @Test
    @DisplayName("registerProvider: clientIdが空の場合、CLIENT_ID_INVALIDをスロー")
    void registerProvider_throws_whenClientIdEmpty() {
        mockTenantExists(TENANT_ID);

        assertThatThrownBy(() -> service.registerProvider(
                TENANT_ID, "google", "name",
                "https://iss", "aud", "", "secret", "reason", "op"))
                .isInstanceOf(TenantOidcProviderRegistrationException.class)
                .extracting("error")
                .isEqualTo(TenantOidcProviderRegistrationError.CLIENT_ID_INVALID);
    }

    @Test
    @DisplayName("registerProvider: clientSecretがnullの場合、CLIENT_SECRET_INVALIDをスロー")
    void registerProvider_throws_whenClientSecretNull() {
        mockTenantExists(TENANT_ID);

        assertThatThrownBy(() -> service.registerProvider(
                TENANT_ID, "google", "name",
                "https://iss", "aud", "cid", null, "reason", "op"))
                .isInstanceOf(TenantOidcProviderRegistrationException.class)
                .extracting("error")
                .isEqualTo(TenantOidcProviderRegistrationError.CLIENT_SECRET_INVALID);
    }

    @Test
    @DisplayName("registerProvider: reasonが空の場合、REASON_INVALIDをスロー")
    void registerProvider_throws_whenReasonEmpty() {
        mockTenantExists(TENANT_ID);

        assertThatThrownBy(() -> service.registerProvider(
                TENANT_ID, "google", "name",
                "https://iss", "aud", "cid", "secret", "", "op"))
                .isInstanceOf(TenantOidcProviderRegistrationException.class)
                .extracting("error")
                .isEqualTo(TenantOidcProviderRegistrationError.REASON_INVALID);
    }

    @Test
    @DisplayName("registerProvider: reasonが1001文字以上の場合、REASON_INVALIDをスロー")
    void registerProvider_throws_whenReasonTooLong() {
        mockTenantExists(TENANT_ID);

        assertThatThrownBy(() -> service.registerProvider(
                TENANT_ID, "google", "name",
                "https://iss", "aud", "cid", "secret", "r".repeat(1001), "op"))
                .isInstanceOf(TenantOidcProviderRegistrationException.class)
                .extracting("error")
                .isEqualTo(TenantOidcProviderRegistrationError.REASON_INVALID);
    }

    @Test
    @DisplayName("registerProvider: providerIdが既存テナント内で重複している場合、PROVIDER_ID_DUPLICATEをスロー")
    void registerProvider_throws_whenProviderIdDuplicate() {
        mockTenantExists(TENANT_ID);
        when(providerRepository.existsByTenantIdAndProviderId(TENANT_ID, "google"))
                .thenReturn(true);

        assertThatThrownBy(() -> service.registerProvider(
                TENANT_ID, "google", "name",
                "https://iss", "aud", "cid", "secret", "reason", "op"))
                .isInstanceOf(TenantOidcProviderRegistrationException.class)
                .extracting("error")
                .isEqualTo(TenantOidcProviderRegistrationError.PROVIDER_ID_DUPLICATE);
    }

    @Test
    @DisplayName("registerProvider: 同一テナント内で(iss,aud)が重複している場合、ISS_AUD_DUPLICATEをスロー")
    void registerProvider_throws_whenIssAudDuplicateInSameTenant() {
        mockTenantExists(TENANT_ID);
        when(providerRepository.existsByTenantIdAndProviderId(TENANT_ID, "another"))
                .thenReturn(false);
        // 既に同テナントに同じ (iss, aud) のプロバイダーが存在
        TenantOidcProvider existing = new TenantOidcProvider(
                TENANT_ID, "existing", BASE_TIME, "Existing",
                "https://iss", "aud", "client", "secret",
                ClaimsMappingTarget.defaultMapping(),
                BASE_TIME, "creator");
        when(providerRepository.findAllLatestByIssAndAud("https://iss", "aud"))
                .thenReturn(List.of(existing));

        assertThatThrownBy(() -> service.registerProvider(
                TENANT_ID, "another", "name",
                "https://iss", "aud", "cid", "secret", "reason", "op"))
                .isInstanceOf(TenantOidcProviderRegistrationException.class)
                .extracting("error")
                .isEqualTo(TenantOidcProviderRegistrationError.ISS_AUD_DUPLICATE);
    }

    @Test
    @DisplayName("registerProvider: 異なるテナントに同じ(iss,aud)が存在しても登録可能")
    void registerProvider_succeeds_whenIssAudExistsInOtherTenant() {
        mockTenantExists(TENANT_ID);
        when(providerRepository.existsByTenantIdAndProviderId(TENANT_ID, "google"))
                .thenReturn(false);
        // 他テナント側に同じ (iss, aud) のプロバイダーが存在
        TenantOidcProvider otherTenantProvider = new TenantOidcProvider(
                OTHER_TENANT_ID, "google", BASE_TIME, "Other Tenant Google",
                "https://iss", "aud", "client", "secret",
                ClaimsMappingTarget.defaultMapping(),
                BASE_TIME, "creator");
        when(providerRepository.findAllLatestByIssAndAud("https://iss", "aud"))
                .thenReturn(List.of(otherTenantProvider));
        when(cryptoPort.encrypt("secret")).thenReturn("encrypted");

        service.registerProvider(
                TENANT_ID, "google", "name",
                "https://iss", "aud", "cid", "secret", "reason", "op");

        verify(providerRepository, times(1)).save(any());
        verify(statusRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("updateProvider: clientSecretを更新する場合、暗号化された新値が保存される")
    void updateProvider_updatesClientSecret_whenProvided() {
        mockTenantExists(TENANT_ID);
        TenantOidcProvider current = providerOf(TENANT_ID, "google", BASE_TIME, "old-encrypted");
        when(providerRepository.findLatestByTenantIdAndProviderId(TENANT_ID, "google"))
                .thenReturn(Optional.of(current));
        when(cryptoPort.encrypt("new-plain")).thenReturn("new-encrypted");

        service.updateProvider(
                TENANT_ID, "google", "New Display", "new-client-id",
                "new-plain", "Updating secret", "op-1");

        ArgumentCaptor<TenantOidcProvider> captor =
                ArgumentCaptor.forClass(TenantOidcProvider.class);
        verify(providerRepository).save(captor.capture());
        TenantOidcProvider saved = captor.getValue();
        assertThat(saved.getDisplayName()).isEqualTo("New Display");
        assertThat(saved.getClientId()).isEqualTo("new-client-id");
        assertThat(saved.getClientSecret()).isEqualTo("new-encrypted");
        // iss/aud は不変
        assertThat(saved.getIss()).isEqualTo(current.getIss());
        assertThat(saved.getAud()).isEqualTo(current.getAud());
    }

    @Test
    @DisplayName("updateProvider: clientSecretが空文字の場合、現在の暗号化値を維持する")
    void updateProvider_preservesClientSecret_whenEmpty() {
        mockTenantExists(TENANT_ID);
        TenantOidcProvider current = providerOf(TENANT_ID, "google", BASE_TIME, "old-encrypted");
        when(providerRepository.findLatestByTenantIdAndProviderId(TENANT_ID, "google"))
                .thenReturn(Optional.of(current));

        service.updateProvider(
                TENANT_ID, "google", "New Display", "new-client-id",
                "", "Updating without secret", "op-1");

        ArgumentCaptor<TenantOidcProvider> captor =
                ArgumentCaptor.forClass(TenantOidcProvider.class);
        verify(providerRepository).save(captor.capture());
        assertThat(captor.getValue().getClientSecret()).isEqualTo("old-encrypted");
        verify(cryptoPort, never()).encrypt(anyString());
    }

    @Test
    @DisplayName("updateProvider: clientSecretがnullの場合も現在の暗号化値を維持する")
    void updateProvider_preservesClientSecret_whenNull() {
        mockTenantExists(TENANT_ID);
        TenantOidcProvider current = providerOf(TENANT_ID, "google", BASE_TIME, "old-encrypted");
        when(providerRepository.findLatestByTenantIdAndProviderId(TENANT_ID, "google"))
                .thenReturn(Optional.of(current));

        service.updateProvider(
                TENANT_ID, "google", "New Display", "new-client-id",
                null, "Updating without secret", "op-1");

        ArgumentCaptor<TenantOidcProvider> captor =
                ArgumentCaptor.forClass(TenantOidcProvider.class);
        verify(providerRepository).save(captor.capture());
        assertThat(captor.getValue().getClientSecret()).isEqualTo("old-encrypted");
        verify(cryptoPort, never()).encrypt(anyString());
    }

    @Test
    @DisplayName("updateProvider: テナント未存在の場合、TENANT_NOT_FOUNDをスロー")
    void updateProvider_throws_whenTenantNotFound() {
        mockTenantNotExists(TENANT_ID);

        assertThatThrownBy(() -> service.updateProvider(
                TENANT_ID, "google", "name", "cid", "secret", "reason", "op"))
                .isInstanceOf(TenantOidcProviderUpdateException.class)
                .extracting("error")
                .isEqualTo(TenantOidcProviderUpdateError.TENANT_NOT_FOUND);
    }

    @Test
    @DisplayName("updateProvider: プロバイダー未存在の場合、PROVIDER_NOT_FOUNDをスロー")
    void updateProvider_throws_whenProviderNotFound() {
        mockTenantExists(TENANT_ID);
        when(providerRepository.findLatestByTenantIdAndProviderId(TENANT_ID, "missing"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateProvider(
                TENANT_ID, "missing", "name", "cid", "secret", "reason", "op"))
                .isInstanceOf(TenantOidcProviderUpdateException.class)
                .extracting("error")
                .isEqualTo(TenantOidcProviderUpdateError.PROVIDER_NOT_FOUND);
    }

    @Test
    @DisplayName("updateProvider: displayNameが空の場合、DISPLAY_NAME_INVALIDをスロー")
    void updateProvider_throws_whenDisplayNameEmpty() {
        mockTenantExists(TENANT_ID);
        when(providerRepository.findLatestByTenantIdAndProviderId(TENANT_ID, "google"))
                .thenReturn(Optional.of(providerOf(TENANT_ID, "google", BASE_TIME, "enc")));

        assertThatThrownBy(() -> service.updateProvider(
                TENANT_ID, "google", "  ", "cid", "secret", "reason", "op"))
                .isInstanceOf(TenantOidcProviderUpdateException.class)
                .extracting("error")
                .isEqualTo(TenantOidcProviderUpdateError.DISPLAY_NAME_INVALID);
    }

    @Test
    @DisplayName("updateProvider: clientIdが空の場合、CLIENT_ID_INVALIDをスロー")
    void updateProvider_throws_whenClientIdEmpty() {
        mockTenantExists(TENANT_ID);
        when(providerRepository.findLatestByTenantIdAndProviderId(TENANT_ID, "google"))
                .thenReturn(Optional.of(providerOf(TENANT_ID, "google", BASE_TIME, "enc")));

        assertThatThrownBy(() -> service.updateProvider(
                TENANT_ID, "google", "name", "", "secret", "reason", "op"))
                .isInstanceOf(TenantOidcProviderUpdateException.class)
                .extracting("error")
                .isEqualTo(TenantOidcProviderUpdateError.CLIENT_ID_INVALID);
    }

    @Test
    @DisplayName("updateProvider: reasonが空の場合、REASON_INVALIDをスロー")
    void updateProvider_throws_whenReasonEmpty() {
        mockTenantExists(TENANT_ID);
        when(providerRepository.findLatestByTenantIdAndProviderId(TENANT_ID, "google"))
                .thenReturn(Optional.of(providerOf(TENANT_ID, "google", BASE_TIME, "enc")));

        assertThatThrownBy(() -> service.updateProvider(
                TENANT_ID, "google", "name", "cid", "secret", "", "op"))
                .isInstanceOf(TenantOidcProviderUpdateException.class)
                .extracting("error")
                .isEqualTo(TenantOidcProviderUpdateError.REASON_INVALID);
    }

    @Test
    @DisplayName("changeStatus: ENABLED→DISABLEDの正常系、新versionステータスが保存される")
    void changeStatus_savesNewStatus_whenEnabledToDisabled() {
        mockTenantExists(TENANT_ID);
        when(providerRepository.findLatestByTenantIdAndProviderId(TENANT_ID, "google"))
                .thenReturn(Optional.of(providerOf(TENANT_ID, "google", BASE_TIME, "enc")));
        when(statusRepository.findLatestByTenantIdAndProviderId(TENANT_ID, "google"))
                .thenReturn(Optional.of(statusOf(TENANT_ID, "google", BASE_TIME,
                        TenantOidcProviderStatusValue.ENABLED)));

        service.changeStatus(
                TENANT_ID, "google", TenantOidcProviderStatusValue.DISABLED,
                "Disabling", "op-1");

        ArgumentCaptor<TenantOidcProviderStatus> captor =
                ArgumentCaptor.forClass(TenantOidcProviderStatus.class);
        verify(statusRepository).save(captor.capture());
        TenantOidcProviderStatus saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(TenantOidcProviderStatusValue.DISABLED);
        assertThat(saved.getReason()).isEqualTo("Disabling");
        assertThat(saved.getCreatedBy()).contains("op-1");
    }

    @Test
    @DisplayName("changeStatus: DISABLED→ENABLEDの正常系")
    void changeStatus_savesNewStatus_whenDisabledToEnabled() {
        mockTenantExists(TENANT_ID);
        when(providerRepository.findLatestByTenantIdAndProviderId(TENANT_ID, "google"))
                .thenReturn(Optional.of(providerOf(TENANT_ID, "google", BASE_TIME, "enc")));
        when(statusRepository.findLatestByTenantIdAndProviderId(TENANT_ID, "google"))
                .thenReturn(Optional.of(statusOf(TENANT_ID, "google", BASE_TIME,
                        TenantOidcProviderStatusValue.DISABLED)));

        service.changeStatus(
                TENANT_ID, "google", TenantOidcProviderStatusValue.ENABLED,
                "Enabling", "op-1");

        ArgumentCaptor<TenantOidcProviderStatus> captor =
                ArgumentCaptor.forClass(TenantOidcProviderStatus.class);
        verify(statusRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus())
                .isEqualTo(TenantOidcProviderStatusValue.ENABLED);
    }

    @Test
    @DisplayName("changeStatus: テナント未存在の場合、TENANT_NOT_FOUNDをスロー")
    void changeStatus_throws_whenTenantNotFound() {
        mockTenantNotExists(TENANT_ID);

        assertThatThrownBy(() -> service.changeStatus(
                TENANT_ID, "google", TenantOidcProviderStatusValue.DISABLED,
                "reason", "op"))
                .isInstanceOf(TenantOidcProviderStatusChangeException.class)
                .extracting("error")
                .isEqualTo(TenantOidcProviderStatusChangeError.TENANT_NOT_FOUND);
    }

    @Test
    @DisplayName("changeStatus: プロバイダー未存在の場合、PROVIDER_NOT_FOUNDをスロー")
    void changeStatus_throws_whenProviderNotFound() {
        mockTenantExists(TENANT_ID);
        when(providerRepository.findLatestByTenantIdAndProviderId(TENANT_ID, "missing"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.changeStatus(
                TENANT_ID, "missing", TenantOidcProviderStatusValue.DISABLED,
                "reason", "op"))
                .isInstanceOf(TenantOidcProviderStatusChangeException.class)
                .extracting("error")
                .isEqualTo(TenantOidcProviderStatusChangeError.PROVIDER_NOT_FOUND);
    }

    @Test
    @DisplayName("changeStatus: 同一ステータスへの遷移を試みた場合、ALREADY_IN_TARGET_STATUSをスロー")
    void changeStatus_throws_whenAlreadyInTargetStatus() {
        mockTenantExists(TENANT_ID);
        when(providerRepository.findLatestByTenantIdAndProviderId(TENANT_ID, "google"))
                .thenReturn(Optional.of(providerOf(TENANT_ID, "google", BASE_TIME, "enc")));
        when(statusRepository.findLatestByTenantIdAndProviderId(TENANT_ID, "google"))
                .thenReturn(Optional.of(statusOf(TENANT_ID, "google", BASE_TIME,
                        TenantOidcProviderStatusValue.ENABLED)));

        assertThatThrownBy(() -> service.changeStatus(
                TENANT_ID, "google", TenantOidcProviderStatusValue.ENABLED,
                "reason", "op"))
                .isInstanceOf(TenantOidcProviderStatusChangeException.class)
                .extracting("error")
                .isEqualTo(TenantOidcProviderStatusChangeError.ALREADY_IN_TARGET_STATUS);
    }

    @Test
    @DisplayName("changeStatus: reasonが空の場合、REASON_INVALIDをスロー")
    void changeStatus_throws_whenReasonEmpty() {
        mockTenantExists(TENANT_ID);
        when(providerRepository.findLatestByTenantIdAndProviderId(TENANT_ID, "google"))
                .thenReturn(Optional.of(providerOf(TENANT_ID, "google", BASE_TIME, "enc")));

        assertThatThrownBy(() -> service.changeStatus(
                TENANT_ID, "google", TenantOidcProviderStatusValue.DISABLED,
                "", "op"))
                .isInstanceOf(TenantOidcProviderStatusChangeException.class)
                .extracting("error")
                .isEqualTo(TenantOidcProviderStatusChangeError.REASON_INVALID);
    }

}