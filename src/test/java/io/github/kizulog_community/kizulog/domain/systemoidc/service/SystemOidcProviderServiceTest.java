package io.github.kizulog_community.kizulog.domain.systemoidc.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
import io.github.kizulog_community.kizulog.domain.systemoidc.exception.OidcProviderError;
import io.github.kizulog_community.kizulog.domain.systemoidc.exception.OidcProviderException;
import io.github.kizulog_community.kizulog.domain.systemoidc.model.DecryptedOidcProvider;
import io.github.kizulog_community.kizulog.domain.systemoidc.model.OidcProviderStatusValue;
import io.github.kizulog_community.kizulog.domain.systemoidc.model.SystemOidcProvider;
import io.github.kizulog_community.kizulog.domain.systemoidc.model.SystemOidcProviderStatus;
import io.github.kizulog_community.kizulog.domain.systemoidc.port.SystemOidcProviderRepository;
import io.github.kizulog_community.kizulog.domain.systemoidc.port.SystemOidcProviderStatusRepository;
import io.github.kizulog_community.kizulog.domain.systemoidc.service.SystemOidcProviderService.ProviderWithStatus;

/**
 * SystemOidcProviderServiceの単体テスト
 *
 * @author Jun Kobayashi
 */
class SystemOidcProviderServiceTest {

    private static final OffsetDateTime BASE_TIME =
            OffsetDateTime.of(2026, 5, 1, 12, 0, 0, 0, ZoneOffset.UTC);

    private SystemOidcProviderRepository providerRepository;
    private SystemOidcProviderStatusRepository statusRepository;
    private CryptoPort cryptoPort;
    private SystemOidcProviderService service;

    @BeforeEach
    void setUp() {
        providerRepository = mock(SystemOidcProviderRepository.class);
        statusRepository = mock(SystemOidcProviderStatusRepository.class);
        cryptoPort = mock(CryptoPort.class);
        service = new SystemOidcProviderService(
                providerRepository, statusRepository, cryptoPort);
    }

    private SystemOidcProvider providerOf(
            String id, OffsetDateTime version, String secret) {
        return new SystemOidcProvider(
                id, version, "Display " + id,
                "https://auth.example/realms/" + id,
                "client-" + id, secret, version, "user:" + id);
    }

    private SystemOidcProviderStatus statusOf(
            String id, OffsetDateTime version, OidcProviderStatusValue value) {
        return new SystemOidcProviderStatus(
                id, version, value, "reason-" + id, version, "user:" + id);
    }

    @Test
    @DisplayName("findEnabledForAuthentication: ステータスENABLEDかつプロバイダーが存在する場合、復号済みオブジェクトを返す")
    void findEnabledForAuthentication_returnsDecryptedProvider_whenEnabled() {
        when(statusRepository.findLatestByProviderId("master"))
                .thenReturn(Optional.of(
                        statusOf("master", BASE_TIME, OidcProviderStatusValue.ENABLED)));
        when(providerRepository.findLatestByProviderId("master"))
                .thenReturn(Optional.of(providerOf("master", BASE_TIME, "encrypted-secret")));
        when(cryptoPort.decrypt("encrypted-secret")).thenReturn("plain-secret");

        Optional<DecryptedOidcProvider> result =
                service.findEnabledForAuthentication("master");

        assertThat(result).isPresent();
        assertThat(result.get().getProviderId()).isEqualTo("master");
        assertThat(result.get().getClientSecret()).isEqualTo("plain-secret");
        verify(cryptoPort).decrypt("encrypted-secret");
    }

    @Test
    @DisplayName("findEnabledForAuthentication: ステータスがDISABLEDの場合、空のOptionalを返す（プロバイダー検索しない）")
    void findEnabledForAuthentication_returnsEmpty_whenDisabled() {
        when(statusRepository.findLatestByProviderId("master"))
                .thenReturn(Optional.of(
                        statusOf("master", BASE_TIME, OidcProviderStatusValue.DISABLED)));

        Optional<DecryptedOidcProvider> result =
                service.findEnabledForAuthentication("master");

        assertThat(result).isEmpty();
        verify(providerRepository, never()).findLatestByProviderId(any());
        verify(cryptoPort, never()).decrypt(any());
    }

