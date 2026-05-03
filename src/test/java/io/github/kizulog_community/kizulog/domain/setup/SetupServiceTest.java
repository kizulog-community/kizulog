package io.github.kizulog_community.kizulog.domain.setup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.kizulog_community.kizulog.domain.port.CryptoPort;
import io.github.kizulog_community.kizulog.domain.shared.SupportedLanguage;
import io.github.kizulog_community.kizulog.domain.shared.SupportedTimezone;
import io.github.kizulog_community.kizulog.domain.systemaccount.model.AccountStatus;
import io.github.kizulog_community.kizulog.domain.systemaccount.model.SystemAccount;
import io.github.kizulog_community.kizulog.domain.systemaccount.model.SystemAccountIdentity;
import io.github.kizulog_community.kizulog.domain.systemaccount.model.SystemAccountIdentityStatus;
import io.github.kizulog_community.kizulog.domain.systemaccount.model.SystemAccountRole;
import io.github.kizulog_community.kizulog.domain.systemaccount.model.SystemAccountRoleStatus;
import io.github.kizulog_community.kizulog.domain.systemaccount.model.SystemAccountStatus;
import io.github.kizulog_community.kizulog.domain.systemaccount.model.SystemRole;
import io.github.kizulog_community.kizulog.domain.systemaccount.port.SystemAccountIdentityRepository;
import io.github.kizulog_community.kizulog.domain.systemaccount.port.SystemAccountIdentityStatusRepository;
import io.github.kizulog_community.kizulog.domain.systemaccount.port.SystemAccountRepository;
import io.github.kizulog_community.kizulog.domain.systemaccount.port.SystemAccountRoleRepository;
import io.github.kizulog_community.kizulog.domain.systemaccount.port.SystemAccountRoleStatusRepository;
import io.github.kizulog_community.kizulog.domain.systemaccount.port.SystemAccountStatusRepository;
import io.github.kizulog_community.kizulog.domain.systemconfig.model.SystemConfig;
import io.github.kizulog_community.kizulog.domain.systemconfig.port.SystemConfigRepository;
import io.github.kizulog_community.kizulog.infrastructure.web.setup.OidcSetting;
import io.github.kizulog_community.kizulog.infrastructure.web.setup.SetupSessionData;

/**
 * SetupServiceの単体テスト
 *
 * @author Jun Kobayashi
 */
class SetupServiceTest {

    private SystemConfigRepository systemConfigRepository;
    private SystemAccountRepository systemAccountRepository;
    private SystemAccountStatusRepository systemAccountStatusRepository;
    private SystemAccountIdentityRepository systemAccountIdentityRepository;
    private SystemAccountIdentityStatusRepository systemAccountIdentityStatusRepository;
    private SystemAccountRoleRepository systemAccountRoleRepository;
    private SystemAccountRoleStatusRepository systemAccountRoleStatusRepository;
    private CryptoPort cryptoPort;
    private ObjectMapper objectMapper;
    private SetupService service;

    @BeforeEach
    void setUp() {
        systemConfigRepository = mock(SystemConfigRepository.class);
        systemAccountRepository = mock(SystemAccountRepository.class);
        systemAccountStatusRepository = mock(SystemAccountStatusRepository.class);
        systemAccountIdentityRepository = mock(SystemAccountIdentityRepository.class);
        systemAccountIdentityStatusRepository = mock(SystemAccountIdentityStatusRepository.class);
        systemAccountRoleRepository = mock(SystemAccountRoleRepository.class);
        systemAccountRoleStatusRepository = mock(SystemAccountRoleStatusRepository.class);
        cryptoPort = mock(CryptoPort.class);
        objectMapper = new ObjectMapper();

        service = new SetupService(
                systemConfigRepository,
                systemAccountRepository,
                systemAccountStatusRepository,
                systemAccountIdentityRepository,
                systemAccountIdentityStatusRepository,
                systemAccountRoleRepository,
                systemAccountRoleStatusRepository,
                cryptoPort,
                objectMapper);
    }

