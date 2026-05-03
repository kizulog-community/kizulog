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
 * <p>Output Port（SystemAccountRoleRepository）のメソッドについて、
 * 正常系・境界値・異常系を検証する。</p>
 *
 * <p>テスト対象は SystemAccountRoleRepositoryImpl のみ。
 * Spring Data JPA提供の SystemAccountRoleJpaRepository は
 * フレームワーク提供のためテスト対象外。</p>
 *
 * <p>SystemAccountRoleはPK=(account_id, role, version)であり、1アカウントが複数ロールを持てる構造。
 * findLatestByAccountIdは(account_id, role)の組み合わせごとに最新versionを取得する設計。</p>
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
            String accountId, SystemRole role, OffsetDateTime version, String createdBy) {
        SystemAccountRoleEntity entity = new SystemAccountRoleEntity(
                new SystemAccountRoleId(accountId, role, version),
                version, createdBy);
        jpaRepository.save(entity);
    }

    // ========================================================================
    // findLatestByAccountId
    // ========================================================================

    @Test
    @DisplayName("findLatestByAccountId: 同一(accountId, role)で複数バージョンが存在する場合、最大versionのレコードを返す")
    void findLatestByAccountId_returnsLatestVersion_whenMultipleVersionsExist() {
        // given
        OffsetDateTime v1 = BASE_TIME;
        OffsetDateTime v2 = BASE_TIME.plusHours(1);
        OffsetDateTime v3 = BASE_TIME.plusHours(2);
        saveEntity("acc-1", SystemRole.SYSTEM_ADMIN, v1, "user:1");
        saveEntity("acc-1", SystemRole.SYSTEM_ADMIN, v3, "user:3");
        saveEntity("acc-1", SystemRole.SYSTEM_ADMIN, v2, "user:2");

        // when
        List<SystemAccountRole> result = sut.findLatestByAccountId("acc-1");

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAccountId()).isEqualTo("acc-1");
        assertThat(result.get(0).getRole()).isEqualTo(SystemRole.SYSTEM_ADMIN);
        assertThat(result.get(0).getVersion()).isEqualTo(v3);
        assertThat(result.get(0).getCreatedBy()).isEqualTo("user:3");
    }

    @Test
    @DisplayName("findLatestByAccountId: 単一ロールが1件のみ存在する場合、そのレコードを返す")
    void findLatestByAccountId_returnsSingleRole_whenOnlyOneExists() {
        // given
        saveEntity("acc-1", SystemRole.SYSTEM_ADMIN, BASE_TIME, "system:setup");

        // when
        List<SystemAccountRole> result = sut.findLatestByAccountId("acc-1");

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRole()).isEqualTo(SystemRole.SYSTEM_ADMIN);
    }

    @Test
    @DisplayName("findLatestByAccountId: 該当accountIdが存在しない場合、空リストを返す")
    void findLatestByAccountId_returnsEmpty_whenNotFound() {
        // given
        saveEntity("acc-1", SystemRole.SYSTEM_ADMIN, BASE_TIME, "user:1");

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
        saveEntity("acc-1", SystemRole.SYSTEM_ADMIN, BASE_TIME, "user:1");
        saveEntity("acc-2", SystemRole.SYSTEM_ADMIN, BASE_TIME.plusHours(1), "user:2");

        // when
        List<SystemAccountRole> result = sut.findLatestByAccountId("acc-1");

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAccountId()).isEqualTo("acc-1");
    }

    // ========================================================================
    // save
    // ========================================================================

    @Test
    @DisplayName("save: 新規レコードを保存できる")
    void save_persistsNewRecord() {
        // given
        SystemAccountRole role = new SystemAccountRole(
                "acc-1", SystemRole.SYSTEM_ADMIN, BASE_TIME, BASE_TIME, "system:setup");

        // when
        sut.save(role);

        // then
        assertThat(jpaRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("save: 同一(accountId, role)で複数バージョンを保存すると全バージョンが残る")
    void save_persistsMultipleVersions() {
        // given
        OffsetDateTime v1 = BASE_TIME;
        OffsetDateTime v2 = BASE_TIME.plusHours(1);
        SystemAccountRole r1 = new SystemAccountRole(
                "acc-1", SystemRole.SYSTEM_ADMIN, v1, v1, "user:1");
        SystemAccountRole r2 = new SystemAccountRole(
                "acc-1", SystemRole.SYSTEM_ADMIN, v2, v2, "user:2");

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
                "acc-1", SystemRole.SYSTEM_ADMIN, BASE_TIME, BASE_TIME, "system:test");

        // when
        sut.save(role);

        // then
        List<SystemAccountRole> loaded = sut.findLatestByAccountId("acc-1");
        assertThat(loaded).hasSize(1);
        assertThat(loaded.get(0).getAccountId()).isEqualTo("acc-1");
        assertThat(loaded.get(0).getRole()).isEqualTo(SystemRole.SYSTEM_ADMIN);
        assertThat(loaded.get(0).getVersion()).isEqualTo(BASE_TIME);
        assertThat(loaded.get(0).getCreatedAt()).isEqualTo(BASE_TIME);
        assertThat(loaded.get(0).getCreatedBy()).isEqualTo("system:test");
    }

}
