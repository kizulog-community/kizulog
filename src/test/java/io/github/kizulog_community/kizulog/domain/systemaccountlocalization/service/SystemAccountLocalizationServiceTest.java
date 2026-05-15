package io.github.kizulog_community.kizulog.domain.systemaccountlocalization.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import org.mockito.ArgumentCaptor;

import io.github.kizulog_community.kizulog.domain.shared.SupportedLanguage;
import io.github.kizulog_community.kizulog.domain.shared.SupportedTimezone;
import io.github.kizulog_community.kizulog.domain.systemaccountlocalization.exception.AccountLocalizationError;
import io.github.kizulog_community.kizulog.domain.systemaccountlocalization.exception.AccountLocalizationException;
import io.github.kizulog_community.kizulog.domain.systemaccountlocalization.model.SystemAccountLocalization;
import io.github.kizulog_community.kizulog.domain.systemaccountlocalization.port.SystemAccountLocalizationRepository;

/**
 * SystemAccountLocalizationServiceの単体テスト
 *
 * @author Jun Kobayashi
 */
class SystemAccountLocalizationServiceTest {

    private static final OffsetDateTime BASE_TIME =
            OffsetDateTime.of(2026, 5, 1, 12, 0, 0, 0, ZoneOffset.UTC);

    private SystemAccountLocalizationRepository repository;
    private SystemAccountLocalizationService service;

    @BeforeEach
    void setUp() {
        repository = mock(SystemAccountLocalizationRepository.class);
        service = new SystemAccountLocalizationService(repository);
    }

    @Test
    @DisplayName("getLocalization: アカウントlocalization設定が存在する場合は値を返す")
    void getLocalization_returnsValue_whenExists() {
        SystemAccountLocalization stored = new SystemAccountLocalization(
                "acc-1", BASE_TIME,
                SupportedLanguage.JA, SupportedTimezone.of("Asia/Tokyo"),
                BASE_TIME, "user:1");
        when(repository.findLatestByAccountId("acc-1")).thenReturn(Optional.of(stored));

        Optional<SystemAccountLocalization> result = service.getLocalization("acc-1");

        assertThat(result).isPresent();
        assertThat(result.get().getLanguage()).isEqualTo(SupportedLanguage.JA);
        assertThat(result.get().getTimezone().getId()).isEqualTo("Asia/Tokyo");
    }