    @Test
    @DisplayName("findEnabledForAuthentication: ステータスが存在しない場合、空のOptionalを返す")
    void findEnabledForAuthentication_returnsEmpty_whenStatusNotFound() {
        when(statusRepository.findLatestByProviderId("master"))
                .thenReturn(Optional.empty());

        Optional<DecryptedOidcProvider> result =
                service.findEnabledForAuthentication("master");

        assertThat(result).isEmpty();
        verify(providerRepository, never()).findLatestByProviderId(any());
    }

    @Test
    @DisplayName("findEnabledForAuthentication: ステータスENABLEDだがプロバイダーが存在しない場合、空のOptionalを返す")
    void findEnabledForAuthentication_returnsEmpty_whenProviderNotFound() {
        when(statusRepository.findLatestByProviderId("master"))
                .thenReturn(Optional.of(
                        statusOf("master", BASE_TIME, OidcProviderStatusValue.ENABLED)));
        when(providerRepository.findLatestByProviderId("master"))
                .thenReturn(Optional.empty());

        Optional<DecryptedOidcProvider> result =
                service.findEnabledForAuthentication("master");

        assertThat(result).isEmpty();
        verify(cryptoPort, never()).decrypt(any());
    }

    @Test
    @DisplayName("validateNewProviderId: 有効な形式かつ未使用なら例外なし")
    void validateNewProviderId_doesNotThrow_whenValidAndNew() {
        when(providerRepository.existsByProviderId("master")).thenReturn(false);

        service.validateNewProviderId("master");
        // 例外なし
    }

    @Test
    @DisplayName("validateNewProviderId: nullを渡すとPROVIDER_ID_INVALID_FORMAT例外")
    void validateNewProviderId_throwsInvalidFormat_whenNull() {
        assertThatThrownBy(() -> service.validateNewProviderId(null))
                .isInstanceOf(OidcProviderException.class)
                .extracting("error")
                .isEqualTo(OidcProviderError.PROVIDER_ID_INVALID_FORMAT);
    }

    @Test
    @DisplayName("validateNewProviderId: 空文字を渡すとPROVIDER_ID_INVALID_FORMAT例外")
    void validateNewProviderId_throwsInvalidFormat_whenEmpty() {
        assertThatThrownBy(() -> service.validateNewProviderId(""))
                .isInstanceOf(OidcProviderException.class)
                .extracting("error")
                .isEqualTo(OidcProviderError.PROVIDER_ID_INVALID_FORMAT);
    }

    @Test
    @DisplayName("validateNewProviderId: 大文字を含むとPROVIDER_ID_INVALID_FORMAT例外")
    void validateNewProviderId_throwsInvalidFormat_whenUppercase() {
        assertThatThrownBy(() -> service.validateNewProviderId("Master"))
                .isInstanceOf(OidcProviderException.class)
                .extracting("error")
                .isEqualTo(OidcProviderError.PROVIDER_ID_INVALID_FORMAT);
    }

    @Test
    @DisplayName("validateNewProviderId: 33文字以上を渡すとPROVIDER_ID_INVALID_FORMAT例外")
    void validateNewProviderId_throwsInvalidFormat_whenTooLong() {
        String tooLong = "a".repeat(33);
        assertThatThrownBy(() -> service.validateNewProviderId(tooLong))
                .isInstanceOf(OidcProviderException.class)
                .extracting("error")
                .isEqualTo(OidcProviderError.PROVIDER_ID_INVALID_FORMAT);
    }

