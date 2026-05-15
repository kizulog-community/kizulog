package io.github.kizulog_community.kizulog.domain.systemaccountlocalization.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.kizulog_community.kizulog.domain.shared.SupportedLanguage;
import io.github.kizulog_community.kizulog.domain.shared.SupportedTimezone;
import io.github.kizulog_community.kizulog.domain.systemaccount.model.AccountStatus;
import io.github.kizulog_community.kizulog.domain.systemaccount.model.SystemAccountStatus;
import io.github.kizulog_community.kizulog.domain.systemaccount.port.SystemAccountStatusRepository;
import io.github.kizulog_community.kizulog.domain.systemaccountlocalization.exception.AccountLocalizationError;
import io.github.kizulog_community.kizulog.domain.systemaccountlocalization.exception.AccountLocalizationException;
import io.github.kizulog_community.kizulog.domain.systemaccountlocalization.model.SystemAccountLocalization;
import io.github.kizulog_community.kizulog.domain.systemconfig.exception.LocalizationConfigError;
import io.github.kizulog_community.kizulog.domain.systemconfig.exception.LocalizationConfigException;
import io.github.kizulog_community.kizulog.domain.systemconfig.model.LanguageSetting;
import io.github.kizulog_community.kizulog.domain.systemconfig.model.TimezoneSetting;
import io.github.kizulog_community.kizulog.domain.systemconfig.service.LocalizationSettingService;

/**
 * AccountLocalizationApplicationServiceの単体テスト
 *
 * @author Jun Kobayashi
 */
class AccountLocalizationApplicationServiceTest {

    private static final OffsetDateTime BASE_TIME =
            OffsetDateTime.of(2026, 5, 1, 12, 0, 0, 0, ZoneOffset.UTC);

    private SystemAccountLocalizationService accountLocalizationService;
    private LocalizationSettingService localizationSettingService;
    private SystemAccountStatusRepository systemAccountStatusRepository;
    private AccountLocalizationApplicationService sut;

    @BeforeEach
    void setUp() {
        accountLocalizationService = mock(SystemAccountLocalizationService.class);
        localizationSettingService = mock(LocalizationSettingService.class);
        systemAccountStatusRepository = mock(SystemAccountStatusRepository.class);
        sut = new AccountLocalizationApplicationService(
                accountLocalizationService,
                localizationSettingService,
                systemAccountStatusRepository);
    }

    private LanguageSetting languageSettingJaEn() {
        return new LanguageSetting(
                SupportedLanguage.JA,
                List.of(SupportedLanguage.JA, SupportedLanguage.EN));
    }

    private TimezoneSetting timezoneSettingTokyoNyc() {
        return new TimezoneSetting(
                SupportedTimezone.of("Asia/Tokyo"),
                List.of(
                        SupportedTimezone.of("Asia/Tokyo"),
                        SupportedTimezone.of("America/New_York")));
    }

    private SystemAccountStatus accountStatus(String accountId, AccountStatus status) {
        return new SystemAccountStatus(
                accountId, BASE_TIME, status, null, BASE_TIME, "system");
    }

    @Test
    @DisplayName("createDefaultLocalizationForAccount: システム既定の言語・TZでアカウントlocalizationを保存する")
    void createDefaultLocalizationForAccount_savesWithSystemDefault() {
        when(localizationSettingService.getLanguageSetting())
                .thenReturn(Optional.of(languageSettingJaEn()));
        when(localizationSettingService.getTimezoneSetting())
                .thenReturn(Optional.of(timezoneSettingTokyoNyc()));

        sut.createDefaultLocalizationForAccount("acc-1", "system:invite");

        verify(accountLocalizationService).saveLocalization(
                "acc-1",
                SupportedLanguage.JA,
                SupportedTimezone.of("Asia/Tokyo"),
                "system:invite");
    }

