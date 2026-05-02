package io.github.kizulog_community.kizulog.infrastructure.persistence.systemconfig.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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

import io.github.kizulog_community.kizulog.domain.systemconfig.model.SystemConfig;
import io.github.kizulog_community.kizulog.infrastructure.persistence.AbstractRepositoryIT;
import io.github.kizulog_community.kizulog.infrastructure.persistence.systemconfig.entity.SystemConfigEntity;
import io.github.kizulog_community.kizulog.infrastructure.persistence.systemconfig.entity.SystemConfigId;

/**
 * SystemConfigRepositoryImpl の統合テスト
 *
 * <p>Output Port（SystemConfigRepository）のメソッドについて、
 * 正常系・境界値・異常系を検証する。</p>
 *
 * <p>テスト対象は SystemConfigRepositoryImpl のみ。
 * Spring Data JPA提供の SystemConfigJpaRepository は
 * フレームワーク提供のためテスト対象外。</p>
 *
 * @author Jun Kobayashi
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import(SystemConfigRepositoryImpl.class)
class SystemConfigRepositoryImplIT extends AbstractRepositoryIT {

    /** テスト対象 */
    @Autowired
    private SystemConfigRepositoryImpl sut;

    /** テストデータ投入用JPAリポジトリ */
    @Autowired
    private SystemConfigJpaRepository jpaRepository;

    /** テスト用基準時刻（UTC） */
    private static final OffsetDateTime BASE_TIME =
            OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

    @BeforeEach
    void setUp() {
        jpaRepository.deleteAll();
    }

    // ========================================================================
    // findLatestByKey
    // ========================================================================

    @Test
    @DisplayName("findLatestByKey: 同一キーで複数バージョンが存在する場合、最大versionのレコードを返す")
    void findLatestByKey_returnsLatestVersion_whenMultipleVersionsExist() {
        // given
        OffsetDateTime v1 = BASE_TIME;
        OffsetDateTime v2 = BASE_TIME.plusHours(1);
        OffsetDateTime v3 = BASE_TIME.plusHours(2);
        saveEntity("OIDC", v1, "{\"a\":1}", "user:1");
        saveEntity("OIDC", v3, "{\"a\":3}", "user:3");
        saveEntity("OIDC", v2, "{\"a\":2}", "user:2");

        // when
        Optional<SystemConfig> result = sut.findLatestByKey("OIDC");

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getKey()).isEqualTo("OIDC");
        assertThat(result.get().getVersion()).isEqualTo(v3);
        assertThat(result.get().getValue()).isEqualTo("{\"a\":3}");
        assertThat(result.get().getCreatedBy()).isEqualTo("user:3");
    }

    @Test
    @DisplayName("findLatestByKey: 単一バージョンしか存在しない場合、そのレコードを返す")
    void findLatestByKey_returnsSingleVersion_whenOnlyOneExists() {
        // given
        saveEntity("SYSTEM", BASE_TIME, "{\"locale\":\"ja\"}", "system:setup");

        // when
        Optional<SystemConfig> result = sut.findLatestByKey("SYSTEM");

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getValue()).isEqualTo("{\"locale\":\"ja\"}");
    }

    @Test
    @DisplayName("findLatestByKey: 該当キーが存在しない場合、空のOptionalを返す")
    void findLatestByKey_returnsEmpty_whenKeyDoesNotExist() {
        // given
        saveEntity("OIDC", BASE_TIME, "{}", "user:1");

        // when
        Optional<SystemConfig> result = sut.findLatestByKey("UNKNOWN");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findLatestByKey: テーブルが空の場合、空のOptionalを返す")
    void findLatestByKey_returnsEmpty_whenTableIsEmpty() {
        // when
        Optional<SystemConfig> result = sut.findLatestByKey("OIDC");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findLatestByKey: 複数キーが混在する場合、指定キーの最新のみを返す")
    void findLatestByKey_returnsOnlySpecifiedKey_whenMultipleKeysExist() {
        // given
        saveEntity("OIDC", BASE_TIME, "{\"oidc\":1}", "user:1");
        saveEntity("OIDC", BASE_TIME.plusHours(1), "{\"oidc\":2}", "user:2");
        saveEntity("SYSTEM", BASE_TIME.plusHours(2), "{\"system\":1}", "user:3");

        // when
        Optional<SystemConfig> result = sut.findLatestByKey("OIDC");

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getKey()).isEqualTo("OIDC");
        assertThat(result.get().getValue()).isEqualTo("{\"oidc\":2}");
    }

    // ========================================================================
    // findByKeyAndVersion
    // ========================================================================

    @Test
    @DisplayName("findByKeyAndVersion: キーとversionが一致するレコードを返す")
    void findByKeyAndVersion_returnsMatchingRecord() {
        // given
        OffsetDateTime v1 = BASE_TIME;
        OffsetDateTime v2 = BASE_TIME.plusHours(1);
        saveEntity("OIDC", v1, "{\"a\":1}", "user:1");
        saveEntity("OIDC", v2, "{\"a\":2}", "user:2");

        // when
        Optional<SystemConfig> result = sut.findByKeyAndVersion("OIDC", v1);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getVersion()).isEqualTo(v1);
        assertThat(result.get().getValue()).isEqualTo("{\"a\":1}");
    }

    @Test
    @DisplayName("findByKeyAndVersion: キーは一致するがversionが一致しない場合、空のOptionalを返す")
    void findByKeyAndVersion_returnsEmpty_whenVersionDoesNotMatch() {
        // given
        saveEntity("OIDC", BASE_TIME, "{}", "user:1");

        // when
        Optional<SystemConfig> result =
                sut.findByKeyAndVersion("OIDC", BASE_TIME.plusSeconds(1));

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByKeyAndVersion: versionは一致するがキーが一致しない場合、空のOptionalを返す")
    void findByKeyAndVersion_returnsEmpty_whenKeyDoesNotMatch() {
        // given
        saveEntity("OIDC", BASE_TIME, "{}", "user:1");

        // when
        Optional<SystemConfig> result = sut.findByKeyAndVersion("SYSTEM", BASE_TIME);

        // then
        assertThat(result).isEmpty();
    }

    // ========================================================================
    // findAllByKey
    // ========================================================================

    @Test
    @DisplayName("findAllByKey: 指定キーの全バージョンを返す")
    void findAllByKey_returnsAllVersionsForKey() {
        // given
        saveEntity("OIDC", BASE_TIME, "{\"a\":1}", "user:1");
        saveEntity("OIDC", BASE_TIME.plusHours(1), "{\"a\":2}", "user:2");
        saveEntity("OIDC", BASE_TIME.plusHours(2), "{\"a\":3}", "user:3");
        saveEntity("SYSTEM", BASE_TIME, "{\"sys\":1}", "user:1");

        // when
        List<SystemConfig> result = sut.findAllByKey("OIDC");

        // then
        assertThat(result).hasSize(3);
        assertThat(result).extracting(SystemConfig::getKey)
                .containsOnly("OIDC");
    }

    @Test
    @DisplayName("findAllByKey: 該当キーが存在しない場合、空のリストを返す")
    void findAllByKey_returnsEmptyList_whenKeyDoesNotExist() {
        // given
        saveEntity("OIDC", BASE_TIME, "{}", "user:1");

        // when
        List<SystemConfig> result = sut.findAllByKey("UNKNOWN");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findAllByKey: テーブルが空の場合、空のリストを返す")
    void findAllByKey_returnsEmptyList_whenTableIsEmpty() {
        // when
        List<SystemConfig> result = sut.findAllByKey("OIDC");

        // then
        assertThat(result).isEmpty();
    }

    // ========================================================================
    // save
    // ========================================================================

    @Test
    @DisplayName("save: 新規レコードを保存できる")
    void save_savesNewRecord() {
        // given
        SystemConfig config = new SystemConfig(
                "OIDC",
                BASE_TIME,
                "{\"issuer\":\"https://example.com\"}",
                OffsetDateTime.now(ZoneOffset.UTC),
                "user:setup");

        // when
        sut.save(config);

        // then
        Optional<SystemConfigEntity> saved =
                jpaRepository.findById(new SystemConfigId("OIDC", BASE_TIME));
        assertThat(saved).isPresent();
        assertThat(saved.get().getValue()).isEqualTo("{\"issuer\":\"https://example.com\"}");
        assertThat(saved.get().getCreatedBy()).isEqualTo("user:setup");
    }

    @Test
    @DisplayName("save: 同一キーの異なるバージョンを複数保存できる（履歴管理の確認）")
    void save_savesMultipleVersionsForSameKey() {
        // given
        OffsetDateTime v1 = BASE_TIME;
        OffsetDateTime v2 = BASE_TIME.plusHours(1);
        SystemConfig c1 = new SystemConfig("OIDC", v1, "{\"v\":1}",
                OffsetDateTime.now(ZoneOffset.UTC), "user:1");
        SystemConfig c2 = new SystemConfig("OIDC", v2, "{\"v\":2}",
                OffsetDateTime.now(ZoneOffset.UTC), "user:2");

        // when
        sut.save(c1);
        sut.save(c2);

        // then
        assertThat(jpaRepository.findByIdKey("OIDC")).hasSize(2);
    }

    @Test
    @DisplayName("save: ドメインモデルの全フィールドが正しく永続化される")
    void save_persistsAllFields() {
        // given
        OffsetDateTime createdAt = BASE_TIME.plusMinutes(5);
        SystemConfig config = new SystemConfig(
                "OIDC",
                BASE_TIME,
                "{\"complex\":{\"nested\":\"value\"}}",
                createdAt,
                "system:setup-wizard");

        // when
        sut.save(config);

        // then
        Optional<SystemConfig> reloaded = sut.findByKeyAndVersion("OIDC", BASE_TIME);
        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getKey()).isEqualTo("OIDC");
        assertThat(reloaded.get().getVersion()).isEqualTo(BASE_TIME);
        assertThat(reloaded.get().getValue())
                .isEqualTo("{\"complex\":{\"nested\":\"value\"}}");
        assertThat(reloaded.get().getCreatedAt()).isEqualTo(createdAt);
        assertThat(reloaded.get().getCreatedBy()).isEqualTo("system:setup-wizard");
    }

    // ========================================================================
    // ヘルパーメソッド
    // ========================================================================

    /**
     * テストデータを直接JPAリポジトリ経由で投入する。
     *
     * <p>SUTの{@code save}を介さないため、findメソッド系のテストで
     * 「テストデータの存在」を前提にする際に使用する。</p>
     */
    private void saveEntity(
    		String key, OffsetDateTime version, String value, String createdBy) {
        SystemConfigEntity entity = new SystemConfigEntity(
                new SystemConfigId(key, version), value
                , OffsetDateTime.now(ZoneOffset.UTC), createdBy);
        jpaRepository.save(entity);
    }

}
