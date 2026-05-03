package io.github.kizulog_community.kizulog.domain.systemauth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.kizulog_community.kizulog.domain.port.CryptoPort;
import io.github.kizulog_community.kizulog.domain.systemauth.model.SystemOidcSettings;
import io.github.kizulog_community.kizulog.domain.systemconfig.model.SystemConfig;
import io.github.kizulog_community.kizulog.domain.systemconfig.service.SystemConfigService;

/**
 * SystemOidcSettingServiceの単体テスト
 *
 * @author Jun Kobayashi
 */
class SystemOidcSettingServiceTest {

    private static final OffsetDateTime VERSION =
            OffsetDateTime.of(2026, 5, 1, 12, 0, 0, 0, ZoneOffset.UTC);

    private SystemConfigService systemConfigService;
    private CryptoPort cryptoPort;
    private ObjectMapper objectMapper;
    private SystemOidcSettingService service;

    @BeforeEach
    void setUp() {
        systemConfigService = mock(SystemConfigService.class);
        cryptoPort = mock(CryptoPort.class);
        objectMapper = new ObjectMapper();
        service = new SystemOidcSettingService(systemConfigService, cryptoPort, objectMapper);
    }

    /**
     * テスト用のSystemConfigを生成する
     */
    private SystemConfig createSystemConfig(String value) {
        return new SystemConfig("OIDC", VERSION, value, VERSION, "test-user");
    }

    @Test
    @DisplayName("findLatest：system_configにOIDCキーが存在しない場合は空のOptionalを返す")
    void findLatest_whenConfigNotFound_returnsEmpty() {
        when(systemConfigService.findLatestByKey("OIDC")).thenReturn(Optional.empty());

        Optional<SystemOidcSettings> result = service.findLatest();

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findLatest：1件のOIDC設定が正しく復号されて返される")
    void findLatest_withSingleSetting_returnsDecryptedSetting() {
        String json = """
                [
                    {
                        "id": "master",
                        "uri": "https://auth.example/realms/master",
                        "clientId": "kizulog-master",
                        "clientSecret": "encrypted-secret"
                    }
                ]
                """;
        when(systemConfigService.findLatestByKey("OIDC"))
                .thenReturn(Optional.of(createSystemConfig(json)));
        when(cryptoPort.decrypt("encrypted-secret")).thenReturn("plain-secret");

        Optional<SystemOidcSettings> result = service.findLatest();

        assertThat(result).isPresent();
        SystemOidcSettings settings = result.get();
        assertThat(settings.getVersion()).isEqualTo(VERSION);
        assertThat(settings.getSettings()).hasSize(1);
        assertThat(settings.getSettings().get(0).getId()).isEqualTo("master");
        assertThat(settings.getSettings().get(0).getUri())
                .isEqualTo("https://auth.example/realms/master");
        assertThat(settings.getSettings().get(0).getClientId()).isEqualTo("kizulog-master");
        assertThat(settings.getSettings().get(0).getClientSecret()).isEqualTo("plain-secret");
    }

    @Test
    @DisplayName("findLatest：複数件のOIDC設定が正しく扱われる")
    void findLatest_withMultipleSettings_returnsAllSettings() {
        String json = """
                [
                    {
                        "id": "master",
                        "uri": "https://auth.example/realms/master",
                        "clientId": "client-master",
                        "clientSecret": "encrypted-1"
                    },
                    {
                        "id": "secondary",
                        "uri": "https://auth.example/realms/secondary",
                        "clientId": "client-secondary",
                        "clientSecret": "encrypted-2"
                    }
                ]
                """;
        when(systemConfigService.findLatestByKey("OIDC"))
                .thenReturn(Optional.of(createSystemConfig(json)));
        when(cryptoPort.decrypt("encrypted-1")).thenReturn("plain-1");
        when(cryptoPort.decrypt("encrypted-2")).thenReturn("plain-2");

        Optional<SystemOidcSettings> result = service.findLatest();

        assertThat(result).isPresent();
        assertThat(result.get().getSettings()).hasSize(2);
        assertThat(result.get().getSettings().get(0).getId()).isEqualTo("master");
        assertThat(result.get().getSettings().get(0).getClientSecret()).isEqualTo("plain-1");
        assertThat(result.get().getSettings().get(1).getId()).isEqualTo("secondary");
        assertThat(result.get().getSettings().get(1).getClientSecret()).isEqualTo("plain-2");
    }

    @Test
    @DisplayName("findLatest：JSON配列が空の場合は空リストを持つSystemOidcSettingsを返す")
    void findLatest_withEmptyJsonArray_returnsEmptySettings() {
        when(systemConfigService.findLatestByKey("OIDC"))
                .thenReturn(Optional.of(createSystemConfig("[]")));

        Optional<SystemOidcSettings> result = service.findLatest();

        assertThat(result).isPresent();
        assertThat(result.get().getSettings()).isEmpty();
    }

    @Test
    @DisplayName("findLatest：不正なJSONの場合はRuntimeExceptionが発生する")
    void findLatest_withInvalidJson_throwsRuntimeException() {
        when(systemConfigService.findLatestByKey("OIDC"))
                .thenReturn(Optional.of(createSystemConfig("not-a-json")));

        assertThatThrownBy(() -> service.findLatest())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("OIDC設定のJSONパースに失敗しました");
    }

    @Test
    @DisplayName("findLatest：CryptoPort.decryptが暗号化されたclientSecretで呼ばれる")
    void findLatest_callsCryptoPortDecryptWithEncryptedSecret() {
        String json = """
                [
                    {
                        "id": "master",
                        "uri": "https://auth.example/realms/master",
                        "clientId": "kizulog-master",
                        "clientSecret": "encrypted-secret-value"
                    }
                ]
                """;
        when(systemConfigService.findLatestByKey("OIDC"))
                .thenReturn(Optional.of(createSystemConfig(json)));
        when(cryptoPort.decrypt("encrypted-secret-value")).thenReturn("plain");

        service.findLatest();

        verify(cryptoPort).decrypt("encrypted-secret-value");
    }

    @Test
    @DisplayName("findLatest：SystemConfigServiceがOIDCキーで呼ばれる")
    void findLatest_callsSystemConfigServiceWithOidcKey() {
        when(systemConfigService.findLatestByKey("OIDC")).thenReturn(Optional.empty());

        service.findLatest();

        verify(systemConfigService).findLatestByKey("OIDC");
    }

}