    @Test
    @DisplayName("createDefaultLocalizationForAccount: システム言語設定が未登録の場合は例外")
    void createDefaultLocalizationForAccount_throwsWhenLanguageNotConfigured() {
        when(localizationSettingService.getLanguageSetting()).thenReturn(Optional.empty());
        when(localizationSettingService.getTimezoneSetting())
                .thenReturn(Optional.of(timezoneSettingTokyoNyc()));

        assertThatThrownBy(() ->
                sut.createDefaultLocalizationForAccount("acc-1", "system:invite"))
                .isInstanceOf(AccountLocalizationException.class)
                .extracting("error")
                .isEqualTo(AccountLocalizationError.SYSTEM_LOCALIZATION_NOT_CONFIGURED);

        verify(accountLocalizationService, never()).saveLocalization(
                any(), any(), any(), any());
    }

    @Test
    @DisplayName("createDefaultLocalizationForAccount: システムTZ設定が未登録の場合は例外")
    void createDefaultLocalizationForAccount_throwsWhenTimezoneNotConfigured() {
        when(localizationSettingService.getLanguageSetting())
                .thenReturn(Optional.of(languageSettingJaEn()));
        when(localizationSettingService.getTimezoneSetting()).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                sut.createDefaultLocalizationForAccount("acc-1", "system:invite"))
                .isInstanceOf(AccountLocalizationException.class)
                .extracting("error")
                .isEqualTo(AccountLocalizationError.SYSTEM_LOCALIZATION_NOT_CONFIGURED);

