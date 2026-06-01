package io.github.kizulog_community.kizulog.domain.setup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

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
import io.github.kizulog_community.kizulog.domain.systemconfig.model.LanguageSetting;
import io.github.kizulog_community.kizulog.domain.systemconfig.model.TimezoneSetting;
import io.github.kizulog_community.kizulog.domain.systemconfig.service.LocalizationSettingService;
import io.github.kizulog_community.kizulog.domain.systemoidc.model.OidcProviderStatusValue;
import io.github.kizulog_community.kizulog.domain.systemoidc.service.SystemOidcProviderService;
import io.github.kizulog_community.kizulog.infrastructure.web.setup.OidcSetting;
import io.github.kizulog_community.kizulog.infrastructure.web.setup.SetupSessionData;

/**
 * SetupServiceの単体テスト
 *
 * @author Jun Kobayashi
 */
class SetupServiceTest {

    private SystemAccountRepository systemAccountRepository;
    private SystemAccountStatusRepository systemAccountStatusRepository;
    private SystemAccountIdentityRepository systemAccountIdentityRepository;
    private SystemAccountIdentityStatusRepository systemAccountIdentityStatusRepository;
    private SystemAccountRoleRepository systemAccountRoleRepository;
    private SystemAccountRoleStatusRepository systemAccountRoleStatusRepository;
    private SystemOidcProviderService systemOidcProviderService;
    private LocalizationSettingService localizationSettingService;
    private SetupService service;

    @BeforeEach
    void setUp() {
        systemAccountRepository = mock(SystemAccountRepository.class);
        systemAccountStatusRepository = mock(SystemAccountStatusRepository.class);
        systemAccountIdentityRepository = mock(SystemAccountIdentityRepository.class);
        systemAccountIdentityStatusRepository = mock(SystemAccountIdentityStatusRepository.class);
        systemAccountRoleRepository = mock(SystemAccountRoleRepository.class);
        systemAccountRoleStatusRepository = mock(SystemAccountRoleStatusRepository.class);
        systemOidcProviderService = mock(SystemOidcProviderService.class);
        localizationSettingService = mock(LocalizationSettingService.class);

        service = new SetupService(
                systemAccountRepository,
                systemAccountStatusRepository,
                systemAccountIdentityRepository,
                systemAccountIdentityStatusRepository,
                systemAccountRoleRepository,
                systemAccountRoleStatusRepository,
                systemOidcProviderService,
                localizationSettingService);
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
    @DisplayName("save()でlocalizationSettingService.saveBoth()が1回呼ばれる")
    void save_callsLocalizationSaveBothOnce() {
        service.save(createSessionData());
        verify(localizationSettingService).saveBoth(
                any(LanguageSetting.class),
                any(TimezoneSetting.class),
                any(OffsetDateTime.class),
                eq("system:setup-wizard"));
    }

    @Test
    @DisplayName("save()でsystemOidcProviderService.registerが呼ばれる")
    void save_callsSystemOidcProviderServiceRegister() {
        service.save(createSessionData());
        verify(systemOidcProviderService).register(
                eq("master"),
                eq("master"),                                   // displayName=id
                eq("https://auth.example.com/realms/master"),
                eq("kizulog-client"),
                eq("plain-secret"),
                any(),                                          // claimsMapping (T.0: デフォルトマッピング)
                eq(OidcProviderStatusValue.ENABLED),
                any(),                                          // version
                eq("system:setup-wizard"));
    }

    @Test
    @DisplayName("save()でsaveBoth()に渡されるLanguageSettingがセッションの値と一致する")
    void save_passesCorrectLanguageSetting() {
        service.save(createSessionData());

        ArgumentCaptor<LanguageSetting> captor =
                ArgumentCaptor.forClass(LanguageSetting.class);
        verify(localizationSettingService).saveBoth(
                captor.capture(),
                any(TimezoneSetting.class),
                any(OffsetDateTime.class),
                any(String.class));

        LanguageSetting captured = captor.getValue();
        assertThat(captured.getDefaultLanguage()).isEqualTo(SupportedLanguage.JA);
        assertThat(captured.getAvailableLanguages())
                .containsExactly(SupportedLanguage.JA, SupportedLanguage.EN);
    }

    @Test
    @DisplayName("save()でsaveBoth()に渡されるTimezoneSettingがセッションの値と一致する")
    void save_passesCorrectTimezoneSetting() {
        service.save(createSessionData());

        ArgumentCaptor<TimezoneSetting> captor =
                ArgumentCaptor.forClass(TimezoneSetting.class);
        verify(localizationSettingService).saveBoth(
                any(LanguageSetting.class),
                captor.capture(),
                any(OffsetDateTime.class),
                any(String.class));

        TimezoneSetting captured = captor.getValue();
        assertThat(captured.getDefaultTimezone().getId()).isEqualTo("Asia/Tokyo");
        assertThat(captured.getAvailableTimezones())
                .extracting(SupportedTimezone::getId)
                .containsExactly("Asia/Tokyo");
    }

    @Test
    @DisplayName("save()でOIDCプロバイダーとLocalizationが同じversionで保存される")
    void save_passesSameVersionToOidcAndLocalization() {
        service.save(createSessionData());

        ArgumentCaptor<OffsetDateTime> oidcVersionCaptor =
                ArgumentCaptor.forClass(OffsetDateTime.class);
        ArgumentCaptor<OffsetDateTime> localizationVersionCaptor =
                ArgumentCaptor.forClass(OffsetDateTime.class);

        verify(systemOidcProviderService).register(
                any(), any(), any(), any(), any(), any(), any(),
                oidcVersionCaptor.capture(),
                any());
        verify(localizationSettingService).saveBoth(
                any(LanguageSetting.class),
                any(TimezoneSetting.class),
                localizationVersionCaptor.capture(),
                any(String.class));

        assertThat(localizationVersionCaptor.getValue())
                .isEqualTo(oidcVersionCaptor.getValue());
    }

    @Test
    @DisplayName("save()でsystem_accountsが保存される（accountIdのみ、iss/aud/subはidentity側）")
    void save_savesSystemAccount() {
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
    @DisplayName("save()でsystem_account_rolesがSYSTEM_ADMINで保存される(role_idベース)")
    void save_savesSystemAccountRoleAsSystemAdmin() {
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

}