    @Test
    @DisplayName("validateNewProviderId: 32文字ちょうどは許容される")
    void validateNewProviderId_passes_whenExactly32Chars() {
        String thirtyTwo = "a".repeat(32);
        when(providerRepository.existsByProviderId(thirtyTwo)).thenReturn(false);

        service.validateNewProviderId(thirtyTwo);
    }

    @Test
    @DisplayName("validateNewProviderId: 英数字とハイフンのみ許容される")
    void validateNewProviderId_passes_whenAllowedChars() {
        when(providerRepository.existsByProviderId("a-b-c-1-2")).thenReturn(false);

        service.validateNewProviderId("a-b-c-1-2");
    }

    @Test
    @DisplayName("validateNewProviderId: 既存のproviderIdを渡すとPROVIDER_ID_DUPLICATE例外")
    void validateNewProviderId_throwsDuplicate_whenExistingId() {
        when(providerRepository.existsByProviderId("master")).thenReturn(true);

        assertThatThrownBy(() -> service.validateNewProviderId("master"))
                .isInstanceOf(OidcProviderException.class)
                .extracting("error")
                .isEqualTo(OidcProviderError.PROVIDER_ID_DUPLICATE);
    }

    @Test
    @DisplayName("register: プロバイダー本体とステータスが両方保存される")
    void register_savesProviderAndStatus() {
        when(cryptoPort.encrypt("plain-secret")).thenReturn("encrypted-secret");

        service.register("master", "Master", "https://auth.example/realms/master",
                "client-1", "plain-secret",
                OidcProviderStatusValue.ENABLED, BASE_TIME, "system:setup");

        verify(providerRepository).save(any(SystemOidcProvider.class));
        verify(statusRepository).save(any(SystemOidcProviderStatus.class));
    }

    @Test
    @DisplayName("register: client_secretが暗号化されて保存される")
    void register_encryptsClientSecret() {
        when(cryptoPort.encrypt("plain-secret")).thenReturn("encrypted-secret");

        service.register("master", "Master", "https://auth.example/realms/master",
                "client-1", "plain-secret",
                OidcProviderStatusValue.ENABLED, BASE_TIME, "system:setup");

        verify(cryptoPort).encrypt("plain-secret");

        ArgumentCaptor<SystemOidcProvider> captor =
                ArgumentCaptor.forClass(SystemOidcProvider.class);
        verify(providerRepository).save(captor.capture());
        assertThat(captor.getValue().getClientSecret()).isEqualTo("encrypted-secret");
    }

    @Test
    @DisplayName("register: 引数の値が正しくプロバイダーオブジェクトに反映される")
    void register_persistsCorrectFields() {
        when(cryptoPort.encrypt("plain-secret")).thenReturn("encrypted-secret");

        service.register("master", "Master Display",
                "https://auth.example/realms/master",
                "client-1", "plain-secret",
                OidcProviderStatusValue.ENABLED, BASE_TIME, "system:setup");

        ArgumentCaptor<SystemOidcProvider> pCap =
                ArgumentCaptor.forClass(SystemOidcProvider.class);
        verify(providerRepository).save(pCap.capture());
        SystemOidcProvider provider = pCap.getValue();
        assertThat(provider.getProviderId()).isEqualTo("master");
        assertThat(provider.getDisplayName()).isEqualTo("Master Display");
        assertThat(provider.getUri()).isEqualTo("https://auth.example/realms/master");
        assertThat(provider.getClientId()).isEqualTo("client-1");
        assertThat(provider.getVersion()).isEqualTo(BASE_TIME);
        assertThat(provider.getCreatedBy()).isEqualTo("system:setup");

        ArgumentCaptor<SystemOidcProviderStatus> sCap =
                ArgumentCaptor.forClass(SystemOidcProviderStatus.class);
        verify(statusRepository).save(sCap.capture());
        SystemOidcProviderStatus status = sCap.getValue();
        assertThat(status.getProviderId()).isEqualTo("master");
        assertThat(status.getStatus()).isEqualTo(OidcProviderStatusValue.ENABLED);
        assertThat(status.getReason()).isNull();
        assertThat(status.getVersion()).isEqualTo(BASE_TIME);
        assertThat(status.getCreatedBy()).isEqualTo("system:setup");
    }

