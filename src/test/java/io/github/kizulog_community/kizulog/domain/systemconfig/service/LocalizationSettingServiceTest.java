package io.github.kizulog_community.kizulog.domain.systemconfig.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.kizulog_community.kizulog.domain.shared.SupportedLanguage;
import io.github.kizulog_community.kizulog.domain.shared.SupportedTimezone;
import io.github.kizulog_community.kizulog.domain.systemconfig.exception.LocalizationConfigError;
import io.github.kizulog_community.kizulog.domain.systemconfig.exception.LocalizationConfigException;
import io.github.kizulog_community.kizulog.domain.systemconfig.model.LanguageSetting;
import io.github.kizulog_community.kizulog.domain.systemconfig.model.SystemConfig;
import io.github.kizulog_community.kizulog.domain.systemconfig.model.TimezoneSetting;
import io.github.kizulog_community.kizulog.domain.systemconfig.port.SystemConfigRepository;

/**
 * LocalizationSettingServiceの単体テスト
 *
 * @author Jun Kobayashi
 */
class LocalizationSettingServiceTest {

    private static final OffsetDateTime BASE_TIME =
            OffsetDateTime.of(2026, 5, 1, 12, 0, 0, 0, ZoneOffset.UTC);

    private SystemConfigRepository repository;
    private ObjectMapper objectMapper;
    private LocalizationSettingService service;

    @BeforeEach
    void setUp() {
        repository = mock(SystemConfigRepository.class);
        objectMapper = new ObjectMapper();
        service = new LocalizationSettingService(repository, objectMapper);
    }

    @Test
    @DisplayName("getLanguageSetting: DBに未保存の場合は空のOptionalを返す")
    void getLanguageSetting_returnsEmpty_whenNotConfigured() {
        when(repository.findLatestByKey("LANGUAGE")).thenReturn(Optional.empty());

        Optional<LanguageSetting> result = service.getLanguageSetting();

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getLanguageSetting: DBに保存済みの場合は値を返す")
    void getLanguageSetting_returnsValue_whenConfigured() {
        SystemConfig stored = new SystemConfig(
                "LANGUAGE", BASE_TIME,
                "{\"DEFAULT\":\"ja\",\"AVAILABLE\":[\"ja\",\"en\"]}",
                BASE_TIME, "system:setup-wizard");
        when(repository.findLatestByKey("LANGUAGE")).thenReturn(Optional.of(stored));

        Optional<LanguageSetting> result = service.getLanguageSetting();

        assertThat(result).isPresent();
        assertThat(result.get().getDefaultLanguage()).isEqualTo(SupportedLanguage.JA);
        assertThat(result.get().getAvailableLanguages())
                .containsExactly(SupportedLanguage.JA, SupportedLanguage.EN);
    }

    @Test
    @DisplayName("getLanguageSetting: JSON破損の場合はDESERIALIZATION_FAILED例外")
    void getLanguageSetting_throws_whenJsonBroken() {
        SystemConfig stored = new SystemConfig(
                "LANGUAGE", BASE_TIME, "{INVALID JSON",
                BASE_TIME, "system:setup-wizard");
        when(repository.findLatestByKey("LANGUAGE")).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> service.getLanguageSetting())
                .isInstanceOf(LocalizationConfigException.class)
                .extracting("error")
                .isEqualTo(LocalizationConfigError.DESERIALIZATION_FAILED);
    }

    @Test
    @DisplayName("getLanguageSetting: 未知の言語コードは静かに無視される")
    void getLanguageSetting_unknownCodesAreSilentlyIgnored() {
        SystemConfig stored = new SystemConfig(
                "LANGUAGE", BASE_TIME,
                "{\"DEFAULT\":\"ja\",\"AVAILABLE\":[\"ja\",\"en\",\"xx\"]}",
                BASE_TIME, "system:setup-wizard");
        when(repository.findLatestByKey("LANGUAGE")).thenReturn(Optional.of(stored));

        Optional<LanguageSetting> result = service.getLanguageSetting();

        assertThat(result).isPresent();
        assertThat(result.get().getAvailableLanguages())
                .containsExactly(SupportedLanguage.JA, SupportedLanguage.EN);
    }