    /**
     * テスト用のSetupSessionDataを生成
     */
    private SetupSessionData createSessionData() {
        SetupSessionData data = new SetupSessionData();
        data.setSetupLanguage(SupportedLanguage.JA);
        data.setDefaultLanguage(SupportedLanguage.JA);
        data.setAvailableLanguages(List.of(SupportedLanguage.JA, SupportedLanguage.EN));
        data.setDefaultTimezone(SupportedTimezone.of("Asia/Tokyo"));
        data.setAvailableTimezones(List.of(SupportedTimezone.of("Asia/Tokyo")));
        data.setHost("kizulog.example.com");

        OidcSetting oidc = new OidcSetting();
        oidc.setId("master");
        oidc.setUri("https://auth.example.com/realms/master");
        oidc.setClientId("kizulog-client");
        oidc.setClientSecret("plain-secret");
        data.setOidcSettings(new java.util.ArrayList<>(List.of(oidc)));

        data.setAdminIss("https://auth.example.com/realms/master");
        data.setAdminAud("kizulog-client");
        data.setAdminSub("user-uuid-123");
        return data;
    }

    @Test
    @DisplayName("save()でsystem_configが3件（OIDC/LANGUAGE/TIMEZONE）保存される")
    void save_savesThreeSystemConfigs() {
        when(cryptoPort.encrypt("plain-secret")).thenReturn("encrypted-secret");
        service.save(createSessionData());
        verify(systemConfigRepository, times(3)).save(any(SystemConfig.class));
    }

    @Test
    @DisplayName("save()でOIDC設定が暗号化されて保存される")
    void save_oidcClientSecretIsEncrypted() {
        when(cryptoPort.encrypt("plain-secret")).thenReturn("encrypted-secret");
        service.save(createSessionData());
        verify(cryptoPort).encrypt("plain-secret");
    }

    @Test
    @DisplayName("save()のOIDC設定にencrypted-secretが含まれる")
    void save_oidcConfigContainsEncryptedSecret() {
        when(cryptoPort.encrypt("plain-secret")).thenReturn("encrypted-secret");
        service.save(createSessionData());

        ArgumentCaptor<SystemConfig> captor = ArgumentCaptor.forClass(SystemConfig.class);
        verify(systemConfigRepository, times(3)).save(captor.capture());

        SystemConfig oidcConfig = captor.getAllValues().stream()
                .filter(c -> "OIDC".equals(c.getKey()))
                .findFirst().orElseThrow();
        assertThat(oidcConfig.getValue()).contains("encrypted-secret");
        assertThat(oidcConfig.getValue()).doesNotContain("plain-secret");
    }

    @Test
    @DisplayName("save()のLANGUAGE設定にDEFAULTとAVAILABLEが含まれる")
    void save_languageConfigContainsDefaultAndAvailable() {
        when(cryptoPort.encrypt("plain-secret")).thenReturn("encrypted-secret");
        service.save(createSessionData());

        ArgumentCaptor<SystemConfig> captor = ArgumentCaptor.forClass(SystemConfig.class);
        verify(systemConfigRepository, times(3)).save(captor.capture());

        SystemConfig langConfig = captor.getAllValues().stream()
                .filter(c -> "LANGUAGE".equals(c.getKey()))
                .findFirst().orElseThrow();
        assertThat(langConfig.getValue()).contains("\"DEFAULT\":\"ja\"");
        assertThat(langConfig.getValue()).contains("\"AVAILABLE\":[\"ja\",\"en\"]");
    }

    @Test
    @DisplayName("save()のTIMEZONE設定にDEFAULTとAVAILABLEが含まれる")
    void save_timezoneConfigContainsDefaultAndAvailable() {
        when(cryptoPort.encrypt("plain-secret")).thenReturn("encrypted-secret");
        service.save(createSessionData());

        ArgumentCaptor<SystemConfig> captor = ArgumentCaptor.forClass(SystemConfig.class);
        verify(systemConfigRepository, times(3)).save(captor.capture());

        SystemConfig tzConfig = captor.getAllValues().stream()
                .filter(c -> "TIMEZONE".equals(c.getKey()))
                .findFirst().orElseThrow();
        assertThat(tzConfig.getValue()).contains("\"DEFAULT\":\"Asia/Tokyo\"");
        assertThat(tzConfig.getValue()).contains("\"AVAILABLE\":[\"Asia/Tokyo\"]");
    }

