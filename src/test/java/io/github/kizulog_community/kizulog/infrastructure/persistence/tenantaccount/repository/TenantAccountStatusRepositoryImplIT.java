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

import io.github.kizulog_community.kizulog.domain.tenantaccount.model.TenantAccountStatus;
import io.github.kizulog_community.kizulog.domain.tenantaccount.model.TenantAccountStatusValue;
import io.github.kizulog_community.kizulog.infrastructure.persistence.AbstractRepositoryIT;
import io.github.kizulog_community.kizulog.infrastructure.persistence.tenantaccount.entity.TenantAccountStatusEntity;
import io.github.kizulog_community.kizulog.infrastructure.persistence.tenantaccount.entity.TenantAccountStatusId;

/**
 * TenantAccountStatusRepositoryImpl の統合テスト
 *
 * @author Jun Kobayashi
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import(TenantAccountStatusRepositoryImpl.class)
class TenantAccountStatusRepositoryImplIT extends AbstractRepositoryIT {

    /** テスト対象 */
    @Autowired
    private TenantAccountStatusRepositoryImpl sut;

    /** テストデータ投入用JPAリポジトリ */
    @Autowired
    private TenantAccountStatusJpaRepository jpaRepository;

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
            TenantAccountStatusValue status, String reason, String createdBy) {
        TenantAccountStatusEntity entity = new TenantAccountStatusEntity(
                new TenantAccountStatusId(accountId, version),
                status, reason, version, createdBy);
        jpaRepository.save(entity);
    }

    @Test
    @DisplayName("findLatestByAccountId: 同一accountIdで複数バージョンが存在する場合、最大versionのレコードを返す")
    void findLatestByAccountId_returnsLatestVersion_whenMultipleVersionsExist() {
        // given
        OffsetDateTime v1 = BASE_TIME;
        OffsetDateTime v2 = BASE_TIME.plusHours(1);
        OffsetDateTime v3 = BASE_TIME.plusHours(2);
        saveEntity("acc-1", v1, TenantAccountStatusValue.ACTIVE, null, "user:1");
        saveEntity("acc-1", v3, TenantAccountStatusValue.INACTIVE, "退職", "user:3");
        saveEntity("acc-1", v2, TenantAccountStatusValue.ACTIVE, null, "user:2");

        // when
        Optional<TenantAccountStatus> result = sut.findLatestByAccountId("acc-1");

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getAccountId()).isEqualTo("acc-1");
        assertThat(result.get().getVersion()).isEqualTo(v3);
        assertThat(result.get().getStatus()).isEqualTo(TenantAccountStatusValue.INACTIVE);
        assertThat(result.get().getReason()).isEqualTo("退職");
        assertThat(result.get().getCreatedBy()).isEqualTo("user:3");
    }

    @Test
    @DisplayName("findLatestByAccountId: 単一バージョンしか存在しない場合、そのレコードを返す")
    void findLatestByAccountId_returnsSingleVersion_whenOnlyOneExists() {
        // given
        saveEntity("acc-1", BASE_TIME,
                TenantAccountStatusValue.ACTIVE, null, "system:setup");

        // when
        Optional<TenantAccountStatus> result = sut.findLatestByAccountId("acc-1");

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getStatus()).isEqualTo(TenantAccountStatusValue.ACTIVE);
    }

    @Test
    @DisplayName("findLatestByAccountId: 該当accountIdが存在しない場合、空のOptionalを返す")
    void findLatestByAccountId_returnsEmpty_whenNotFound() {
        // given
        saveEntity("acc-1", BASE_TIME,
                TenantAccountStatusValue.ACTIVE, null, "user:1");

        // when
        Optional<TenantAccountStatus> result = sut.findLatestByAccountId("acc-X");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findLatestByAccountId: テーブルが空の場合、空のOptionalを返す")
    void findLatestByAccountId_returnsEmpty_whenTableIsEmpty() {
        // when
        Optional<TenantAccountStatus> result = sut.findLatestByAccountId("acc-1");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findLatestByAccountId: 複数accountIdが混在する場合、指定accountIdの最新のみを返す")
    void findLatestByAccountId_returnsOnlySpecifiedAccountId() {
        // given
        saveEntity("acc-1", BASE_TIME,
                TenantAccountStatusValue.ACTIVE, null, "user:1");
        saveEntity("acc-1", BASE_TIME.plusHours(1),
                TenantAccountStatusValue.INACTIVE, "test", "user:2");
        saveEntity("acc-2", BASE_TIME.plusHours(2),
                TenantAccountStatusValue.ACTIVE, null, "user:3");

        // when
        Optional<TenantAccountStatus> result = sut.findLatestByAccountId("acc-1");

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getAccountId()).isEqualTo("acc-1");
        assertThat(result.get().getVersion()).isEqualTo(BASE_TIME.plusHours(1));
        assertThat(result.get().getStatus()).isEqualTo(TenantAccountStatusValue.INACTIVE);
    }

    @Test
    @DisplayName("findAllByAccountIdOrderByVersionDesc: 指定accountIdの全履歴をversion降順で返す")
    void findAllByAccountIdOrderByVersionDesc_returnsAllHistoryDesc() {
        // given
        OffsetDateTime v1 = BASE_TIME;
        OffsetDateTime v2 = BASE_TIME.plusHours(1);
        OffsetDateTime v3 = BASE_TIME.plusHours(2);
        saveEntity("acc-1", v2, TenantAccountStatusValue.SUSPENDED, "セキュリティ違反", "user:2");
        saveEntity("acc-1", v1, TenantAccountStatusValue.ACTIVE, null, "user:1");
        saveEntity("acc-1", v3, TenantAccountStatusValue.ACTIVE, "復帰", "user:3");

        // when
        List<TenantAccountStatus> result =
                sut.findAllByAccountIdOrderByVersionDesc("acc-1");

        // then
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getVersion()).isEqualTo(v3);
        assertThat(result.get(1).getVersion()).isEqualTo(v2);
        assertThat(result.get(2).getVersion()).isEqualTo(v1);
    }

    @Test
    @DisplayName("findAllByAccountIdOrderByVersionDesc: 該当なしは空リスト")
    void findAllByAccountIdOrderByVersionDesc_returnsEmpty_whenNotFound() {
        // given
        saveEntity("acc-1", BASE_TIME,
                TenantAccountStatusValue.ACTIVE, null, "user:1");

        // when
        List<TenantAccountStatus> result =
                sut.findAllByAccountIdOrderByVersionDesc("acc-X");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findAllByAccountIdOrderByVersionDesc: 他accountIdのレコードを含まない")
    void findAllByAccountIdOrderByVersionDesc_filtersByAccountId() {
        // given
        saveEntity("acc-1", BASE_TIME,
                TenantAccountStatusValue.ACTIVE, null, "user:1");
        saveEntity("acc-2", BASE_TIME.plusHours(1),
                TenantAccountStatusValue.INACTIVE, "test", "user:2");

        // when
        List<TenantAccountStatus> result =
                sut.findAllByAccountIdOrderByVersionDesc("acc-1");

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAccountId()).isEqualTo("acc-1");
    }

    @Test
    @DisplayName("save: 新規レコードを保存できる")
    void save_persistsNewRecord() {
        // given
        TenantAccountStatus status = new TenantAccountStatus(
                "acc-1", BASE_TIME, TenantAccountStatusValue.ACTIVE, null,
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
        TenantAccountStatus s1 = new TenantAccountStatus(
                "acc-1", v1, TenantAccountStatusValue.ACTIVE, null, v1, "user:1");
        TenantAccountStatus s2 = new TenantAccountStatus(
                "acc-1", v2, TenantAccountStatusValue.INACTIVE, "退職", v2, "user:2");

        // when
        sut.save(s1);
        sut.save(s2);

        // then
        List<TenantAccountStatusEntity> all = jpaRepository.findAll();
        assertThat(all).hasSize(2);
    }

    @Test
    @DisplayName("save: 全フィールドが正しく永続化される（reason含む）")
    void save_persistsAllFields() {
        // given
        TenantAccountStatus status = new TenantAccountStatus(
                "acc-1", BASE_TIME, TenantAccountStatusValue.SUSPENDED, "セキュリティ違反",
                BASE_TIME, "system:test");

        // when
        sut.save(status);

        // then
        Optional<TenantAccountStatus> loaded = sut.findLatestByAccountId("acc-1");
        assertThat(loaded).isPresent();
        assertThat(loaded.get().getAccountId()).isEqualTo("acc-1");
        assertThat(loaded.get().getVersion()).isEqualTo(BASE_TIME);
        assertThat(loaded.get().getStatus()).isEqualTo(TenantAccountStatusValue.SUSPENDED);
        assertThat(loaded.get().getReason()).isEqualTo("セキュリティ違反");
        assertThat(loaded.get().getCreatedAt()).isEqualTo(BASE_TIME);
        assertThat(loaded.get().getCreatedBy()).isEqualTo("system:test");
    }

}
