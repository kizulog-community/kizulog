package io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccount.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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
import io.github.kizulog_community.kizulog.domain.systemaccount.model.SystemAccountRoleStatus;
import io.github.kizulog_community.kizulog.infrastructure.persistence.AbstractRepositoryIT;
import io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccount.entity.SystemAccountRoleStatusEntity;
import io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccount.entity.SystemAccountRoleStatusId;

/**
 * SystemAccountRoleStatusRepositoryImpl の統合テスト
 *
 * @author Jun Kobayashi
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import(SystemAccountRoleStatusRepositoryImpl.class)
class SystemAccountRoleStatusRepositoryImplIT extends AbstractRepositoryIT {

    /** テスト対象 */
    @Autowired
    private SystemAccountRoleStatusRepositoryImpl sut;

    /** テストデータ投入用JPAリポジトリ */
    @Autowired
    private SystemAccountRoleStatusJpaRepository jpaRepository;

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
            String roleId, OffsetDateTime version,
            AccountStatus status, String reason, String createdBy) {
        SystemAccountRoleStatusEntity entity = new SystemAccountRoleStatusEntity(
                new SystemAccountRoleStatusId(roleId, version),
                status, reason, version, createdBy);
        jpaRepository.save(entity);
    }

    // ========================================================================
    // findLatestByRoleId
    // ========================================================================

    @Test
    @DisplayName("findLatestByRoleId: 同一role_idで複数versionが存在する場合、最大versionを返す")
    void findLatestByRoleId_returnsLatestVersion() {
        // given - ACTIVE → INACTIVE のような遷移
        OffsetDateTime v1 = BASE_TIME;
        OffsetDateTime v2 = BASE_TIME.plusHours(1);
        saveEntity("role-1", v1, AccountStatus.ACTIVE, null, "user:1");
        saveEntity("role-1", v2, AccountStatus.INACTIVE, "revoked", "user:2");

        // when
        Optional<SystemAccountRoleStatus> result = sut.findLatestByRoleId("role-1");

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getVersion()).isEqualTo(v2);
        assertThat(result.get().getStatus()).isEqualTo(AccountStatus.INACTIVE);
        assertThat(result.get().getReason()).isEqualTo("revoked");
    }

    @Test
    @DisplayName("findLatestByRoleId: 単一versionのみ存在する場合、そのレコードを返す")
    void findLatestByRoleId_returnsSingleVersion() {
        // given
        saveEntity("role-1", BASE_TIME, AccountStatus.ACTIVE, null, "system:setup");

        // when
        Optional<SystemAccountRoleStatus> result = sut.findLatestByRoleId("role-1");

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getStatus()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    @DisplayName("findLatestByRoleId: 該当role_idが存在しない場合、空Optional")
    void findLatestByRoleId_returnsEmpty_whenNotFound() {
        // given
        saveEntity("role-1", BASE_TIME, AccountStatus.ACTIVE, null, "user:1");

        // when
        Optional<SystemAccountRoleStatus> result = sut.findLatestByRoleId("role-X");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findLatestByRoleId: テーブルが空の場合、空Optional")
    void findLatestByRoleId_returnsEmpty_whenTableIsEmpty() {
        // when
        Optional<SystemAccountRoleStatus> result = sut.findLatestByRoleId("role-1");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findLatestByRoleId: 複数role_idが混在する場合、指定IDのみ返す")
    void findLatestByRoleId_returnsOnlyMatching() {
        // given
        saveEntity("role-1", BASE_TIME, AccountStatus.ACTIVE, null, "user:1");
        saveEntity("role-2", BASE_TIME.plusHours(1), AccountStatus.INACTIVE, "x", "user:2");

        // when
        Optional<SystemAccountRoleStatus> result = sut.findLatestByRoleId("role-1");

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getRoleId()).isEqualTo("role-1");
        assertThat(result.get().getStatus()).isEqualTo(AccountStatus.ACTIVE);
    }

    // ========================================================================
    // save
    // ========================================================================

    @Test
    @DisplayName("save: 新規レコードを保存できる")
    void save_persistsNewRecord() {
        // given
        SystemAccountRoleStatus status = new SystemAccountRoleStatus(
                "role-1", BASE_TIME, AccountStatus.ACTIVE, null,
                BASE_TIME, "system:setup");

        // when
        sut.save(status);

        // then
        assertThat(jpaRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("save: 同一role_idで複数バージョンを保存すると全バージョンが残る")
    void save_persistsMultipleVersions() {
        // given
        SystemAccountRoleStatus s1 = new SystemAccountRoleStatus(
                "role-1", BASE_TIME, AccountStatus.ACTIVE, null, BASE_TIME, "user:1");
        SystemAccountRoleStatus s2 = new SystemAccountRoleStatus(
                "role-1", BASE_TIME.plusHours(1), AccountStatus.INACTIVE, "reason",
                BASE_TIME.plusHours(1), "user:2");

        // when
        sut.save(s1);
        sut.save(s2);

        // then
        assertThat(jpaRepository.findAll()).hasSize(2);
    }

    @Test
    @DisplayName("save: 全フィールドが正しく永続化される")
    void save_persistsAllFields() {
        // given
        SystemAccountRoleStatus status = new SystemAccountRoleStatus(
                "role-1", BASE_TIME, AccountStatus.INACTIVE, "test reason",
                BASE_TIME, "system:test");

        // when
        sut.save(status);

        // then
        Optional<SystemAccountRoleStatus> loaded = sut.findLatestByRoleId("role-1");
        assertThat(loaded).isPresent();
        assertThat(loaded.get().getRoleId()).isEqualTo("role-1");
        assertThat(loaded.get().getVersion()).isEqualTo(BASE_TIME);
        assertThat(loaded.get().getStatus()).isEqualTo(AccountStatus.INACTIVE);
        assertThat(loaded.get().getReason()).isEqualTo("test reason");
        assertThat(loaded.get().getCreatedAt()).isEqualTo(BASE_TIME);
        assertThat(loaded.get().getCreatedBy()).isEqualTo("system:test");
    }

}
