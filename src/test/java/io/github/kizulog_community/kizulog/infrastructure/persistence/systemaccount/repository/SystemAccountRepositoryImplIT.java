package io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccount.repository;

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

import io.github.kizulog_community.kizulog.domain.systemaccount.model.SystemAccount;
import io.github.kizulog_community.kizulog.infrastructure.persistence.AbstractRepositoryIT;
import io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccount.entity.SystemAccountEntity;
import io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccount.entity.SystemAccountId;

/**
 * SystemAccountRepositoryImpl の統合テスト
 *
 * <p>Output Port（SystemAccountRepository）のメソッドについて、
 * 正常系・境界値・異常系を検証する。</p>
 *
 * <p>テスト対象は SystemAccountRepositoryImpl のみ。
 * Spring Data JPA提供の SystemAccountJpaRepository は
 * フレームワーク提供のためテスト対象外。</p>
 *
 * @author Jun Kobayashi
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import(SystemAccountRepositoryImpl.class)
class SystemAccountRepositoryImplIT extends AbstractRepositoryIT {

    /** テスト対象 */
    @Autowired
    private SystemAccountRepositoryImpl sut;

    /** テストデータ投入用JPAリポジトリ */
    @Autowired
    private SystemAccountJpaRepository jpaRepository;

    /** テスト用基準時刻（UTC） */
    private static final OffsetDateTime BASE_TIME =
            OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

    @BeforeEach
    void setUp() {
        jpaRepository.deleteAll();
    }

    /**
     * テストデータ投入ヘルパー（JPA直接保存）
     */
    private void saveEntity(
            String accountId, OffsetDateTime version,
            String iss, String aud, String sub, String createdBy) {
        SystemAccountEntity entity = new SystemAccountEntity(
                new SystemAccountId(accountId, version),
                iss, aud, sub, version, createdBy);
        jpaRepository.save(entity);
    }

    // ========================================================================
    // findLatestByIssAndAudAndSub
    // ========================================================================

    @Test
    @DisplayName("findLatestByIssAndAudAndSub: 同一(iss,aud,sub)で複数バージョンが存在する場合、最大versionのレコードを返す")
    void findLatestByIssAndAudAndSub_returnsLatestVersion_whenMultipleVersionsExist() {
        // given
        OffsetDateTime v1 = BASE_TIME;
        OffsetDateTime v2 = BASE_TIME.plusHours(1);
        OffsetDateTime v3 = BASE_TIME.plusHours(2);
        saveEntity("acc-1", v1, "iss-A", "aud-A", "sub-A", "user:1");
        saveEntity("acc-1", v3, "iss-A", "aud-A", "sub-A", "user:3");
        saveEntity("acc-1", v2, "iss-A", "aud-A", "sub-A", "user:2");

        // when
        Optional<SystemAccount> result =
                sut.findLatestByIssAndAudAndSub("iss-A", "aud-A", "sub-A");

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getAccountId()).isEqualTo("acc-1");
        assertThat(result.get().getVersion()).isEqualTo(v3);
        assertThat(result.get().getCreatedBy()).isEqualTo("user:3");
    }

    @Test
    @DisplayName("findLatestByIssAndAudAndSub: 単一バージョンしか存在しない場合、そのレコードを返す")
    void findLatestByIssAndAudAndSub_returnsSingleVersion_whenOnlyOneExists() {
        // given
        saveEntity("acc-1", BASE_TIME, "iss-A", "aud-A", "sub-A", "system:setup");

        // when
        Optional<SystemAccount> result =
                sut.findLatestByIssAndAudAndSub("iss-A", "aud-A", "sub-A");

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getAccountId()).isEqualTo("acc-1");
    }

    @Test
    @DisplayName("findLatestByIssAndAudAndSub: 該当(iss,aud,sub)が存在しない場合、空のOptionalを返す")
    void findLatestByIssAndAudAndSub_returnsEmpty_whenNotFound() {
        // given
        saveEntity("acc-1", BASE_TIME, "iss-A", "aud-A", "sub-A", "user:1");

        // when
        Optional<SystemAccount> result =
                sut.findLatestByIssAndAudAndSub("iss-X", "aud-X", "sub-X");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findLatestByIssAndAudAndSub: テーブルが空の場合、空のOptionalを返す")
    void findLatestByIssAndAudAndSub_returnsEmpty_whenTableIsEmpty() {
        // when
        Optional<SystemAccount> result =
                sut.findLatestByIssAndAudAndSub("iss-A", "aud-A", "sub-A");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findLatestByIssAndAudAndSub: 複数(iss,aud,sub)が混在する場合、指定組合せの最新のみを返す")
    void findLatestByIssAndAudAndSub_returnsOnlySpecifiedTriple() {
        // given
        saveEntity("acc-1", BASE_TIME,
                "iss-A", "aud-A", "sub-A", "user:1");
        saveEntity("acc-1", BASE_TIME.plusHours(1),
                "iss-A", "aud-A", "sub-A", "user:2");
        saveEntity("acc-2", BASE_TIME.plusHours(2),
                "iss-B", "aud-B", "sub-B", "user:3");

        // when
        Optional<SystemAccount> result =
                sut.findLatestByIssAndAudAndSub("iss-A", "aud-A", "sub-A");

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getAccountId()).isEqualTo("acc-1");
        assertThat(result.get().getVersion()).isEqualTo(BASE_TIME.plusHours(1));
    }

    // ========================================================================
    // save
    // ========================================================================

    @Test
    @DisplayName("save: 新規レコードを保存できる")
    void save_persistsNewRecord() {
        // given
        SystemAccount account = new SystemAccount(
                "acc-1", BASE_TIME, "iss-A", "aud-A", "sub-A",
                BASE_TIME, "system:setup");

        // when
        sut.save(account);

        // then
        assertThat(jpaRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("save: 同一accountIdで複数バージョンを保存すると全バージョンが残る")
    void save_persistsMultipleVersions() {
        // given
        OffsetDateTime v1 = BASE_TIME;
        OffsetDateTime v2 = BASE_TIME.plusHours(1);
        SystemAccount a1 = new SystemAccount(
                "acc-1", v1, "iss-A", "aud-A", "sub-A", v1, "user:1");
        SystemAccount a2 = new SystemAccount(
                "acc-1", v2, "iss-A", "aud-A", "sub-A", v2, "user:2");

        // when
        sut.save(a1);
        sut.save(a2);

        // then
        List<SystemAccountEntity> all = jpaRepository.findAll();
        assertThat(all).hasSize(2);
    }

    @Test
    @DisplayName("save: 全フィールドが正しく永続化される")
    void save_persistsAllFields() {
        // given
        SystemAccount account = new SystemAccount(
                "acc-1", BASE_TIME,
                "iss-value", "aud-value", "sub-value",
                BASE_TIME, "system:test");

        // when
        sut.save(account);

        // then
        Optional<SystemAccount> loaded =
                sut.findLatestByIssAndAudAndSub("iss-value", "aud-value", "sub-value");
        assertThat(loaded).isPresent();
        assertThat(loaded.get().getAccountId()).isEqualTo("acc-1");
        assertThat(loaded.get().getVersion()).isEqualTo(BASE_TIME);
        assertThat(loaded.get().getIss()).isEqualTo("iss-value");
        assertThat(loaded.get().getAud()).isEqualTo("aud-value");
        assertThat(loaded.get().getSub()).isEqualTo("sub-value");
        assertThat(loaded.get().getCreatedAt()).isEqualTo(BASE_TIME);
        assertThat(loaded.get().getCreatedBy()).isEqualTo("system:test");
    }

}