    @Test
    @DisplayName("registerWithValidation: 正常系ではバリデーション通過後にプロバイダーが保存される")
    void registerWithValidation_savesProvider_whenValid() {
        when(providerRepository.existsByProviderId("google")).thenReturn(false);
        when(cryptoPort.encrypt("plain-secret")).thenReturn("encrypted-secret");

        service.registerWithValidation("google", "Google",
                "https://accounts.google.com",
                "client-g", "plain-secret", "user:admin");

        verify(providerRepository).save(any(SystemOidcProvider.class));
        verify(statusRepository).save(any(SystemOidcProviderStatus.class));
    }

    @Test
    @DisplayName("registerWithValidation: 不正な形式のproviderIdはPROVIDER_ID_INVALID_FORMAT例外、保存しない")
    void registerWithValidation_throwsAndDoesNotSave_whenInvalidFormat() {
        assertThatThrownBy(() ->
                service.registerWithValidation("INVALID", "Invalid",
                        "https://example.com",
                        "client", "secret", "user"))
                .isInstanceOf(OidcProviderException.class)
                .extracting("error")
                .isEqualTo(OidcProviderError.PROVIDER_ID_INVALID_FORMAT);

        verify(providerRepository, never()).save(any());
        verify(statusRepository, never()).save(any());
    }

    @Test
    @DisplayName("registerWithValidation: 既存のproviderIdはPROVIDER_ID_DUPLICATE例外、保存しない")
    void registerWithValidation_throwsAndDoesNotSave_whenDuplicateId() {
        when(providerRepository.existsByProviderId("master")).thenReturn(true);

        assertThatThrownBy(() ->
                service.registerWithValidation("master", "Master",
                        "https://example.com",
                        "client", "secret", "user"))
                .isInstanceOf(OidcProviderException.class)
                .extracting("error")
                .isEqualTo(OidcProviderError.PROVIDER_ID_DUPLICATE);

        verify(providerRepository, never()).save(any());
        verify(statusRepository, never()).save(any());
    }

    @Test
    @DisplayName("registerWithValidation: 正常系では各リポジトリのsaveが1回ずつ呼ばれる")
    void registerWithValidation_savesOnce() {
        when(providerRepository.existsByProviderId("test")).thenReturn(false);
        when(cryptoPort.encrypt(any())).thenReturn("encrypted");

        service.registerWithValidation("test", "Test", "https://example.com",
                "client", "secret", "user");

        verify(providerRepository, times(1)).save(any());
        verify(statusRepository, times(1)).save(any());
        verify(cryptoPort, times(1)).encrypt("secret");
    }

    @Test
    @DisplayName("registerWithValidation: providerとstatusのversionとproviderIdが同じ値で記録される")
    void registerWithValidation_providerVersionMatchesStatusVersion() {
        when(providerRepository.existsByProviderId("test")).thenReturn(false);
        when(cryptoPort.encrypt(any())).thenReturn("encrypted");

        service.registerWithValidation("test", "Test", "https://example.com",
                "client", "secret", "user");

        ArgumentCaptor<SystemOidcProvider> pCap =
                ArgumentCaptor.forClass(SystemOidcProvider.class);
        ArgumentCaptor<SystemOidcProviderStatus> sCap =
                ArgumentCaptor.forClass(SystemOidcProviderStatus.class);
        verify(providerRepository).save(pCap.capture());
        verify(statusRepository).save(sCap.capture());

        assertThat(sCap.getValue().getVersion())
                .isEqualTo(pCap.getValue().getVersion());
        assertThat(sCap.getValue().getProviderId())
                .isEqualTo(pCap.getValue().getProviderId());
    }