    @Test
    @DisplayName("save()でsystem_accountsが保存される（accountIdのみ、iss/aud/subはidentity側）")
    void save_savesSystemAccount() {
        when(cryptoPort.encrypt("plain-secret")).thenReturn("encrypted-secret");
        service.save(createSessionData());

        ArgumentCaptor<SystemAccount> captor =
                ArgumentCaptor.forClass(SystemAccount.class);
        verify(systemAccountRepository).save(captor.capture());

        SystemAccount account = captor.getValue();
        assertThat(account.getAccountId()).isNotBlank();
        assertThat(account.getCreatedBy()).isEqualTo("system:setup-wizard");
    }

    @Test
    @DisplayName("save()でsystem_account_statusがACTIVEで保存される")
    void save_savesSystemAccountStatusAsActive() {
        when(cryptoPort.encrypt("plain-secret")).thenReturn("encrypted-secret");
        service.save(createSessionData());

        ArgumentCaptor<SystemAccountStatus> captor =
                ArgumentCaptor.forClass(SystemAccountStatus.class);
        verify(systemAccountStatusRepository).save(captor.capture());

        SystemAccountStatus status = captor.getValue();
        assertThat(status.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(status.getReason()).isNull();
        assertThat(status.getCreatedBy()).isEqualTo("system:setup-wizard");
    }

    @Test
    @DisplayName("save()でsystem_account_identitiesがiss/aud/sub付きで保存される")
    void save_savesSystemAccountIdentity() {
        when(cryptoPort.encrypt("plain-secret")).thenReturn("encrypted-secret");
        service.save(createSessionData());

        ArgumentCaptor<SystemAccountIdentity> captor =
                ArgumentCaptor.forClass(SystemAccountIdentity.class);
        verify(systemAccountIdentityRepository).save(captor.capture());

        SystemAccountIdentity identity = captor.getValue();
        assertThat(identity.getIdentityId()).isNotBlank();
        assertThat(identity.getIss()).isEqualTo("https://auth.example.com/realms/master");
        assertThat(identity.getAud()).isEqualTo("kizulog-client");
        assertThat(identity.getSub()).isEqualTo("user-uuid-123");
        assertThat(identity.getCreatedBy()).isEqualTo("system:setup-wizard");
    }

    @Test
    @DisplayName("save()でsystem_account_identity_statusがACTIVEで保存される")
    void save_savesSystemAccountIdentityStatusAsActive() {
        when(cryptoPort.encrypt("plain-secret")).thenReturn("encrypted-secret");
        service.save(createSessionData());

        ArgumentCaptor<SystemAccountIdentityStatus> captor =
                ArgumentCaptor.forClass(SystemAccountIdentityStatus.class);
        verify(systemAccountIdentityStatusRepository).save(captor.capture());

        SystemAccountIdentityStatus status = captor.getValue();
        assertThat(status.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(status.getReason()).isNull();
        assertThat(status.getCreatedBy()).isEqualTo("system:setup-wizard");
    }

    @Test
    @DisplayName("save()でsystem_account_rolesがSYSTEM_ADMINで保存される（role_idベース）")
    void save_savesSystemAccountRoleAsSystemAdmin() {
        when(cryptoPort.encrypt("plain-secret")).thenReturn("encrypted-secret");
        service.save(createSessionData());

        ArgumentCaptor<SystemAccountRole> captor =
                ArgumentCaptor.forClass(SystemAccountRole.class);
        verify(systemAccountRoleRepository).save(captor.capture());

        SystemAccountRole role = captor.getValue();
        assertThat(role.getRoleId()).isNotBlank();
        assertThat(role.getRole()).isEqualTo(SystemRole.SYSTEM_ADMIN);
        assertThat(role.getCreatedBy()).isEqualTo("system:setup-wizard");
    }

    @Test
    @DisplayName("save()でsystem_account_role_statusがACTIVEで保存される")
    void save_savesSystemAccountRoleStatusAsActive() {
        when(cryptoPort.encrypt("plain-secret")).thenReturn("encrypted-secret");
        service.save(createSessionData());

        ArgumentCaptor<SystemAccountRoleStatus> captor =
                ArgumentCaptor.forClass(SystemAccountRoleStatus.class);
        verify(systemAccountRoleStatusRepository).save(captor.capture());

        SystemAccountRoleStatus status = captor.getValue();
        assertThat(status.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(status.getReason()).isNull();
        assertThat(status.getCreatedBy()).isEqualTo("system:setup-wizard");
    }

    @Test
    @DisplayName("save()でアカウント・ステータス・identityが同じaccountIdで保存される")
    void save_useSameAccountIdAcrossEntities() {
        when(cryptoPort.encrypt("plain-secret")).thenReturn("encrypted-secret");
        service.save(createSessionData());

        ArgumentCaptor<SystemAccount> accountCaptor =
                ArgumentCaptor.forClass(SystemAccount.class);
        ArgumentCaptor<SystemAccountStatus> accountStatusCaptor =
                ArgumentCaptor.forClass(SystemAccountStatus.class);
        ArgumentCaptor<SystemAccountIdentity> identityCaptor =
                ArgumentCaptor.forClass(SystemAccountIdentity.class);
        ArgumentCaptor<SystemAccountRole> roleCaptor =
                ArgumentCaptor.forClass(SystemAccountRole.class);

        verify(systemAccountRepository).save(accountCaptor.capture());
        verify(systemAccountStatusRepository).save(accountStatusCaptor.capture());
        verify(systemAccountIdentityRepository).save(identityCaptor.capture());
        verify(systemAccountRoleRepository).save(roleCaptor.capture());

        String accountId = accountCaptor.getValue().getAccountId();
        assertThat(accountStatusCaptor.getValue().getAccountId()).isEqualTo(accountId);
        assertThat(identityCaptor.getValue().getAccountId()).isEqualTo(accountId);
        assertThat(roleCaptor.getValue().getAccountId()).isEqualTo(accountId);
    }

    @Test
    @DisplayName("save()でidentityとidentity_statusが同じidentityIdで保存される")
    void save_useSameIdentityIdAcrossIdentityEntities() {
        when(cryptoPort.encrypt("plain-secret")).thenReturn("encrypted-secret");
        service.save(createSessionData());

        ArgumentCaptor<SystemAccountIdentity> identityCaptor =
                ArgumentCaptor.forClass(SystemAccountIdentity.class);
        ArgumentCaptor<SystemAccountIdentityStatus> statusCaptor =
                ArgumentCaptor.forClass(SystemAccountIdentityStatus.class);

        verify(systemAccountIdentityRepository).save(identityCaptor.capture());
        verify(systemAccountIdentityStatusRepository).save(statusCaptor.capture());

        assertThat(statusCaptor.getValue().getIdentityId())
                .isEqualTo(identityCaptor.getValue().getIdentityId());
    }

    @Test
    @DisplayName("save()でroleとrole_statusが同じroleIdで保存される")
    void save_useSameRoleIdAcrossRoleEntities() {
        when(cryptoPort.encrypt("plain-secret")).thenReturn("encrypted-secret");
        service.save(createSessionData());

        ArgumentCaptor<SystemAccountRole> roleCaptor =
                ArgumentCaptor.forClass(SystemAccountRole.class);
        ArgumentCaptor<SystemAccountRoleStatus> statusCaptor =
                ArgumentCaptor.forClass(SystemAccountRoleStatus.class);

        verify(systemAccountRoleRepository).save(roleCaptor.capture());
        verify(systemAccountRoleStatusRepository).save(statusCaptor.capture());

        assertThat(statusCaptor.getValue().getRoleId())
                .isEqualTo(roleCaptor.getValue().getRoleId());
    }

    @Test
    @DisplayName("save()でOIDCのJSON変換に失敗するとRuntimeException")
    void save_oidcJsonProcessingException_throwsRuntimeException() throws Exception {
        ObjectMapper mockMapper = mock(ObjectMapper.class);
        when(mockMapper.writeValueAsString(any()))
                .thenThrow(new JsonProcessingException("error") {
                    private static final long serialVersionUID = 1L;
                });

        SetupService failingService = new SetupService(
                systemConfigRepository,
                systemAccountRepository,
                systemAccountStatusRepository,
                systemAccountIdentityRepository,
                systemAccountIdentityStatusRepository,
                systemAccountRoleRepository,
                systemAccountRoleStatusRepository,
                cryptoPort,
                mockMapper);

        when(cryptoPort.encrypt(any())).thenReturn("encrypted");

        assertThatThrownBy(() -> failingService.save(createSessionData()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("OIDC設定のJSON変換に失敗しました");
    }

}