    @Test
    @DisplayName("getLanguageSetting: DEFAULTがnullの場合もデシリアライズできる")
    void getLanguageSetting_nullDefault_returnsNullDefaultLanguage() {
        SystemConfig stored = new SystemConfig(
                "LANGUAGE", BASE_TIME,
                "{\"AVAILABLE\":[\"ja\"]}",
                BASE_TIME, "system:setup-wizard");
        when(repository.findLatestByKey("LANGUAGE")).thenReturn(Optional.of(stored));

        Optional<LanguageSetting> result = service.getLanguageSetting();

        assertThat(result).isPresent();
        assertThat(result.get().getDefaultLanguage()).isNull();
        assertThat(result.get().getAvailableLanguages()).containsExactly(SupportedLanguage.JA);
    }

    @Test
    @DisplayName("getLanguageSetting: AVAILABLEがnullの場合は空リストになる")
    void getLanguageSetting_nullAvailable_returnsEmptyList() {
        SystemConfig stored = new SystemConfig(
                "LANGUAGE", BASE_TIME,
                "{\"DEFAULT\":\"ja\"}",
                BASE_TIME, "system:setup-wizard");
        when(repository.findLatestByKey("LANGUAGE")).thenReturn(Optional.of(stored));

        Optional<LanguageSetting> result = service.getLanguageSetting();

        assertThat(result).isPresent();
        assertThat(result.get().getAvailableLanguages()).isEmpty();
    }

    @Test
    @DisplayName("getTimezoneSetting: DBに未保存の場合は空のOptionalを返す")
    void getTimezoneSetting_returnsEmpty_whenNotConfigured() {
        when(repository.findLatestByKey("TIMEZONE")).thenReturn(Optional.empty());

        Optional<TimezoneSetting> result = service.getTimezoneSetting();

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getTimezoneSetting: DBに保存済みの場合は値を返す")
    void getTimezoneSetting_returnsValue_whenConfigured() {
        SystemConfig stored = new SystemConfig(
                "TIMEZONE", BASE_TIME,
                "{\"DEFAULT\":\"Asia/Tokyo\",\"AVAILABLE\":[\"Asia/Tokyo\",\"UTC\"]}",
                BASE_TIME, "system:setup-wizard");
        when(repository.findLatestByKey("TIMEZONE")).thenReturn(Optional.of(stored));

        Optional<TimezoneSetting> result = service.getTimezoneSetting();

        assertThat(result).isPresent();
        assertThat(result.get().getDefaultTimezone().getId()).isEqualTo("Asia/Tokyo");
        assertThat(result.get().getAvailableTimezones())
                .extracting(SupportedTimezone::getId)
                .containsExactly("Asia/Tokyo", "UTC");
    }

    @Test
    @DisplayName("getTimezoneSetting: JSON破損の場合はDESERIALIZATION_FAILED例外")
    void getTimezoneSetting_throws_whenJsonBroken() {
        SystemConfig stored = new SystemConfig(
                "TIMEZONE", BASE_TIME, "{INVALID JSON",
                BASE_TIME, "system:setup-wizard");
        when(repository.findLatestByKey("TIMEZONE")).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> service.getTimezoneSetting())
                .isInstanceOf(LocalizationConfigException.class)
                .extracting("error")
                .isEqualTo(LocalizationConfigError.DESERIALIZATION_FAILED);
    }

    @Test
    @DisplayName("getTimezoneSetting: 未知のタイムゾーンIDは静かに無視される")
    void getTimezoneSetting_unknownIdsAreSilentlyIgnored() {
        SystemConfig stored = new SystemConfig(
                "TIMEZONE", BASE_TIME,
                "{\"DEFAULT\":\"Asia/Tokyo\",\"AVAILABLE\":[\"Asia/Tokyo\",\"Mars/Olympus\"]}",
                BASE_TIME, "system:setup-wizard");
        when(repository.findLatestByKey("TIMEZONE")).thenReturn(Optional.of(stored));

        Optional<TimezoneSetting> result = service.getTimezoneSetting();

        assertThat(result).isPresent();
        assertThat(result.get().getAvailableTimezones())
                .extracting(SupportedTimezone::getId)
                .containsExactly("Asia/Tokyo");
    }