    @Test
    @DisplayName("updateMutableFields: 正常系では編集項目が更新され、新versionで保存される")
    void updateMutableFields_updatesWithNewVersion() {
        SystemOidcProvider current = providerOf("master", BASE_TIME, "old-encrypted");
        when(providerRepository.findLatestByProviderId("master"))
                .thenReturn(Optional.of(current));
        when(cryptoPort.encrypt("new-plain-secret")).thenReturn("new-encrypted");

        service.updateMutableFields("master", "New Display",
                "new-client-id", "new-plain-secret", "user:admin");

        ArgumentCaptor<SystemOidcProvider> captor =
                ArgumentCaptor.forClass(SystemOidcProvider.class);
        verify(providerRepository).save(captor.capture());
        SystemOidcProvider saved = captor.getValue();
        assertThat(saved.getProviderId()).isEqualTo("master");
        assertThat(saved.getDisplayName()).isEqualTo("New Display");
        assertThat(saved.getClientId()).isEqualTo("new-client-id");
        assertThat(saved.getClientSecret()).isEqualTo("new-encrypted");
        assertThat(saved.getCreatedBy()).isEqualTo("user:admin");
        assertThat(saved.getVersion()).isAfter(BASE_TIME);
    }

    @Test
    @DisplayName("updateMutableFields: URIは編集できない（変更されない）")
    void updateMutableFields_doesNotChangeUri() {
        String originalUri = "https://auth.example/realms/master";
        SystemOidcProvider current = new SystemOidcProvider(
                "master", BASE_TIME, "Old", originalUri,
                "old-client", "old-encrypted", BASE_TIME, "user:old");
        when(providerRepository.findLatestByProviderId("master"))
                .thenReturn(Optional.of(current));
        when(cryptoPort.encrypt("new-secret")).thenReturn("new-encrypted");

        service.updateMutableFields("master", "New", "new-client",
                "new-secret", "user:admin");

        ArgumentCaptor<SystemOidcProvider> captor =
                ArgumentCaptor.forClass(SystemOidcProvider.class);
        verify(providerRepository).save(captor.capture());
        assertThat(captor.getValue().getUri()).isEqualTo(originalUri);
    }

    @Test
    @DisplayName("updateMutableFields: client_secretがnullの場合、既存の暗号化済みsecretを流用")
    void updateMutableFields_usesCurrentSecret_whenNullSecret() {
        SystemOidcProvider current = providerOf("master", BASE_TIME, "old-encrypted");
        when(providerRepository.findLatestByProviderId("master"))
                .thenReturn(Optional.of(current));

        service.updateMutableFields("master", "New", "new-client", null, "user:admin");

        ArgumentCaptor<SystemOidcProvider> captor =
                ArgumentCaptor.forClass(SystemOidcProvider.class);
        verify(providerRepository).save(captor.capture());
        assertThat(captor.getValue().getClientSecret()).isEqualTo("old-encrypted");
        verify(cryptoPort, never()).encrypt(any());
    }

    @Test
    @DisplayName("updateMutableFields: client_secretが空文字の場合、既存の暗号化済みsecretを流用")
    void updateMutableFields_usesCurrentSecret_whenEmptySecret() {
        SystemOidcProvider current = providerOf("master", BASE_TIME, "old-encrypted");
        when(providerRepository.findLatestByProviderId("master"))
                .thenReturn(Optional.of(current));

        service.updateMutableFields("master", "New", "new-client", "", "user:admin");

        ArgumentCaptor<SystemOidcProvider> captor =
                ArgumentCaptor.forClass(SystemOidcProvider.class);
        verify(providerRepository).save(captor.capture());
        assertThat(captor.getValue().getClientSecret()).isEqualTo("old-encrypted");
        verify(cryptoPort, never()).encrypt(any());
    }

