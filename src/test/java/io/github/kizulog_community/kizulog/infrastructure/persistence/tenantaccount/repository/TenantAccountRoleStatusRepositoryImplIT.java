package io.github.kizulog_community.kizulog.infrastructure.persistence.tenantaccount.repository;

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

import io.github.kizulog_community.kizulog.domain.tenantaccount.model.TenantAccountRoleStatus;
import io.github.kizulog_community.kizulog.domain.tenantaccount.model.TenantAccountStatusValue;
import io.github.kizulog_community.kizulog.infrastructure.persistence.AbstractRepositoryIT;
import io.github.kizulog_community.kizulog.infrastructure.persistence.tenantaccount.entity.TenantAccountRoleStatusEntity;
import io.github.kizulog_community.kizulog.infrastructure.persistence.tenantaccount.entity.TenantAccountRoleStatusId;

/**
 * TenantAccountRoleStatusRepositoryImpl の統合テスト
 *
 * @author Jun Kobayashi
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import(TenantAccountRoleStatusRepositoryImpl.class)
class TenantAccountRoleStatusRepositoryImplIT extends AbstractRepositoryIT {

    /** テスト対象 */
    @Autowired
    private TenantAccountRoleStatusRepositoryImpl sut;

    /** テストデータ投入用JPAリポジトリ */
    @Autowired
    private TenantAccountRoleStatusJpaRepository jpaRepository;

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
            TenantAccountStatusValue status, String reason, String createdBy) {
        TenantAccountRoleStatusEntity entity = new TenantAccountRoleStatusEntity(
                new TenantAccountRoleStatusId(roleId, version),
                status, reason, version, createdBy);
        jpaRepository.save(entity);
    }

    @Test
    @DisplayName("findLatestByRoleId: 同一role_idで複数versionが存在する場合、最大versionを返す")
    void findLatestByRoleId_returnsLatestVersion() {
        // given - ACTIVE → INACTIVE のような遷移
        OffsetDateTime v1 = BASE_TIME;
        OffsetDateTime v2 = BASE_TIME.plusHours(1);
        saveEntity("role-1", v1, TenantAccountStatusValue.ACTIVE, null, "user:1");
        saveEntity("role-1", v2, TenantAccountStatusValue.INACTIVE, "revoked", "user:2");

        // when
        Optional<TenantAccountRoleStatus> result = sut.findLatestByRoleId("role-1");

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getVersion()).isEqualTo(v2);
        assertThat(result.get().getStatus()).isEqualTo(TenantAccountStatusValue.INACTIVE);
        assertThat(result.get().getReason()).isEqualTo("revoked");
    }

    @Test
    @DisplayName("findLatestByRoleId: 単一versionのみ存在する場合、そのレコードを返す")
    void findLatestByRoleId_returnsSingleVersion() {
        // given
        saveEntity("role-1", BASE_TIME,
                TenantAccountStatusValue.ACTIVE, null, "system:setup");

        // when
        Optional<TenantAccountRoleStatus> result = sut.findLatestByRoleId("role-1");

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getStatus()).isEqualTo(TenantAccountStatusValue.ACTIVE);
    }

    @Test
    @DisplayName("findLatestByRoleId: 該当role_idが存在しない場合、空Optional")
    void findLatestByRoleId_returnsEmpty_whenNotFound() {
        // given
        saveEntity("role-1", BASE_TIME,
                TenantAccountStatusValue.ACTIVE, null, "user:1");

        // when
        Optional<TenantAccountRoleStatus> result = sut.findLatestByRoleId("role-X");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findLatestByRoleId: テーブルが空の場合、空Optional")
    void findLatestByRoleId_returnsEmpty_whenTableIsEmpty() {
        // when
        Optional<TenantAccountRoleStatus> result = sut.findLatestByRoleId("role-1");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findLatestByRoleId: 複数role_idが混在する場合、指定IDのみ返す")
    void findLatestByRoleId_returnsOnlyMatching() {
        // given
        saveEntity("role-1", BASE_TIME,
                TenantAccountStatusValue.ACTIVE, null, "user:1");
        saveEntity("role-2", BASE_TIME.plusHours(1),
                TenantAccountStatusValue.INACTIVE, "x", "user:2");

        // when
        Optional<TenantAccountRoleStatus> result = sut.findLatestByRoleId("role-1");

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getRoleId()).isEqualTo("role-1");
        assertThat(result.get().getStatus()).isEqualTo(TenantAccountStatusValue.ACTIVE);
    }

    @Test
    @DisplayName("save: 新規レコードを保存できる")
    void save_persistsNewRecord() {
        // given
        TenantAccountRoleStatus status = new TenantAccountRoleStatus(
                "role-1", BASE_TIME, TenantAccountStatusValue.ACTIVE, null,
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
        TenantAccountRoleStatus s1 = new TenantAccountRoleStatus(
                "role-1", BASE_TIME, TenantAccountStatusValue.ACTIVE, null,
                BASE_TIME, "user:1");
        TenantAccountRoleStatus s2 = new TenantAccountRoleStatus(
                "role-1", BASE_TIME.plusHours(1), TenantAccountStatusValue.INACTIVE,
                "reason", BASE_TIME.plusHours(1), "user:2");

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
        TenantAccountRoleStatus status = new TenantAccountRoleStatus(
                "role-1", BASE_TIME, TenantAccountStatusValue.INACTIVE, "test reason",
                BASE_TIME, "system:test");

        // when
        sut.save(status);

        // then
        Optional<TenantAccountRoleStatus> loaded = sut.findLatestByRoleId("role-1");
        assertThat(loaded).isPresent();
        assertThat(loaded.get().getRoleId()).isEqualTo("role-1");
        assertThat(loaded.get().getVersion()).isEqualTo(BASE_TIME);
        assertThat(loaded.get().getStatus()).isEqualTo(TenantAccountStatusValue.INACTIVE);
        assertThat(loaded.get().getReason()).isEqualTo("test reason");
        assertThat(loaded.get().getCreatedAt()).isEqualTo(BASE_TIME);
        assertThat(loaded.get().getCreatedBy()).isEqualTo("system:test");
    }

}