    @Test
    @DisplayName("getTimezoneSetting: DEFAULTがnullの場合もデシリアライズできる")
    void getTimezoneSetting_nullDefault_returnsNullDefaultTimezone() {
        SystemConfig stored = new SystemConfig(
                "TIMEZONE", BASE_TIME,
                "{\"AVAILABLE\":[\"Asia/Tokyo\"]}",
                BASE_TIME, "system:setup-wizard");
        when(repository.findLatestByKey("TIMEZONE")).thenReturn(Optional.of(stored));

        Optional<TimezoneSetting> result = service.getTimezoneSetting();

        assertThat(result).isPresent();
        assertThat(result.get().getDefaultTimezone()).isNull();
    }

    @Test
    @DisplayName("getTimezoneSetting: AVAILABLEがnullの場合は空リストになる")
    void getTimezoneSetting_nullAvailable_returnsEmptyList() {
        SystemConfig stored = new SystemConfig(
                "TIMEZONE", BASE_TIME,
                "{\"DEFAULT\":\"Asia/Tokyo\"}",
                BASE_TIME, "system:setup-wizard");
        when(repository.findLatestByKey("TIMEZONE")).thenReturn(Optional.of(stored));

        Optional<TimezoneSetting> result = service.getTimezoneSetting();

        assertThat(result).isPresent();
        assertThat(result.get().getAvailableTimezones()).isEmpty();
    }

    @Test
    @DisplayName("saveLanguageSetting: 正常系で1件SystemConfigが保存される")
    void saveLanguageSetting_savesOneSystemConfig() {
        LanguageSetting setting = new LanguageSetting(
                SupportedLanguage.JA,
                List.of(SupportedLanguage.JA, SupportedLanguage.EN));

        service.saveLanguageSetting(setting, "user:admin");

        ArgumentCaptor<SystemConfig> captor =
                ArgumentCaptor.forClass(SystemConfig.class);
        verify(repository, times(1)).save(captor.capture());

        SystemConfig saved = captor.getValue();
        assertThat(saved.getKey()).isEqualTo("LANGUAGE");
        assertThat(saved.getValue()).contains("\"DEFAULT\":\"ja\"");
        assertThat(saved.getValue()).contains("\"AVAILABLE\":[\"ja\",\"en\"]");
        assertThat(saved.getCreatedBy()).isEqualTo("user:admin");
    }

    @Test
    @DisplayName("saveLanguageSetting: createdAtとversionが同一")
    void saveLanguageSetting_versionAndCreatedAtAreSame() {
        LanguageSetting setting = new LanguageSetting(
                SupportedLanguage.JA, List.of(SupportedLanguage.JA));

        service.saveLanguageSetting(setting, "user:admin");

        ArgumentCaptor<SystemConfig> captor =
                ArgumentCaptor.forClass(SystemConfig.class);
        verify(repository).save(captor.capture());

        SystemConfig saved = captor.getValue();
        assertThat(saved.getVersion()).isEqualTo(saved.getCreatedAt());
    }

    @Test
    @DisplayName("saveLanguageSetting: defaultLanguageがnullなら DEFAULT_LANGUAGE_REQUIRED")
    void saveLanguageSetting_throws_whenDefaultLanguageNull() {
        LanguageSetting setting = new LanguageSetting(
                null, List.of(SupportedLanguage.JA));

        assertThatThrownBy(() -> service.saveLanguageSetting(setting, "user:admin"))
                .isInstanceOf(LocalizationConfigException.class)
                .extracting("error")
                .isEqualTo(LocalizationConfigError.DEFAULT_LANGUAGE_REQUIRED);

        verify(repository, never()).save(any(SystemConfig.class));
    }

    @Test
    @DisplayName("saveLanguageSetting: availableLanguagesが空なら AVAILABLE_LANGUAGES_EMPTY")
    void saveLanguageSetting_throws_whenAvailableLanguagesEmpty() {
        LanguageSetting setting = new LanguageSetting(
                SupportedLanguage.JA, List.of());

        assertThatThrownBy(() -> service.saveLanguageSetting(setting, "user:admin"))
                .isInstanceOf(LocalizationConfigException.class)
                .extracting("error")
                .isEqualTo(LocalizationConfigError.AVAILABLE_LANGUAGES_EMPTY);

        verify(repository, never()).save(any(SystemConfig.class));
    }