    @Test
    @DisplayName("updateMutableFields: プロバイダーが存在しない場合、PROVIDER_NOT_FOUND例外、保存しない")
    void updateMutableFields_throwsAndDoesNotSave_whenNotFound() {
        when(providerRepository.findLatestByProviderId("not-exist"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.updateMutableFields("not-exist", "X", "x", "x", "u"))
                .isInstanceOf(OidcProviderException.class)
                .extracting("error")
                .isEqualTo(OidcProviderError.PROVIDER_NOT_FOUND);

        verify(providerRepository, never()).save(any());
    }

    @Test
    @DisplayName("findEncryptedClientSecret: プロバイダーが存在する場合、暗号化済みsecretを返す")
    void findEncryptedClientSecret_returnsEncryptedSecret_whenExists() {
        when(providerRepository.findLatestByProviderId("master"))
                .thenReturn(Optional.of(providerOf("master", BASE_TIME, "encrypted-value")));

        Optional<String> result = service.findEncryptedClientSecret("master");

        assertThat(result).contains("encrypted-value");
    }

    @Test
    @DisplayName("findEncryptedClientSecret: プロバイダーが存在しない場合、空のOptionalを返す")
    void findEncryptedClientSecret_returnsEmpty_whenNotExists() {
        when(providerRepository.findLatestByProviderId("not-exist"))
                .thenReturn(Optional.empty());

        Optional<String> result = service.findEncryptedClientSecret("not-exist");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("listAll: ENABLED→DISABLEDの順、各群内では displayName 昇順")
    void listAll_sortedByEnabledFirstThenName() {
        SystemOidcProvider g = new SystemOidcProvider(
                "google", BASE_TIME, "Google", "u-g", "c-g", "s-g",
                BASE_TIME, "u");
        SystemOidcProvider m = new SystemOidcProvider(
                "master", BASE_TIME, "Master", "u-m", "c-m", "s-m",
                BASE_TIME, "u");
        SystemOidcProvider a = new SystemOidcProvider(
                "azure", BASE_TIME, "Azure AD", "u-a", "c-a", "s-a",
                BASE_TIME, "u");

        when(providerRepository.findAllLatest()).thenReturn(List.of(g, m, a));
        when(statusRepository.findLatestByProviderId("google"))
                .thenReturn(Optional.of(statusOf("google", BASE_TIME,
                        OidcProviderStatusValue.DISABLED)));
        when(statusRepository.findLatestByProviderId("master"))
                .thenReturn(Optional.of(statusOf("master", BASE_TIME,
                        OidcProviderStatusValue.ENABLED)));
        when(statusRepository.findLatestByProviderId("azure"))
                .thenReturn(Optional.of(statusOf("azure", BASE_TIME,
                        OidcProviderStatusValue.ENABLED)));

        List<ProviderWithStatus> result = service.listAll();

        assertThat(result).hasSize(3);
        assertThat(result.get(0).provider().getProviderId()).isEqualTo("azure");
        assertThat(result.get(1).provider().getProviderId()).isEqualTo("master");
        assertThat(result.get(2).provider().getProviderId()).isEqualTo("google");
    }

    @Test
    @DisplayName("listAll: プロバイダーが存在しない場合、空のリストを返す")
    void listAll_returnsEmpty_whenNoProviders() {
        when(providerRepository.findAllLatest()).thenReturn(List.of());

        List<ProviderWithStatus> result = service.listAll();

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findDetailByProviderId: プロバイダーとステータスが揃って取得できる")
    void findDetailByProviderId_returnsProviderWithStatus() {
        SystemOidcProvider provider = providerOf("master", BASE_TIME, "secret");
        SystemOidcProviderStatus status =
                statusOf("master", BASE_TIME, OidcProviderStatusValue.ENABLED);
        when(providerRepository.findLatestByProviderId("master"))
                .thenReturn(Optional.of(provider));
        when(statusRepository.findLatestByProviderId("master"))
                .thenReturn(Optional.of(status));

        Optional<ProviderWithStatus> result = service.findDetailByProviderId("master");

        assertThat(result).isPresent();
        assertThat(result.get().provider()).isEqualTo(provider);
        assertThat(result.get().status()).isEqualTo(status);
    }

    @Test
    @DisplayName("findDetailByProviderId: プロバイダーが存在しない場合、空のOptionalを返す")
    void findDetailByProviderId_returnsEmpty_whenNotExists() {
        when(providerRepository.findLatestByProviderId("not-exist"))
                .thenReturn(Optional.empty());

        Optional<ProviderWithStatus> result =
                service.findDetailByProviderId("not-exist");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findDetailByProviderId: プロバイダーは存在するがステータスがない場合、status=nullで返る")
    void findDetailByProviderId_returnsWithNullStatus_whenStatusMissing() {
        SystemOidcProvider provider = providerOf("master", BASE_TIME, "secret");
        when(providerRepository.findLatestByProviderId("master"))
                .thenReturn(Optional.of(provider));
        when(statusRepository.findLatestByProviderId("master"))
                .thenReturn(Optional.empty());

        Optional<ProviderWithStatus> result = service.findDetailByProviderId("master");

        assertThat(result).isPresent();
        assertThat(result.get().provider()).isEqualTo(provider);
        assertThat(result.get().status()).isNull();
    }

    @Test
    @DisplayName("enable: DISABLEDなプロバイダーをENABLEDに変更できる")
    void enable_changesDisabledToEnabled() {
        when(statusRepository.findLatestByProviderId("master"))
                .thenReturn(Optional.of(
                        statusOf("master", BASE_TIME, OidcProviderStatusValue.DISABLED)));

        service.enable("master", "re-activated", "user:admin");

        ArgumentCaptor<SystemOidcProviderStatus> captor =
                ArgumentCaptor.forClass(SystemOidcProviderStatus.class);
        verify(statusRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus())
                .isEqualTo(OidcProviderStatusValue.ENABLED);
        assertThat(captor.getValue().getReason()).isEqualTo("re-activated");
        assertThat(captor.getValue().getCreatedBy()).isEqualTo("user:admin");
    }

    @Test
    @DisplayName("enable: プロバイダーが存在しない場合、PROVIDER_NOT_FOUND例外、保存しない")
    void enable_throwsAndDoesNotSave_whenNotFound() {
        when(statusRepository.findLatestByProviderId("not-exist"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.enable("not-exist", "any", "user"))
                .isInstanceOf(OidcProviderException.class)
                .extracting("error")
                .isEqualTo(OidcProviderError.PROVIDER_NOT_FOUND);

        verify(statusRepository, never()).save(any());
    }

    @Test
    @DisplayName("enable: 既にENABLEDの場合、ALREADY_ENABLED例外、保存しない")
    void enable_throwsAndDoesNotSave_whenAlreadyEnabled() {
        when(statusRepository.findLatestByProviderId("master"))
                .thenReturn(Optional.of(
                        statusOf("master", BASE_TIME, OidcProviderStatusValue.ENABLED)));

        assertThatThrownBy(() ->
                service.enable("master", "any", "user"))
                .isInstanceOf(OidcProviderException.class)
                .extracting("error")
                .isEqualTo(OidcProviderError.ALREADY_ENABLED);

        verify(statusRepository, never()).save(any());
    }

    @Test
    @DisplayName("enable: 状態変更時のversionと createdAt が同じ値で記録される")
    void enable_versionAndCreatedAtAreSameValue() {
        when(statusRepository.findLatestByProviderId("master"))
                .thenReturn(Optional.of(
                        statusOf("master", BASE_TIME, OidcProviderStatusValue.DISABLED)));

        service.enable("master", "any", "user");

        ArgumentCaptor<SystemOidcProviderStatus> captor =
                ArgumentCaptor.forClass(SystemOidcProviderStatus.class);
        verify(statusRepository).save(captor.capture());
        assertThat(captor.getValue().getVersion())
                .isEqualTo(captor.getValue().getCreatedAt());
    }

    @Test
    @DisplayName("disable: 他のENABLEDが存在する場合、ENABLED→DISABLEDに変更できる")
    void disable_changesEnabledToDisabled_whenOtherEnabledExists() {
        when(statusRepository.findLatestByProviderId("master"))
                .thenReturn(Optional.of(
                        statusOf("master", BASE_TIME, OidcProviderStatusValue.ENABLED)));
        when(statusRepository.findProviderIdsByLatestStatus(OidcProviderStatusValue.ENABLED))
                .thenReturn(List.of("master", "google"));

        service.disable("master", "deprecated", "user:admin");

        ArgumentCaptor<SystemOidcProviderStatus> captor =
                ArgumentCaptor.forClass(SystemOidcProviderStatus.class);
        verify(statusRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus())
                .isEqualTo(OidcProviderStatusValue.DISABLED);
        assertThat(captor.getValue().getReason()).isEqualTo("deprecated");
    }

    @Test
    @DisplayName("disable: プロバイダーが存在しない場合、PROVIDER_NOT_FOUND例外、保存しない")
    void disable_throwsAndDoesNotSave_whenNotFound() {
        when(statusRepository.findLatestByProviderId("not-exist"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.disable("not-exist", "any", "user"))
                .isInstanceOf(OidcProviderException.class)
                .extracting("error")
                .isEqualTo(OidcProviderError.PROVIDER_NOT_FOUND);

        verify(statusRepository, never()).save(any());
    }

    @Test
    @DisplayName("disable: 既にDISABLEDの場合、ALREADY_DISABLED例外、保存しない")
    void disable_throwsAndDoesNotSave_whenAlreadyDisabled() {
        when(statusRepository.findLatestByProviderId("master"))
                .thenReturn(Optional.of(
                        statusOf("master", BASE_TIME, OidcProviderStatusValue.DISABLED)));

        assertThatThrownBy(() ->
                service.disable("master", "any", "user"))
                .isInstanceOf(OidcProviderException.class)
                .extracting("error")
                .isEqualTo(OidcProviderError.ALREADY_DISABLED);

        verify(statusRepository, never()).save(any());
        verify(statusRepository, never())
                .findProviderIdsByLatestStatus(any());
    }

    @Test
    @DisplayName("disable: 最後のENABLEDプロバイダーを無効化しようとするとLAST_ENABLED_REQUIRED例外、保存しない")
    void disable_throwsLastEnabledRequired_whenSelfIsLastEnabled() {
        when(statusRepository.findLatestByProviderId("master"))
                .thenReturn(Optional.of(
                        statusOf("master", BASE_TIME, OidcProviderStatusValue.ENABLED)));
        when(statusRepository.findProviderIdsByLatestStatus(OidcProviderStatusValue.ENABLED))
                .thenReturn(List.of("master"));

        assertThatThrownBy(() ->
                service.disable("master", "any", "user"))
                .isInstanceOf(OidcProviderException.class)
                .extracting("error")
                .isEqualTo(OidcProviderError.LAST_ENABLED_REQUIRED);

        verify(statusRepository, never()).save(any());
    }

    @Test
    @DisplayName("disable: ENABLED一覧が空の場合もLAST_ENABLED_REQUIRED例外、保存しない")
    void disable_throwsLastEnabledRequired_whenEnabledListEmpty() {
        when(statusRepository.findLatestByProviderId("master"))
                .thenReturn(Optional.of(
                        statusOf("master", BASE_TIME, OidcProviderStatusValue.ENABLED)));
        when(statusRepository.findProviderIdsByLatestStatus(OidcProviderStatusValue.ENABLED))
                .thenReturn(List.of());

        assertThatThrownBy(() ->
                service.disable("master", "any", "user"))
                .isInstanceOf(OidcProviderException.class)
                .extracting("error")
                .isEqualTo(OidcProviderError.LAST_ENABLED_REQUIRED);

        verify(statusRepository, never()).save(any());
    }

}