    @Test
    @DisplayName("getLocalization: アカウントlocalization設定が未登録の場合は空のOptionalを返す")
    void getLocalization_returnsEmpty_whenNotExists() {
        when(repository.findLatestByAccountId("acc-1")).thenReturn(Optional.empty());

        Optional<SystemAccountLocalization> result = service.getLocalization("acc-1");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("saveLocalization: 正常系では新versionのSystemAccountLocalizationを保存する")
    void saveLocalization_savesNewVersion() {
        // when
        service.saveLocalization(
                "acc-1",
                SupportedLanguage.JA,
                SupportedTimezone.of("Asia/Tokyo"),
                "user:1");

        // then
        ArgumentCaptor<SystemAccountLocalization> captor =
                ArgumentCaptor.forClass(SystemAccountLocalization.class);
        verify(repository).save(captor.capture());

        SystemAccountLocalization saved = captor.getValue();
        assertThat(saved.getAccountId()).isEqualTo("acc-1");
        assertThat(saved.getLanguage()).isEqualTo(SupportedLanguage.JA);
        assertThat(saved.getTimezone().getId()).isEqualTo("Asia/Tokyo");
        assertThat(saved.getCreatedBy()).isEqualTo("user:1");
        // versionとcreatedAtは現在時刻ベース
        assertThat(saved.getVersion()).isNotNull();
        assertThat(saved.getCreatedAt()).isEqualTo(saved.getVersion());
    }

    @Test
    @DisplayName("saveLocalization: languageがnullの場合はLANGUAGE_REQUIRED例外")
    void saveLocalization_throwsLanguageRequired_whenLanguageIsNull() {
        assertThatThrownBy(() -> service.saveLocalization(
                "acc-1", null, SupportedTimezone.of("Asia/Tokyo"), "user:1"))
                .isInstanceOf(AccountLocalizationException.class)
                .extracting("error")
                .isEqualTo(AccountLocalizationError.LANGUAGE_REQUIRED);

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("saveLocalization: timezoneがnullの場合はTIMEZONE_REQUIRED例外")
    void saveLocalization_throwsTimezoneRequired_whenTimezoneIsNull() {
        assertThatThrownBy(() -> service.saveLocalization(
                "acc-1", SupportedLanguage.JA, null, "user:1"))
                .isInstanceOf(AccountLocalizationException.class)
                .extracting("error")
                .isEqualTo(AccountLocalizationError.TIMEZONE_REQUIRED);

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("findAccountIdsUsingLanguage: 該当言語コードを使用中のaccountIdリストを返す")
    void findAccountIdsUsingLanguage_returnsAccountIds() {
        when(repository.findAccountIdsUsingLanguage("ja"))
                .thenReturn(List.of("acc-1", "acc-2"));

        List<String> result = service.findAccountIdsUsingLanguage(SupportedLanguage.JA);

        assertThat(result).containsExactly("acc-1", "acc-2");
        verify(repository).findAccountIdsUsingLanguage("ja");
    }

    @Test
    @DisplayName("findAccountIdsUsingLanguage: 一致なしの場合は空のリストを返す")
    void findAccountIdsUsingLanguage_returnsEmpty_whenNoMatch() {
        when(repository.findAccountIdsUsingLanguage("en")).thenReturn(List.of());

        List<String> result = service.findAccountIdsUsingLanguage(SupportedLanguage.EN);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findAccountIdsUsingLanguage: languageがnullの場合はLANGUAGE_REQUIRED例外")
    void findAccountIdsUsingLanguage_throwsLanguageRequired_whenNull() {
        assertThatThrownBy(() -> service.findAccountIdsUsingLanguage(null))
                .isInstanceOf(AccountLocalizationException.class)
                .extracting("error")
                .isEqualTo(AccountLocalizationError.LANGUAGE_REQUIRED);

        verify(repository, never()).findAccountIdsUsingLanguage(any());
    }

    @Test
    @DisplayName("findAccountIdsUsingTimezone: 該当TZ IDを使用中のaccountIdリストを返す")
    void findAccountIdsUsingTimezone_returnsAccountIds() {
        when(repository.findAccountIdsUsingTimezone("Asia/Tokyo"))
                .thenReturn(List.of("acc-1", "acc-2"));

        List<String> result = service.findAccountIdsUsingTimezone(
                SupportedTimezone.of("Asia/Tokyo"));

        assertThat(result).containsExactly("acc-1", "acc-2");
        verify(repository).findAccountIdsUsingTimezone("Asia/Tokyo");
    }

    @Test
    @DisplayName("findAccountIdsUsingTimezone: 一致なしの場合は空のリストを返す")
    void findAccountIdsUsingTimezone_returnsEmpty_whenNoMatch() {
        when(repository.findAccountIdsUsingTimezone(eq("America/New_York")))
                .thenReturn(List.of());

        List<String> result = service.findAccountIdsUsingTimezone(
                SupportedTimezone.of("America/New_York"));

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findAccountIdsUsingTimezone: timezoneがnullの場合はTIMEZONE_REQUIRED例外")
    void findAccountIdsUsingTimezone_throwsTimezoneRequired_whenNull() {
        assertThatThrownBy(() -> service.findAccountIdsUsingTimezone(null))
                .isInstanceOf(AccountLocalizationException.class)
                .extracting("error")
                .isEqualTo(AccountLocalizationError.TIMEZONE_REQUIRED);

        verify(repository, never()).findAccountIdsUsingTimezone(any());
    }

}