    @Test
    @DisplayName("saveLanguageSetting: availableLanguagesがnullなら AVAILABLE_LANGUAGES_EMPTY")
    void saveLanguageSetting_throws_whenAvailableLanguagesNull() {
        LanguageSetting setting = new LanguageSetting(SupportedLanguage.JA, null);

        assertThatThrownBy(() -> service.saveLanguageSetting(setting, "user:admin"))
                .isInstanceOf(LocalizationConfigException.class)
                .extracting("error")
                .isEqualTo(LocalizationConfigError.AVAILABLE_LANGUAGES_EMPTY);

        verify(repository, never()).save(any(SystemConfig.class));
    }

    @Test
    @DisplayName("saveLanguageSetting: defaultが利用可能に含まれない場合 DEFAULT_LANGUAGE_NOT_IN_AVAILABLE")
    void saveLanguageSetting_throws_whenDefaultNotInAvailable() {
        LanguageSetting setting = new LanguageSetting(
                SupportedLanguage.JA, List.of(SupportedLanguage.EN));

        assertThatThrownBy(() -> service.saveLanguageSetting(setting, "user:admin"))
                .isInstanceOf(LocalizationConfigException.class)
                .extracting("error")
                .isEqualTo(LocalizationConfigError.DEFAULT_LANGUAGE_NOT_IN_AVAILABLE);

        verify(repository, never()).save(any(SystemConfig.class));
    }

    @Test
    @DisplayName("saveLanguageSetting: JSON変換失敗時は SERIALIZATION_FAILED")
    void saveLanguageSetting_throws_whenSerializationFails() throws Exception {
        ObjectMapper failingMapper = mock(ObjectMapper.class);
        when(failingMapper.writeValueAsString(any()))
                .thenThrow(new JsonProcessingException("test") {
                    private static final long serialVersionUID = 1L;
                });
        LocalizationSettingService failingService =
                new LocalizationSettingService(repository, failingMapper);

        LanguageSetting setting = new LanguageSetting(
                SupportedLanguage.JA, List.of(SupportedLanguage.JA));

        assertThatThrownBy(() -> failingService.saveLanguageSetting(setting, "user:admin"))
                .isInstanceOf(LocalizationConfigException.class)
                .extracting("error")
                .isEqualTo(LocalizationConfigError.SERIALIZATION_FAILED);
    }

    @Test
    @DisplayName("saveTimezoneSetting: 正常系で1件SystemConfigが保存される")
    void saveTimezoneSetting_savesOneSystemConfig() {
        TimezoneSetting setting = new TimezoneSetting(
                SupportedTimezone.of("Asia/Tokyo"),
                List.of(SupportedTimezone.of("Asia/Tokyo"),
                        SupportedTimezone.of("UTC")));

        service.saveTimezoneSetting(setting, "user:admin");

        ArgumentCaptor<SystemConfig> captor =
                ArgumentCaptor.forClass(SystemConfig.class);
        verify(repository, times(1)).save(captor.capture());

        SystemConfig saved = captor.getValue();
        assertThat(saved.getKey()).isEqualTo("TIMEZONE");
        assertThat(saved.getValue()).contains("\"DEFAULT\":\"Asia/Tokyo\"");
        assertThat(saved.getValue()).contains("\"AVAILABLE\":[\"Asia/Tokyo\",\"UTC\"]");
        assertThat(saved.getCreatedBy()).isEqualTo("user:admin");
    }

    @Test
    @DisplayName("saveTimezoneSetting: createdAtとversionが同一")
    void saveTimezoneSetting_versionAndCreatedAtAreSame() {
        TimezoneSetting setting = new TimezoneSetting(
                SupportedTimezone.of("UTC"),
                List.of(SupportedTimezone.of("UTC")));

        service.saveTimezoneSetting(setting, "user:admin");

        ArgumentCaptor<SystemConfig> captor =
                ArgumentCaptor.forClass(SystemConfig.class);
        verify(repository).save(captor.capture());

        SystemConfig saved = captor.getValue();
        assertThat(saved.getVersion()).isEqualTo(saved.getCreatedAt());
    }

