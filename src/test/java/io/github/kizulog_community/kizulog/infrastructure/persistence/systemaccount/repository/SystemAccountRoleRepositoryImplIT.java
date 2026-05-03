package io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccount.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.context.annotation.Import;

import io.github.kizulog_community.kizulog.domain.systemaccount.model.SystemAccountRole;
import io.github.kizulog_community.kizulog.domain.systemaccount.model.SystemRole;
import io.github.kizulog_community.kizulog.infrastructure.persistence.AbstractRepositoryIT;
import io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccount.entity.SystemAccountRoleEntity;
import io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccount.entity.SystemAccountRoleId;

/**
 * SystemAccountRoleRepositoryImpl の統合テスト
 *
 * @author Jun Kobayashi
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import(SystemAccountRoleRepositoryImpl.class)
class SystemAccountRoleRepositoryImplIT extends AbstractRepositoryIT {

    /** テスト対象 */
    @Autowired
    private SystemAccountRoleRepositoryImpl sut;

    /** テストデータ投入用JPAリポジトリ */
    @Autowired
    private SystemAccountRoleJpaRepository jpaRepository;

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
            String accountId, SystemRole role, String createdBy) {
        SystemAccountRoleEntity entity = new SystemAccountRoleEntity(
                new SystemAccountRoleId(roleId, version),
                accountId, role, version, createdBy);
        jpaRepository.save(entity);
    }

    // ========================================================================
    // findLatestByAccountId
    // ========================================================================

    @Test
    @DisplayName("findLatestByAccountId: 同一role_idで複数バージョンが存在する場合、最大versionのレコードを返す")
    void findLatestByAccountId_returnsLatestVersion_whenMultipleVersionsExist() {
        // given - 同じrole_idで複数version
        OffsetDateTime v1 = BASE_TIME;
        OffsetDateTime v2 = BASE_TIME.plusHours(1);
        OffsetDateTime v3 = BASE_TIME.plusHours(2);
        saveEntity("role-1", v1, "acc-1", SystemRole.SYSTEM_ADMIN, "user:1");
        saveEntity("role-1", v3, "acc-1", SystemRole.SYSTEM_ADMIN, "user:3");
        saveEntity("role-1", v2, "acc-1", SystemRole.SYSTEM_ADMIN, "user:2");

        // when
        List<SystemAccountRole> result = sut.findLatestByAccountId("acc-1");

        // then - role-1の最新版（v3）のみ
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRoleId()).isEqualTo("role-1");
        assertThat(result.get(0).getAccountId()).isEqualTo("acc-1");
        assertThat(result.get(0).getRole()).isEqualTo(SystemRole.SYSTEM_ADMIN);
        assertThat(result.get(0).getVersion()).isEqualTo(v3);
        assertThat(result.get(0).getCreatedBy()).isEqualTo("user:3");
    }

    @Test
    @DisplayName("findLatestByAccountId: 単一role_idが1件のみ存在する場合、そのレコードを返す")
    void findLatestByAccountId_returnsSingleRole_whenOnlyOneExists() {
        // given
        saveEntity("role-1", BASE_TIME, "acc-1", SystemRole.SYSTEM_ADMIN, "system:setup");

        // when
        List<SystemAccountRole> result = sut.findLatestByAccountId("acc-1");

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRoleId()).isEqualTo("role-1");
        assertThat(result.get(0).getRole()).isEqualTo(SystemRole.SYSTEM_ADMIN);
    }

    @Test
    @DisplayName("findLatestByAccountId: 該当accountIdが存在しない場合、空リストを返す")
    void findLatestByAccountId_returnsEmpty_whenNotFound() {
        // given
        saveEntity("role-1", BASE_TIME, "acc-1", SystemRole.SYSTEM_ADMIN, "user:1");

        // when
        List<SystemAccountRole> result = sut.findLatestByAccountId("acc-X");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findLatestByAccountId: テーブルが空の場合、空リストを返す")
    void findLatestByAccountId_returnsEmpty_whenTableIsEmpty() {
        // when
        List<SystemAccountRole> result = sut.findLatestByAccountId("acc-1");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findLatestByAccountId: 複数accountIdが混在する場合、指定accountIdのみを返す")
    void findLatestByAccountId_returnsOnlySpecifiedAccountId() {
        // given
        saveEntity("role-1", BASE_TIME, "acc-1", SystemRole.SYSTEM_ADMIN, "user:1");
        saveEntity("role-2", BASE_TIME.plusHours(1), "acc-2", SystemRole.SYSTEM_ADMIN, "user:2");

        // when
        List<SystemAccountRole> result = sut.findLatestByAccountId("acc-1");

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRoleId()).isEqualTo("role-1");
        assertThat(result.get(0).getAccountId()).isEqualTo("acc-1");
    }

    @Test
    @DisplayName("findLatestByAccountId: 同一accountIdに複数role_idがある場合、各role_idの最新版を全て返す")
    void findLatestByAccountId_returnsAllRolesForAccount() {
        // given - 同じacc-1に異なるrole_idで2つのロール
        saveEntity("role-1", BASE_TIME, "acc-1", SystemRole.SYSTEM_ADMIN, "user:1");
        saveEntity("role-2", BASE_TIME.plusHours(1), "acc-1", SystemRole.SYSTEM_ADMIN, "user:2");

        // when
        List<SystemAccountRole> result = sut.findLatestByAccountId("acc-1");

        // then - 両方が返る（同じrole名でも別role_idなので別レコードとして扱う）
        assertThat(result).hasSize(2);
        assertThat(result).extracting(SystemAccountRole::getRoleId)
                .containsExactlyInAnyOrder("role-1", "role-2");
    }

    // ========================================================================
    // save
    // ========================================================================

    @Test
    @DisplayName("save: 新規レコードを保存できる")
    void save_persistsNewRecord() {
        // given
        SystemAccountRole role = new SystemAccountRole(
                "role-1", BASE_TIME, "acc-1", SystemRole.SYSTEM_ADMIN,
                BASE_TIME, "system:setup");

        // when
        sut.save(role);

        // then
        assertThat(jpaRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("save: 同一role_idで複数バージョンを保存すると全バージョンが残る")
    void save_persistsMultipleVersions() {
        // given
        OffsetDateTime v1 = BASE_TIME;
        OffsetDateTime v2 = BASE_TIME.plusHours(1);
        SystemAccountRole r1 = new SystemAccountRole(
                "role-1", v1, "acc-1", SystemRole.SYSTEM_ADMIN, v1, "user:1");
        SystemAccountRole r2 = new SystemAccountRole(
                "role-1", v2, "acc-1", SystemRole.SYSTEM_ADMIN, v2, "user:2");

        // when
        sut.save(r1);
        sut.save(r2);

        // then
        List<SystemAccountRoleEntity> all = jpaRepository.findAll();
        assertThat(all).hasSize(2);
    }

    @Test
    @DisplayName("save: 全フィールドが正しく永続化される")
    void save_persistsAllFields() {
        // given
        SystemAccountRole role = new SystemAccountRole(
                "role-1", BASE_TIME, "acc-1", SystemRole.SYSTEM_ADMIN,
                BASE_TIME, "system:test");

        // when
        sut.save(role);

        // then
        List<SystemAccountRole> loaded = sut.findLatestByAccountId("acc-1");
        assertThat(loaded).hasSize(1);
        assertThat(loaded.get(0).getRoleId()).isEqualTo("role-1");
        assertThat(loaded.get(0).getAccountId()).isEqualTo("acc-1");
        assertThat(loaded.get(0).getRole()).isEqualTo(SystemRole.SYSTEM_ADMIN);
        assertThat(loaded.get(0).getVersion()).isEqualTo(BASE_TIME);
        assertThat(loaded.get(0).getCreatedAt()).isEqualTo(BASE_TIME);
        assertThat(loaded.get(0).getCreatedBy()).isEqualTo("system:test");
    }

}
