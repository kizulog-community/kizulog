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

import io.github.kizulog_community.kizulog.domain.systemaccount.model.AccountStatus;
import io.github.kizulog_community.kizulog.domain.systemaccount.model.SystemAccountStatus;
import io.github.kizulog_community.kizulog.infrastructure.persistence.AbstractRepositoryIT;
import io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccount.entity.SystemAccountStatusEntity;
import io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccount.entity.SystemAccountStatusId;

/**
 * SystemAccountStatusRepositoryImpl の統合テスト
 *
 * <p>Output Port（SystemAccountStatusRepository）のメソッドについて、
 * 正常系・境界値・異常系を検証する。</p>
 *
 * <p>テスト対象は SystemAccountStatusRepositoryImpl のみ。
 * Spring Data JPA提供の SystemAccountStatusJpaRepository は
 * フレームワーク提供のためテスト対象外。</p>
 *
 * @author Jun Kobayashi
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import(SystemAccountStatusRepositoryImpl.class)
class SystemAccountStatusRepositoryImplIT extends AbstractRepositoryIT {

    /** テスト対象 */
    @Autowired
    private SystemAccountStatusRepositoryImpl sut;

    /** テストデータ投入用JPAリポジトリ */
    @Autowired
    private SystemAccountStatusJpaRepository jpaRepository;

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
            AccountStatus status, String reason, String createdBy) {
        SystemAccountStatusEntity entity = new SystemAccountStatusEntity(
                new SystemAccountStatusId(accountId, version),
                status, reason, version, createdBy);
        jpaRepository.save(entity);
    }

    // ========================================================================
    // findLatestByAccountId
    // ========================================================================

    @Test
    @DisplayName("findLatestByAccountId: 同一accountIdで複数バージョンが存在する場合、最大versionのレコードを返す")
    void findLatestByAccountId_returnsLatestVersion_whenMultipleVersionsExist() {
        // given
        OffsetDateTime v1 = BASE_TIME;
        OffsetDateTime v2 = BASE_TIME.plusHours(1);
        OffsetDateTime v3 = BASE_TIME.plusHours(2);
        saveEntity("acc-1", v1, AccountStatus.ACTIVE, null, "user:1");
        saveEntity("acc-1", v3, AccountStatus.INACTIVE, "退職", "user:3");
        saveEntity("acc-1", v2, AccountStatus.ACTIVE, null, "user:2");

        // when
        Optional<SystemAccountStatus> result = sut.findLatestByAccountId("acc-1");

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getAccountId()).isEqualTo("acc-1");
        assertThat(result.get().getVersion()).isEqualTo(v3);
        assertThat(result.get().getStatus()).isEqualTo(AccountStatus.INACTIVE);
        assertThat(result.get().getReason()).isEqualTo("退職");
        assertThat(result.get().getCreatedBy()).isEqualTo("user:3");
    }

    @Test
    @DisplayName("findLatestByAccountId: 単一バージョンしか存在しない場合、そのレコードを返す")
    void findLatestByAccountId_returnsSingleVersion_whenOnlyOneExists() {
        // given
        saveEntity("acc-1", BASE_TIME, AccountStatus.ACTIVE, null, "system:setup");

        // when
        Optional<SystemAccountStatus> result = sut.findLatestByAccountId("acc-1");

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getStatus()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    @DisplayName("findLatestByAccountId: 該当accountIdが存在しない場合、空のOptionalを返す")
    void findLatestByAccountId_returnsEmpty_whenNotFound() {
        // given
        saveEntity("acc-1", BASE_TIME, AccountStatus.ACTIVE, null, "user:1");

        // when
        Optional<SystemAccountStatus> result = sut.findLatestByAccountId("acc-X");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findLatestByAccountId: テーブルが空の場合、空のOptionalを返す")
    void findLatestByAccountId_returnsEmpty_whenTableIsEmpty() {
        // when
        Optional<SystemAccountStatus> result = sut.findLatestByAccountId("acc-1");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findLatestByAccountId: 複数accountIdが混在する場合、指定accountIdの最新のみを返す")
    void findLatestByAccountId_returnsOnlySpecifiedAccountId() {
        // given
        saveEntity("acc-1", BASE_TIME, AccountStatus.ACTIVE, null, "user:1");
        saveEntity("acc-1", BASE_TIME.plusHours(1),
                AccountStatus.INACTIVE, "test", "user:2");
        saveEntity("acc-2", BASE_TIME.plusHours(2),
                AccountStatus.ACTIVE, null, "user:3");

        // when
        Optional<SystemAccountStatus> result = sut.findLatestByAccountId("acc-1");

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getAccountId()).isEqualTo("acc-1");
        assertThat(result.get().getVersion()).isEqualTo(BASE_TIME.plusHours(1));
        assertThat(result.get().getStatus()).isEqualTo(AccountStatus.INACTIVE);
    }

    // ========================================================================
    // save
    // ========================================================================

    @Test
    @DisplayName("save: 新規レコードを保存できる")
    void save_persistsNewRecord() {
        // given
        SystemAccountStatus status = new SystemAccountStatus(
                "acc-1", BASE_TIME, AccountStatus.ACTIVE, null,
                BASE_TIME, "system:setup");

        // when
        sut.save(status);

        // then
        assertThat(jpaRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("save: 同一accountIdで複数バージョンを保存すると全バージョンが残る")
    void save_persistsMultipleVersions() {
        // given
        OffsetDateTime v1 = BASE_TIME;
        OffsetDateTime v2 = BASE_TIME.plusHours(1);
        SystemAccountStatus s1 = new SystemAccountStatus(
                "acc-1", v1, AccountStatus.ACTIVE, null, v1, "user:1");
        SystemAccountStatus s2 = new SystemAccountStatus(
                "acc-1", v2, AccountStatus.INACTIVE, "退職", v2, "user:2");

        // when
        sut.save(s1);
        sut.save(s2);

        // then
        List<SystemAccountStatusEntity> all = jpaRepository.findAll();
        assertThat(all).hasSize(2);
    }

    @Test
    @DisplayName("save: 全フィールドが正しく永続化される（reason含む）")
    void save_persistsAllFields() {
        // given
        SystemAccountStatus status = new SystemAccountStatus(
                "acc-1", BASE_TIME, AccountStatus.SUSPENDED, "セキュリティ違反",
                BASE_TIME, "system:test");

        // when
        sut.save(status);

        // then
        Optional<SystemAccountStatus> loaded = sut.findLatestByAccountId("acc-1");
        assertThat(loaded).isPresent();
        assertThat(loaded.get().getAccountId()).isEqualTo("acc-1");
        assertThat(loaded.get().getVersion()).isEqualTo(BASE_TIME);
        assertThat(loaded.get().getStatus()).isEqualTo(AccountStatus.SUSPENDED);
        assertThat(loaded.get().getReason()).isEqualTo("セキュリティ違反");
        assertThat(loaded.get().getCreatedAt()).isEqualTo(BASE_TIME);
        assertThat(loaded.get().getCreatedBy()).isEqualTo("system:test");
    }

}