    @Test
    @DisplayName("saveTimezoneSetting: defaultTimezoneがnullなら DEFAULT_TIMEZONE_REQUIRED")
    void saveTimezoneSetting_throws_whenDefaultTimezoneNull() {
        TimezoneSetting setting = new TimezoneSetting(
                null, List.of(SupportedTimezone.of("UTC")));

        assertThatThrownBy(() -> service.saveTimezoneSetting(setting, "user:admin"))
                .isInstanceOf(LocalizationConfigException.class)
                .extracting("error")
                .isEqualTo(LocalizationConfigError.DEFAULT_TIMEZONE_REQUIRED);

        verify(repository, never()).save(any(SystemConfig.class));
    }

    @Test
    @DisplayName("saveTimezoneSetting: availableTimezonesが空なら AVAILABLE_TIMEZONES_EMPTY")
    void saveTimezoneSetting_throws_whenAvailableTimezonesEmpty() {
        TimezoneSetting setting = new TimezoneSetting(
                SupportedTimezone.of("Asia/Tokyo"), List.of());

        assertThatThrownBy(() -> service.saveTimezoneSetting(setting, "user:admin"))
                .isInstanceOf(LocalizationConfigException.class)
                .extracting("error")
                .isEqualTo(LocalizationConfigError.AVAILABLE_TIMEZONES_EMPTY);

        verify(repository, never()).save(any(SystemConfig.class));
    }

    @Test
    @DisplayName("saveTimezoneSetting: availableTimezonesがnullなら AVAILABLE_TIMEZONES_EMPTY")
    void saveTimezoneSetting_throws_whenAvailableTimezonesNull() {
        TimezoneSetting setting = new TimezoneSetting(
                SupportedTimezone.of("Asia/Tokyo"), null);

        assertThatThrownBy(() -> service.saveTimezoneSetting(setting, "user:admin"))
                .isInstanceOf(LocalizationConfigException.class)
                .extracting("error")
                .isEqualTo(LocalizationConfigError.AVAILABLE_TIMEZONES_EMPTY);

        verify(repository, never()).save(any(SystemConfig.class));
    }

    @Test
    @DisplayName("saveTimezoneSetting: defaultが利用可能に含まれない場合 DEFAULT_TIMEZONE_NOT_IN_AVAILABLE")
    void saveTimezoneSetting_throws_whenDefaultNotInAvailable() {
        TimezoneSetting setting = new TimezoneSetting(
                SupportedTimezone.of("Asia/Tokyo"),
                List.of(SupportedTimezone.of("UTC")));

        assertThatThrownBy(() -> service.saveTimezoneSetting(setting, "user:admin"))
                .isInstanceOf(LocalizationConfigException.class)
                .extracting("error")
                .isEqualTo(LocalizationConfigError.DEFAULT_TIMEZONE_NOT_IN_AVAILABLE);

        verify(repository, never()).save(any(SystemConfig.class));
    }

    @Test
    @DisplayName("saveTimezoneSetting: JSON変換失敗時は SERIALIZATION_FAILED")
    void saveTimezoneSetting_throws_whenSerializationFails() throws Exception {
        ObjectMapper failingMapper = mock(ObjectMapper.class);
        when(failingMapper.writeValueAsString(any()))
                .thenThrow(new JsonProcessingException("test") {
                    private static final long serialVersionUID = 1L;
                });
        LocalizationSettingService failingService =
                new LocalizationSettingService(repository, failingMapper);

        TimezoneSetting setting = new TimezoneSetting(
                SupportedTimezone.of("UTC"),
                List.of(SupportedTimezone.of("UTC")));

        assertThatThrownBy(() -> failingService.saveTimezoneSetting(setting, "user:admin"))
                .isInstanceOf(LocalizationConfigException.class)
                .extracting("error")
                .isEqualTo(LocalizationConfigError.SERIALIZATION_FAILED);
    }

    @Test
    @DisplayName("saveBoth: 正常系で2件SystemConfigが保存される")
    void saveBoth_savesTwoSystemConfigs() {
        LanguageSetting lang = new LanguageSetting(
                SupportedLanguage.JA, List.of(SupportedLanguage.JA));
        TimezoneSetting tz = new TimezoneSetting(
                SupportedTimezone.of("Asia/Tokyo"),
                List.of(SupportedTimezone.of("Asia/Tokyo")));

        service.saveBoth(lang, tz, BASE_TIME, "system:setup-wizard");

        verify(repository, times(2)).save(any(SystemConfig.class));
    }

