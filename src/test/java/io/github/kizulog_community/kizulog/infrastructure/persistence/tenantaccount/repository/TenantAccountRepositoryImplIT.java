package io.github.kizulog_community.kizulog.infrastructure.persistence.tenantaccount.repository;

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

import io.github.kizulog_community.kizulog.domain.tenantaccount.model.TenantAccount;
import io.github.kizulog_community.kizulog.infrastructure.persistence.AbstractRepositoryIT;
import io.github.kizulog_community.kizulog.infrastructure.persistence.tenantaccount.entity.TenantAccountEntity;
import io.github.kizulog_community.kizulog.infrastructure.persistence.tenantaccount.entity.TenantAccountId;

/**
 * TenantAccountRepositoryImpl の統合テスト
 *
 * @author Jun Kobayashi
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import(TenantAccountRepositoryImpl.class)
class TenantAccountRepositoryImplIT extends AbstractRepositoryIT {

    /** テスト対象 */
    @Autowired
    private TenantAccountRepositoryImpl sut;

    /** テストデータ投入用JPAリポジトリ */
    @Autowired
    private TenantAccountJpaRepository jpaRepository;

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
            String tenantId, String createdBy) {
        TenantAccountEntity entity = new TenantAccountEntity(
                new TenantAccountId(accountId, version),
                tenantId, version, createdBy);
        jpaRepository.save(entity);
    }

    @Test
    @DisplayName("findLatestByAccountId: 同一accountIdで複数バージョンが存在する場合、最大versionのレコードを返す")
    void findLatestByAccountId_returnsLatestVersion_whenMultipleVersionsExist() {
        // given
        OffsetDateTime v1 = BASE_TIME;
        OffsetDateTime v2 = BASE_TIME.plusHours(1);
        OffsetDateTime v3 = BASE_TIME.plusHours(2);
        saveEntity("acc-1", v1, "tenant-A", "user:1");
        saveEntity("acc-1", v3, "tenant-A", "user:3");
        saveEntity("acc-1", v2, "tenant-A", "user:2");

        // when
        Optional<TenantAccount> result = sut.findLatestByAccountId("acc-1");

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getAccountId()).isEqualTo("acc-1");
        assertThat(result.get().getVersion()).isEqualTo(v3);
        assertThat(result.get().getTenantId()).isEqualTo("tenant-A");
        assertThat(result.get().getCreatedBy()).isEqualTo("user:3");
    }

    @Test
    @DisplayName("findLatestByAccountId: 単一バージョンしか存在しない場合、そのレコードを返す")
    void findLatestByAccountId_returnsSingleVersion_whenOnlyOneExists() {
        // given
        saveEntity("acc-1", BASE_TIME, "tenant-A", "system:setup");

        // when
        Optional<TenantAccount> result = sut.findLatestByAccountId("acc-1");

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getAccountId()).isEqualTo("acc-1");
        assertThat(result.get().getTenantId()).isEqualTo("tenant-A");
    }

    @Test
    @DisplayName("findLatestByAccountId: 該当accountIdが存在しない場合、空のOptionalを返す")
    void findLatestByAccountId_returnsEmpty_whenNotFound() {
        // given
        saveEntity("acc-1", BASE_TIME, "tenant-A", "user:1");

        // when
        Optional<TenantAccount> result = sut.findLatestByAccountId("acc-X");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findLatestByAccountId: テーブルが空の場合、空のOptionalを返す")
    void findLatestByAccountId_returnsEmpty_whenTableIsEmpty() {
        // when
        Optional<TenantAccount> result = sut.findLatestByAccountId("acc-1");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findLatestByAccountId: 複数accountIdが混在する場合、指定accountIdのみを返す")
    void findLatestByAccountId_returnsOnlySpecifiedAccountId() {
        // given
        saveEntity("acc-1", BASE_TIME, "tenant-A", "user:1");
        saveEntity("acc-1", BASE_TIME.plusHours(1), "tenant-A", "user:2");
        saveEntity("acc-2", BASE_TIME.plusHours(2), "tenant-A", "user:3");

        // when
        Optional<TenantAccount> result = sut.findLatestByAccountId("acc-1");

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getAccountId()).isEqualTo("acc-1");
        assertThat(result.get().getVersion()).isEqualTo(BASE_TIME.plusHours(1));
    }

    @Test
    @DisplayName("findAllLatestByTenantId: 指定テナントの各accountIdの最新versionを返す")
    void findAllLatestByTenantId_returnsLatestVersionPerAccountId() {
        // given
        saveEntity("acc-1", BASE_TIME, "tenant-A", "user:1");
        saveEntity("acc-1", BASE_TIME.plusHours(1), "tenant-A", "user:1");
        saveEntity("acc-2", BASE_TIME.plusHours(2), "tenant-A", "user:2");

        // when
        List<TenantAccount> result = sut.findAllLatestByTenantId("tenant-A");

        // then
        assertThat(result).hasSize(2);
        assertThat(result)
                .anySatisfy(a -> {
                    assertThat(a.getAccountId()).isEqualTo("acc-1");
                    assertThat(a.getVersion()).isEqualTo(BASE_TIME.plusHours(1));
                })
                .anySatisfy(a -> {
                    assertThat(a.getAccountId()).isEqualTo("acc-2");
                    assertThat(a.getVersion()).isEqualTo(BASE_TIME.plusHours(2));
                });
    }

    @Test
    @DisplayName("findAllLatestByTenantId: テーブルが空の場合、空リストを返す")
    void findAllLatestByTenantId_returnsEmptyList_whenTableIsEmpty() {
        // when
        List<TenantAccount> result = sut.findAllLatestByTenantId("tenant-A");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findAllLatestByTenantId: 別テナントのアカウントは含まない（テナント境界）")
    void findAllLatestByTenantId_excludesOtherTenants() {
        // given - tenant-A に 2件、tenant-B に 1件
        saveEntity("acc-A1", BASE_TIME, "tenant-A", "user:1");
        saveEntity("acc-A2", BASE_TIME.plusHours(1), "tenant-A", "user:2");
        saveEntity("acc-B1", BASE_TIME.plusHours(2), "tenant-B", "user:3");

        // when
        List<TenantAccount> result = sut.findAllLatestByTenantId("tenant-A");

        // then - tenant-A の 2件のみ
        assertThat(result).hasSize(2);
        assertThat(result).extracting(TenantAccount::getAccountId)
                .containsExactlyInAnyOrder("acc-A1", "acc-A2");
        assertThat(result).extracting(TenantAccount::getTenantId)
                .containsOnly("tenant-A");
    }

    @Test
    @DisplayName("findAllLatestByTenantId: 該当テナントにアカウントがない場合、空リストを返す")
    void findAllLatestByTenantId_returnsEmpty_whenTenantHasNoAccounts() {
        // given - tenant-A にのみ登録
        saveEntity("acc-1", BASE_TIME, "tenant-A", "user:1");

        // when - tenant-B を検索
        List<TenantAccount> result = sut.findAllLatestByTenantId("tenant-B");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("save: 新規レコードを保存できる")
    void save_persistsNewRecord() {
        // given
        TenantAccount account = new TenantAccount(
                "acc-1", BASE_TIME, "tenant-A", BASE_TIME, "system:setup");

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
        TenantAccount a1 = new TenantAccount("acc-1", v1, "tenant-A", v1, "user:1");
        TenantAccount a2 = new TenantAccount("acc-1", v2, "tenant-A", v2, "user:2");

        // when
        sut.save(a1);
        sut.save(a2);

        // then
        List<TenantAccountEntity> all = jpaRepository.findAll();
        assertThat(all).hasSize(2);
    }

    @Test
    @DisplayName("save: 全フィールドが正しく永続化される（tenantIdを含む）")
    void save_persistsAllFields() {
        // given
        TenantAccount account = new TenantAccount(
                "acc-1", BASE_TIME, "tenant-A", BASE_TIME, "system:test");

        // when
        sut.save(account);

        // then
        Optional<TenantAccount> loaded = sut.findLatestByAccountId("acc-1");
        assertThat(loaded).isPresent();
        assertThat(loaded.get().getAccountId()).isEqualTo("acc-1");
        assertThat(loaded.get().getVersion()).isEqualTo(BASE_TIME);
        assertThat(loaded.get().getTenantId()).isEqualTo("tenant-A");
        assertThat(loaded.get().getCreatedAt()).isEqualTo(BASE_TIME);
        assertThat(loaded.get().getCreatedBy()).isEqualTo("system:test");
    }

}
