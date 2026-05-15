package io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccountlocalization.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.zone.ZoneRulesException;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.context.annotation.Import;

import io.github.kizulog_community.kizulog.domain.shared.SupportedLanguage;
import io.github.kizulog_community.kizulog.domain.shared.SupportedTimezone;
import io.github.kizulog_community.kizulog.domain.systemaccountlocalization.model.SystemAccountLocalization;
import io.github.kizulog_community.kizulog.infrastructure.persistence.AbstractRepositoryIT;
import io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccountlocalization.entity.SystemAccountLocalizationEntity;
import io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccountlocalization.entity.SystemAccountLocalizationId;

/**
 * SystemAccountLocalizationRepositoryImpl の統合テスト
 *
 * @author Jun Kobayashi
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import(SystemAccountLocalizationRepositoryImpl.class)
class SystemAccountLocalizationRepositoryImplIT extends AbstractRepositoryIT {

    /** テスト対象 */
    @Autowired
    private SystemAccountLocalizationRepositoryImpl sut;

    /** テストデータ投入用JPAリポジトリ */
    @Autowired
    private SystemAccountLocalizationJpaRepository jpaRepository;

    /** テスト用基準時刻（UTC） */
    private static final OffsetDateTime BASE_TIME =
            OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

    @BeforeEach
    void setUp() {
        jpaRepository.deleteAll();
    }

    @Test
    @DisplayName("findLatestByAccountId: 単一バージョンしか存在しない場合、そのレコードを返す")
    void findLatestByAccountId_returnsSingleVersion_whenOnlyOneExists() {
        // given
        saveEntity("acc-1", BASE_TIME, "ja", "Asia/Tokyo", "system:setup");

        // when
        Optional<SystemAccountLocalization> result = sut.findLatestByAccountId("acc-1");

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getAccountId()).isEqualTo("acc-1");
        assertThat(result.get().getLanguage()).isEqualTo(SupportedLanguage.JA);
        assertThat(result.get().getTimezone().getId()).isEqualTo("Asia/Tokyo");
    }

    @Test
    @DisplayName("findLatestByAccountId: 同一accountIdで複数バージョンが存在する場合、最大versionを返す")
    void findLatestByAccountId_returnsLatestVersion_whenMultipleVersionsExist() {
        // given
        OffsetDateTime v1 = BASE_TIME;
        OffsetDateTime v2 = BASE_TIME.plusHours(1);
        OffsetDateTime v3 = BASE_TIME.plusHours(2);
        saveEntity("acc-1", v1, "ja", "Asia/Tokyo", "user:1");
        saveEntity("acc-1", v3, "en", "America/New_York", "user:3");
        saveEntity("acc-1", v2, "ja", "Europe/London", "user:2");

        // when
        Optional<SystemAccountLocalization> result = sut.findLatestByAccountId("acc-1");

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getVersion()).isEqualTo(v3);
        assertThat(result.get().getLanguage()).isEqualTo(SupportedLanguage.EN);
        assertThat(result.get().getTimezone().getId()).isEqualTo("America/New_York");
        assertThat(result.get().getCreatedBy()).isEqualTo("user:3");
    }

    @Test
    @DisplayName("findLatestByAccountId: 該当accountIdが存在しない場合、空のOptionalを返す")
    void findLatestByAccountId_returnsEmpty_whenAccountDoesNotExist() {
        // given
        saveEntity("acc-1", BASE_TIME, "ja", "Asia/Tokyo", "user:1");

        // when
        Optional<SystemAccountLocalization> result = sut.findLatestByAccountId("unknown-acc");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findLatestByAccountId: テーブルが空の場合、空のOptionalを返す")
    void findLatestByAccountId_returnsEmpty_whenTableIsEmpty() {
        // when
        Optional<SystemAccountLocalization> result = sut.findLatestByAccountId("acc-1");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findLatestByAccountId: 複数accountId混在時、指定accountIdの最新のみ返す")
    void findLatestByAccountId_returnsOnlySpecifiedAccount_whenMultipleAccountsExist() {
        // given
        saveEntity("acc-1", BASE_TIME, "ja", "Asia/Tokyo", "user:1");
        saveEntity("acc-1", BASE_TIME.plusHours(1), "en", "America/New_York", "user:2");
        saveEntity("acc-2", BASE_TIME.plusHours(2), "ja", "Europe/London", "user:3");

        // when
        Optional<SystemAccountLocalization> result = sut.findLatestByAccountId("acc-1");

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getAccountId()).isEqualTo("acc-1");
        assertThat(result.get().getLanguage()).isEqualTo(SupportedLanguage.EN);
    }

    @Test
    @DisplayName("findAccountIdsUsingLanguage: 指定言語を使用中のaccountIdを返す（単一一致）")
    void findAccountIdsUsingLanguage_returnsMatchingAccountId() {
        // given
        saveEntity("acc-1", BASE_TIME, "ja", "Asia/Tokyo", "user:1");
        saveEntity("acc-2", BASE_TIME, "en", "America/New_York", "user:2");

        // when
        List<String> result = sut.findAccountIdsUsingLanguage("ja");

        // then
        assertThat(result).containsExactly("acc-1");
    }

    @Test
    @DisplayName("findAccountIdsUsingLanguage: 指定言語を使用中の複数accountIdを返す")
    void findAccountIdsUsingLanguage_returnsMultipleMatchingAccounts() {
        // given
        saveEntity("acc-1", BASE_TIME, "ja", "Asia/Tokyo", "user:1");
        saveEntity("acc-2", BASE_TIME, "ja", "Europe/London", "user:2");
        saveEntity("acc-3", BASE_TIME, "en", "America/New_York", "user:3");

        // when
        List<String> result = sut.findAccountIdsUsingLanguage("ja");

        // then
        assertThat(result).containsExactlyInAnyOrder("acc-1", "acc-2");
    }

    @Test
    @DisplayName("findAccountIdsUsingLanguage: 該当言語を使用中のアカウントが存在しない場合、空のリストを返す")
    void findAccountIdsUsingLanguage_returnsEmpty_whenNoMatchingAccount() {
        // given
        saveEntity("acc-1", BASE_TIME, "ja", "Asia/Tokyo", "user:1");

        // when
        List<String> result = sut.findAccountIdsUsingLanguage("en");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findAccountIdsUsingLanguage: テーブルが空の場合、空のリストを返す")
    void findAccountIdsUsingLanguage_returnsEmpty_whenTableIsEmpty() {
        // when
        List<String> result = sut.findAccountIdsUsingLanguage("ja");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findAccountIdsUsingLanguage: 同一accountIdで最新がjaから別言語に変更された場合、jaは含まれない")
    void findAccountIdsUsingLanguage_excludesAccountWhoseLatestIsDifferentLanguage() {
        // given
        // acc-1: ja → en に変更（最新はen）
        saveEntity("acc-1", BASE_TIME, "ja", "Asia/Tokyo", "user:1");
        saveEntity("acc-1", BASE_TIME.plusHours(1), "en", "Asia/Tokyo", "user:1");
        // acc-2: 単一version、ja使用中
        saveEntity("acc-2", BASE_TIME, "ja", "Asia/Tokyo", "user:2");

        // when
        List<String> result = sut.findAccountIdsUsingLanguage("ja");

        // then
        assertThat(result).containsExactly("acc-2");
    }

    @Test
    @DisplayName("findAccountIdsUsingTimezone: 指定TZを使用中のaccountIdを返す（単一一致）")
    void findAccountIdsUsingTimezone_returnsMatchingAccountId() {
        // given
        saveEntity("acc-1", BASE_TIME, "ja", "Asia/Tokyo", "user:1");
        saveEntity("acc-2", BASE_TIME, "en", "America/New_York", "user:2");

        // when
        List<String> result = sut.findAccountIdsUsingTimezone("Asia/Tokyo");

        // then
        assertThat(result).containsExactly("acc-1");
    }

    @Test
    @DisplayName("findAccountIdsUsingTimezone: 指定TZを使用中の複数accountIdを返す")
    void findAccountIdsUsingTimezone_returnsMultipleMatchingAccounts() {
        // given
        saveEntity("acc-1", BASE_TIME, "ja", "Asia/Tokyo", "user:1");
        saveEntity("acc-2", BASE_TIME, "en", "Asia/Tokyo", "user:2");
        saveEntity("acc-3", BASE_TIME, "en", "America/New_York", "user:3");

        // when
        List<String> result = sut.findAccountIdsUsingTimezone("Asia/Tokyo");

        // then
        assertThat(result).containsExactlyInAnyOrder("acc-1", "acc-2");
    }

    @Test
    @DisplayName("findAccountIdsUsingTimezone: 該当TZを使用中のアカウントが存在しない場合、空のリストを返す")
    void findAccountIdsUsingTimezone_returnsEmpty_whenNoMatchingAccount() {
        // given
        saveEntity("acc-1", BASE_TIME, "ja", "Asia/Tokyo", "user:1");

        // when
        List<String> result = sut.findAccountIdsUsingTimezone("America/New_York");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findAccountIdsUsingTimezone: テーブルが空の場合、空のリストを返す")
    void findAccountIdsUsingTimezone_returnsEmpty_whenTableIsEmpty() {
        // when
        List<String> result = sut.findAccountIdsUsingTimezone("Asia/Tokyo");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findAccountIdsUsingTimezone: 同一accountIdで最新がAsia/Tokyoから別TZに変更された場合、含まれない")
    void findAccountIdsUsingTimezone_excludesAccountWhoseLatestIsDifferentTimezone() {
        // given
        // acc-1: Asia/Tokyo → America/New_York に変更（最新はNYC）
        saveEntity("acc-1", BASE_TIME, "ja", "Asia/Tokyo", "user:1");
        saveEntity("acc-1", BASE_TIME.plusHours(1), "ja", "America/New_York", "user:1");
        // acc-2: 単一version、Asia/Tokyo使用中
        saveEntity("acc-2", BASE_TIME, "ja", "Asia/Tokyo", "user:2");

        // when
        List<String> result = sut.findAccountIdsUsingTimezone("Asia/Tokyo");

        // then
        assertThat(result).containsExactly("acc-2");
    }

    @Test
    @DisplayName("save: 新規レコードを保存できる")
    void save_savesNewRecord() {
        // given
        SystemAccountLocalization localization = new SystemAccountLocalization(
                "acc-1",
                BASE_TIME,
                SupportedLanguage.JA,
                SupportedTimezone.of("Asia/Tokyo"),
                OffsetDateTime.now(ZoneOffset.UTC),
                "user:setup");

        // when
        sut.save(localization);

        // then
        Optional<SystemAccountLocalizationEntity> saved =
                jpaRepository.findById(new SystemAccountLocalizationId("acc-1", BASE_TIME));
        assertThat(saved).isPresent();
        assertThat(saved.get().getLanguageCode()).isEqualTo("ja");
        assertThat(saved.get().getTimezoneId()).isEqualTo("Asia/Tokyo");
        assertThat(saved.get().getCreatedBy()).isEqualTo("user:setup");
    }

    @Test
    @DisplayName("save: 同一accountIdの異なるバージョンを複数保存できる（履歴管理の確認）")
    void save_savesMultipleVersionsForSameAccount() {
        // given
        OffsetDateTime v1 = BASE_TIME;
        OffsetDateTime v2 = BASE_TIME.plusHours(1);
        SystemAccountLocalization l1 = new SystemAccountLocalization(
                "acc-1", v1,
                SupportedLanguage.JA, SupportedTimezone.of("Asia/Tokyo"),
                OffsetDateTime.now(ZoneOffset.UTC), "user:1");
        SystemAccountLocalization l2 = new SystemAccountLocalization(
                "acc-1", v2,
                SupportedLanguage.EN, SupportedTimezone.of("America/New_York"),
                OffsetDateTime.now(ZoneOffset.UTC), "user:1");

        // when
        sut.save(l1);
        sut.save(l2);

        // then
        assertThat(jpaRepository.findAll()).hasSize(2);
    }

    @Test
    @DisplayName("save: ドメインモデルの全フィールドが正しく永続化される")
    void save_persistsAllFields() {
        // given
        OffsetDateTime createdAt = BASE_TIME.plusMinutes(5);
        SystemAccountLocalization localization = new SystemAccountLocalization(
                "acc-1",
                BASE_TIME,
                SupportedLanguage.EN,
                SupportedTimezone.of("America/New_York"),
                createdAt,
                "system:setup-wizard");

        // when
        sut.save(localization);

        // then
        Optional<SystemAccountLocalization> reloaded = sut.findLatestByAccountId("acc-1");
        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getAccountId()).isEqualTo("acc-1");
        assertThat(reloaded.get().getVersion()).isEqualTo(BASE_TIME);
        assertThat(reloaded.get().getLanguage()).isEqualTo(SupportedLanguage.EN);
        assertThat(reloaded.get().getTimezone().getId()).isEqualTo("America/New_York");
        assertThat(reloaded.get().getCreatedAt()).isEqualTo(createdAt);
        assertThat(reloaded.get().getCreatedBy()).isEqualTo("system:setup-wizard");
    }

    @Test
    @DisplayName("findLatestByAccountId: 保存された言語コードが不正な場合、IllegalStateExceptionを投げる")
    void findLatestByAccountId_throwsIllegalStateException_whenInvalidLanguageCode() {
        // given - 不正な言語コードを直接DB保存
        saveEntity("acc-1", BASE_TIME, "xx-INVALID", "Asia/Tokyo", "user:1");

        // when / then
        assertThatThrownBy(() -> sut.findLatestByAccountId("acc-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Unknown language code")
                .hasMessageContaining("xx-INVALID");
    }

    @Test
    @DisplayName("findLatestByAccountId: 保存されたタイムゾーンIDが不正な場合、ZoneRulesExceptionを投げる")
    void findLatestByAccountId_throwsZoneRulesException_whenInvalidTimezoneId() {
        // given - 不正なTZ IDを直接DB保存
        saveEntity("acc-1", BASE_TIME, "ja", "Invalid/Timezone", "user:1");

        // when / then
        assertThatThrownBy(() -> sut.findLatestByAccountId("acc-1"))
                .isInstanceOf(ZoneRulesException.class);
    }

    /**
     * テストデータを直接JPAリポジトリ経由で投入する。
     */
    private void saveEntity(
            String accountId,
            OffsetDateTime version,
            String languageCode,
            String timezoneId,
            String createdBy) {
        SystemAccountLocalizationEntity entity = new SystemAccountLocalizationEntity(
                new SystemAccountLocalizationId(accountId, version),
                languageCode,
                timezoneId,
                OffsetDateTime.now(ZoneOffset.UTC),
                createdBy);
        jpaRepository.save(entity);
    }

}