    @Test
    @DisplayName("saveBoth: 渡したversionが両方のSystemConfigに使用される")
    void saveBoth_usesProvidedVersionForBoth() {
        LanguageSetting lang = new LanguageSetting(
                SupportedLanguage.JA, List.of(SupportedLanguage.JA));
        TimezoneSetting tz = new TimezoneSetting(
                SupportedTimezone.of("Asia/Tokyo"),
                List.of(SupportedTimezone.of("Asia/Tokyo")));

        service.saveBoth(lang, tz, BASE_TIME, "system:setup-wizard");

        ArgumentCaptor<SystemConfig> captor =
                ArgumentCaptor.forClass(SystemConfig.class);
        verify(repository, times(2)).save(captor.capture());

        assertThat(captor.getAllValues())
                .extracting(SystemConfig::getVersion)
                .containsExactly(BASE_TIME, BASE_TIME);
    }

    @Test
    @DisplayName("saveBoth: 言語バリデーション失敗時はどちらも保存されない")
    void saveBoth_throwsBeforeAnySave_whenLanguageInvalid() {
        LanguageSetting invalidLang = new LanguageSetting(null, List.of(SupportedLanguage.JA));
        TimezoneSetting tz = new TimezoneSetting(
                SupportedTimezone.of("Asia/Tokyo"),
                List.of(SupportedTimezone.of("Asia/Tokyo")));

        assertThatThrownBy(() ->
                service.saveBoth(invalidLang, tz, BASE_TIME, "system:setup-wizard"))
                .isInstanceOf(LocalizationConfigException.class)
                .extracting("error")
                .isEqualTo(LocalizationConfigError.DEFAULT_LANGUAGE_REQUIRED);

        verify(repository, never()).save(any(SystemConfig.class));
    }

    @Test
    @DisplayName("saveBoth: タイムゾーンバリデーション失敗時はどちらも保存されない")
    void saveBoth_throwsBeforeAnySave_whenTimezoneInvalid() {
        LanguageSetting lang = new LanguageSetting(
                SupportedLanguage.JA, List.of(SupportedLanguage.JA));
        TimezoneSetting invalidTz = new TimezoneSetting(
                null, List.of(SupportedTimezone.of("Asia/Tokyo")));

        assertThatThrownBy(() ->
                service.saveBoth(lang, invalidTz, BASE_TIME, "system:setup-wizard"))
                .isInstanceOf(LocalizationConfigException.class)
                .extracting("error")
                .isEqualTo(LocalizationConfigError.DEFAULT_TIMEZONE_REQUIRED);

        verify(repository, never()).save(any(SystemConfig.class));
    }

    @Test
    @DisplayName("saveBoth: 言語保存後にRepository例外が発生した場合は伝播する")
    void saveBoth_propagatesException_fromRepositoryAfterLanguageSaved() {
        LanguageSetting lang = new LanguageSetting(
                SupportedLanguage.JA, List.of(SupportedLanguage.JA));
        TimezoneSetting tz = new TimezoneSetting(
                SupportedTimezone.of("Asia/Tokyo"),
                List.of(SupportedTimezone.of("Asia/Tokyo")));

        doThrow(new RuntimeException("DB error"))
                .when(repository).save(any(SystemConfig.class));

        assertThatThrownBy(() ->
                service.saveBoth(lang, tz, BASE_TIME, "system:setup-wizard"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("DB error");
    }

    @Test
    @DisplayName("KEY_LANGUAGEは 'LANGUAGE'")
    void keyLanguage_isLanguage() {
        assertThat(LocalizationSettingService.KEY_LANGUAGE).isEqualTo("LANGUAGE");
    }

    @Test
    @DisplayName("KEY_TIMEZONEは 'TIMEZONE'")
    void keyTimezone_isTimezone() {
        assertThat(LocalizationSettingService.KEY_TIMEZONE).isEqualTo("TIMEZONE");
    }

}