        verify(accountLocalizationService, never()).saveLocalization(
                any(), any(), any(), any());
    }

    @Test
    @DisplayName("createDefaultLocalizationForAccount: システム言語のdefaultがnullの場合は例外")
    void createDefaultLocalizationForAccount_throwsWhenDefaultLanguageIsNull() {
        LanguageSetting brokenLang = new LanguageSetting(null, List.of(SupportedLanguage.JA));
        when(localizationSettingService.getLanguageSetting())
                .thenReturn(Optional.of(brokenLang));
        when(localizationSettingService.getTimezoneSetting())
                .thenReturn(Optional.of(timezoneSettingTokyoNyc()));

        assertThatThrownBy(() ->
                sut.createDefaultLocalizationForAccount("acc-1", "system:invite"))
                .isInstanceOf(AccountLocalizationException.class)
                .extracting("error")
                .isEqualTo(AccountLocalizationError.SYSTEM_LOCALIZATION_NOT_CONFIGURED);
    }

    @Test
    @DisplayName("createDefaultLocalizationForAccount: システムTZのdefaultがnullの場合は例外")
    void createDefaultLocalizationForAccount_throwsWhenDefaultTimezoneIsNull() {
        TimezoneSetting brokenTz =
                new TimezoneSetting(null, List.of(SupportedTimezone.of("Asia/Tokyo")));
        when(localizationSettingService.getLanguageSetting())
                .thenReturn(Optional.of(languageSettingJaEn()));
        when(localizationSettingService.getTimezoneSetting())
                .thenReturn(Optional.of(brokenTz));

        assertThatThrownBy(() ->
                sut.createDefaultLocalizationForAccount("acc-1", "system:invite"))
                .isInstanceOf(AccountLocalizationException.class)
                .extracting("error")
                .isEqualTo(AccountLocalizationError.SYSTEM_LOCALIZATION_NOT_CONFIGURED);
    }

    @Test
    @DisplayName("saveAccountLocalization: AVAILABLE範囲内の値であれば保存できる")
    void saveAccountLocalization_savesWhenInAvailableRange() {
        when(localizationSettingService.getLanguageSetting())
                .thenReturn(Optional.of(languageSettingJaEn()));
        when(localizationSettingService.getTimezoneSetting())
                .thenReturn(Optional.of(timezoneSettingTokyoNyc()));

        sut.saveAccountLocalization(
                "acc-1",
                SupportedLanguage.EN,
                SupportedTimezone.of("America/New_York"),
                "acc-1");

        verify(accountLocalizationService).saveLocalization(
                "acc-1",
                SupportedLanguage.EN,
                SupportedTimezone.of("America/New_York"),
                "acc-1");
    }

    @Test
    @DisplayName("saveAccountLocalization: languageがnullの場合はLANGUAGE_REQUIRED例外")
    void saveAccountLocalization_throwsWhenLanguageIsNull() {
        assertThatThrownBy(() -> sut.saveAccountLocalization(
                "acc-1", null, SupportedTimezone.of("Asia/Tokyo"), "acc-1"))
                .isInstanceOf(AccountLocalizationException.class)
                .extracting("error")
                .isEqualTo(AccountLocalizationError.LANGUAGE_REQUIRED);

        verify(accountLocalizationService, never()).saveLocalization(
                any(), any(), any(), any());
    }

    @Test
    @DisplayName("saveAccountLocalization: timezoneがnullの場合はTIMEZONE_REQUIRED例外")
    void saveAccountLocalization_throwsWhenTimezoneIsNull() {
        assertThatThrownBy(() -> sut.saveAccountLocalization(
                "acc-1", SupportedLanguage.JA, null, "acc-1"))
                .isInstanceOf(AccountLocalizationException.class)
                .extracting("error")
                .isEqualTo(AccountLocalizationError.TIMEZONE_REQUIRED);

        verify(accountLocalizationService, never()).saveLocalization(
                any(), any(), any(), any());
    }

    @Test
    @DisplayName("saveAccountLocalization: システム側設定が未登録の場合はSYSTEM_LOCALIZATION_NOT_CONFIGURED例外")
    void saveAccountLocalization_throwsWhenSystemNotConfigured() {
        when(localizationSettingService.getLanguageSetting()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.saveAccountLocalization(
                "acc-1",
                SupportedLanguage.JA, SupportedTimezone.of("Asia/Tokyo"), "acc-1"))
                .isInstanceOf(AccountLocalizationException.class)
                .extracting("error")
                .isEqualTo(AccountLocalizationError.SYSTEM_LOCALIZATION_NOT_CONFIGURED);

        verify(accountLocalizationService, never()).saveLocalization(
                any(), any(), any(), any());
    }

    @Test
    @DisplayName("saveAccountLocalization: 言語がAVAILABLE範囲外の場合はLANGUAGE_NOT_AVAILABLE例外")
    void saveAccountLocalization_throwsWhenLanguageNotAvailable() {
        // システム側AVAILABLEはJAのみ。ENを指定する
        LanguageSetting jaOnly = new LanguageSetting(
                SupportedLanguage.JA, List.of(SupportedLanguage.JA));
        when(localizationSettingService.getLanguageSetting())
                .thenReturn(Optional.of(jaOnly));
        when(localizationSettingService.getTimezoneSetting())
                .thenReturn(Optional.of(timezoneSettingTokyoNyc()));

        assertThatThrownBy(() -> sut.saveAccountLocalization(
                "acc-1",
                SupportedLanguage.EN, SupportedTimezone.of("Asia/Tokyo"), "acc-1"))
                .isInstanceOf(AccountLocalizationException.class)
                .extracting("error")
                .isEqualTo(AccountLocalizationError.LANGUAGE_NOT_AVAILABLE);

        verify(accountLocalizationService, never()).saveLocalization(
                any(), any(), any(), any());
    }

    @Test
    @DisplayName("saveAccountLocalization: TZがAVAILABLE範囲外の場合はTIMEZONE_NOT_AVAILABLE例外")
    void saveAccountLocalization_throwsWhenTimezoneNotAvailable() {
        TimezoneSetting tokyoOnly = new TimezoneSetting(
                SupportedTimezone.of("Asia/Tokyo"),
                List.of(SupportedTimezone.of("Asia/Tokyo")));
        when(localizationSettingService.getLanguageSetting())
                .thenReturn(Optional.of(languageSettingJaEn()));
        when(localizationSettingService.getTimezoneSetting())
                .thenReturn(Optional.of(tokyoOnly));

        assertThatThrownBy(() -> sut.saveAccountLocalization(
                "acc-1",
                SupportedLanguage.JA, SupportedTimezone.of("America/New_York"), "acc-1"))
                .isInstanceOf(AccountLocalizationException.class)
                .extracting("error")
                .isEqualTo(AccountLocalizationError.TIMEZONE_NOT_AVAILABLE);

        verify(accountLocalizationService, never()).saveLocalization(
                any(), any(), any(), any());
    }

    @Test
    @DisplayName("countActiveAccountsUsingLanguage: 使用中accountIdが全てACTIVEならその件数を返す")
    void countActiveAccountsUsingLanguage_returnsCount_whenAllActive() {
        when(accountLocalizationService.findAccountIdsUsingLanguage(SupportedLanguage.JA))
                .thenReturn(List.of("acc-1", "acc-2", "acc-3"));
        when(systemAccountStatusRepository.findLatestByAccountId("acc-1"))
                .thenReturn(Optional.of(accountStatus("acc-1", AccountStatus.ACTIVE)));
        when(systemAccountStatusRepository.findLatestByAccountId("acc-2"))
                .thenReturn(Optional.of(accountStatus("acc-2", AccountStatus.ACTIVE)));
        when(systemAccountStatusRepository.findLatestByAccountId("acc-3"))
                .thenReturn(Optional.of(accountStatus("acc-3", AccountStatus.ACTIVE)));

        int count = sut.countActiveAccountsUsingLanguage(SupportedLanguage.JA);

        assertThat(count).isEqualTo(3);
    }

    @Test
    @DisplayName("countActiveAccountsUsingLanguage: ACTIVEとINACTIVE/SUSPENDEDが混在する場合はACTIVEのみカウント")
    void countActiveAccountsUsingLanguage_countsOnlyActive() {
        when(accountLocalizationService.findAccountIdsUsingLanguage(SupportedLanguage.JA))
                .thenReturn(List.of("acc-active", "acc-inactive", "acc-suspended"));
        when(systemAccountStatusRepository.findLatestByAccountId("acc-active"))
                .thenReturn(Optional.of(accountStatus("acc-active", AccountStatus.ACTIVE)));
        when(systemAccountStatusRepository.findLatestByAccountId("acc-inactive"))
                .thenReturn(Optional.of(accountStatus("acc-inactive", AccountStatus.INACTIVE)));
        when(systemAccountStatusRepository.findLatestByAccountId("acc-suspended"))
                .thenReturn(Optional.of(accountStatus("acc-suspended", AccountStatus.SUSPENDED)));

        int count = sut.countActiveAccountsUsingLanguage(SupportedLanguage.JA);

        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("countActiveAccountsUsingLanguage: statusレコード未登録のaccountIdは非ACTIVE扱い（カウントしない）")
    void countActiveAccountsUsingLanguage_treatsMissingStatusAsInactive() {
        when(accountLocalizationService.findAccountIdsUsingLanguage(SupportedLanguage.JA))
                .thenReturn(List.of("acc-active", "acc-orphan"));
        when(systemAccountStatusRepository.findLatestByAccountId("acc-active"))
                .thenReturn(Optional.of(accountStatus("acc-active", AccountStatus.ACTIVE)));
        when(systemAccountStatusRepository.findLatestByAccountId("acc-orphan"))
                .thenReturn(Optional.empty());

        int count = sut.countActiveAccountsUsingLanguage(SupportedLanguage.JA);

        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("countActiveAccountsUsingLanguage: 使用中のaccountIdが空の場合は0を返す")
    void countActiveAccountsUsingLanguage_returnsZero_whenNoAccountUses() {
        when(accountLocalizationService.findAccountIdsUsingLanguage(SupportedLanguage.JA))
                .thenReturn(List.of());

        int count = sut.countActiveAccountsUsingLanguage(SupportedLanguage.JA);

        assertThat(count).isEqualTo(0);
        verify(systemAccountStatusRepository, never()).findLatestByAccountId(any());
    }

    @Test
    @DisplayName("countActiveAccountsUsingTimezone: ACTIVEなアカウントのみカウントする")
    void countActiveAccountsUsingTimezone_countsOnlyActive() {
        SupportedTimezone tokyo = SupportedTimezone.of("Asia/Tokyo");
        when(accountLocalizationService.findAccountIdsUsingTimezone(tokyo))
                .thenReturn(List.of("acc-active", "acc-inactive"));
        when(systemAccountStatusRepository.findLatestByAccountId("acc-active"))
                .thenReturn(Optional.of(accountStatus("acc-active", AccountStatus.ACTIVE)));
        when(systemAccountStatusRepository.findLatestByAccountId("acc-inactive"))
                .thenReturn(Optional.of(accountStatus("acc-inactive", AccountStatus.INACTIVE)));

        int count = sut.countActiveAccountsUsingTimezone(tokyo);

        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("resolveEffectiveLanguage: アカウント設定がAVAILABLE範囲内ならアカウント値を返す")
    void resolveEffectiveLanguage_returnsAccountValue_whenInRange() {
        when(localizationSettingService.getLanguageSetting())
                .thenReturn(Optional.of(languageSettingJaEn()));
        when(accountLocalizationService.getLocalization("acc-1"))
                .thenReturn(Optional.of(new SystemAccountLocalization(
                        "acc-1", BASE_TIME,
                        SupportedLanguage.EN, SupportedTimezone.of("Asia/Tokyo"),
                        BASE_TIME, "acc-1")));

        Optional<SupportedLanguage> result = sut.resolveEffectiveLanguage("acc-1");

        assertThat(result).contains(SupportedLanguage.EN);
    }

    @Test
    @DisplayName("resolveEffectiveLanguage: アカウント設定が範囲外ならシステムデフォルトを返す")
    void resolveEffectiveLanguage_returnsDefault_whenOutOfRange() {
        // システム側AVAILABLEはJAのみ。アカウントENはoutOfRange
        LanguageSetting jaOnly = new LanguageSetting(
                SupportedLanguage.JA, List.of(SupportedLanguage.JA));
        when(localizationSettingService.getLanguageSetting())
                .thenReturn(Optional.of(jaOnly));
        when(accountLocalizationService.getLocalization("acc-1"))
                .thenReturn(Optional.of(new SystemAccountLocalization(
                        "acc-1", BASE_TIME,
                        SupportedLanguage.EN, SupportedTimezone.of("Asia/Tokyo"),
                        BASE_TIME, "acc-1")));

        Optional<SupportedLanguage> result = sut.resolveEffectiveLanguage("acc-1");

        assertThat(result).contains(SupportedLanguage.JA);
    }

    @Test
    @DisplayName("resolveEffectiveLanguage: アカウント設定未登録ならシステムデフォルトを返す")
    void resolveEffectiveLanguage_returnsDefault_whenAccountNotConfigured() {
        when(localizationSettingService.getLanguageSetting())
                .thenReturn(Optional.of(languageSettingJaEn()));
        when(accountLocalizationService.getLocalization("acc-1"))
                .thenReturn(Optional.empty());

        Optional<SupportedLanguage> result = sut.resolveEffectiveLanguage("acc-1");

        assertThat(result).contains(SupportedLanguage.JA);
    }

    @Test
    @DisplayName("resolveEffectiveLanguage: システム側設定も未登録なら空Optional")
    void resolveEffectiveLanguage_returnsEmpty_whenSystemNotConfigured() {
        when(localizationSettingService.getLanguageSetting()).thenReturn(Optional.empty());
        when(accountLocalizationService.getLocalization("acc-1"))
                .thenReturn(Optional.empty());

        Optional<SupportedLanguage> result = sut.resolveEffectiveLanguage("acc-1");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("resolveEffectiveLanguage: システム側設定取得がデシリアライズ失敗なら空Optional（フォールバック）")
    void resolveEffectiveLanguage_returnsEmpty_whenSystemDeserializationFails() {
        when(localizationSettingService.getLanguageSetting())
                .thenThrow(new LocalizationConfigException(
                        LocalizationConfigError.DESERIALIZATION_FAILED));
        when(accountLocalizationService.getLocalization("acc-1"))
                .thenReturn(Optional.empty());

        Optional<SupportedLanguage> result = sut.resolveEffectiveLanguage("acc-1");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("resolveEffectiveLanguage: アカウント設定取得がIllegalStateException（保存値破損）ならフォールバック")
    void resolveEffectiveLanguage_fallsBackToDefault_whenAccountDataCorrupted() {
        when(localizationSettingService.getLanguageSetting())
                .thenReturn(Optional.of(languageSettingJaEn()));
        when(accountLocalizationService.getLocalization("acc-1"))
                .thenThrow(new IllegalStateException("Unknown language code: xx"));

        Optional<SupportedLanguage> result = sut.resolveEffectiveLanguage("acc-1");

        // アカウント側破損 → システムデフォルトへフォールバック
        assertThat(result).contains(SupportedLanguage.JA);
    }

    @Test
    @DisplayName("resolveEffectiveTimezone: アカウント設定がAVAILABLE範囲内ならアカウント値を返す")
    void resolveEffectiveTimezone_returnsAccountValue_whenInRange() {
        when(localizationSettingService.getTimezoneSetting())
                .thenReturn(Optional.of(timezoneSettingTokyoNyc()));
        when(accountLocalizationService.getLocalization("acc-1"))
                .thenReturn(Optional.of(new SystemAccountLocalization(
                        "acc-1", BASE_TIME,
                        SupportedLanguage.JA, SupportedTimezone.of("America/New_York"),
                        BASE_TIME, "acc-1")));

        Optional<SupportedTimezone> result = sut.resolveEffectiveTimezone("acc-1");

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo("America/New_York");
    }

    @Test
    @DisplayName("resolveEffectiveTimezone: アカウント設定が範囲外ならシステムデフォルトを返す")
    void resolveEffectiveTimezone_returnsDefault_whenOutOfRange() {
        TimezoneSetting tokyoOnly = new TimezoneSetting(
                SupportedTimezone.of("Asia/Tokyo"),
                List.of(SupportedTimezone.of("Asia/Tokyo")));
        when(localizationSettingService.getTimezoneSetting())
                .thenReturn(Optional.of(tokyoOnly));
        when(accountLocalizationService.getLocalization("acc-1"))
                .thenReturn(Optional.of(new SystemAccountLocalization(
                        "acc-1", BASE_TIME,
                        SupportedLanguage.JA, SupportedTimezone.of("America/New_York"),
                        BASE_TIME, "acc-1")));

        Optional<SupportedTimezone> result = sut.resolveEffectiveTimezone("acc-1");

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo("Asia/Tokyo");
    }

    @Test
    @DisplayName("resolveEffectiveTimezone: システム側設定も未登録なら空Optional")
    void resolveEffectiveTimezone_returnsEmpty_whenSystemNotConfigured() {
        when(localizationSettingService.getTimezoneSetting()).thenReturn(Optional.empty());
        when(accountLocalizationService.getLocalization("acc-1"))
                .thenReturn(Optional.empty());

        Optional<SupportedTimezone> result = sut.resolveEffectiveTimezone("acc-1");

        assertThat(result).isEmpty();
    }

}
