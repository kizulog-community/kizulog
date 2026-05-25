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

import io.github.kizulog_community.kizulog.domain.tenantaccount.model.TenantAccountIdentityStatus;
import io.github.kizulog_community.kizulog.domain.tenantaccount.model.TenantAccountStatusValue;
import io.github.kizulog_community.kizulog.infrastructure.persistence.AbstractRepositoryIT;
import io.github.kizulog_community.kizulog.infrastructure.persistence.tenantaccount.entity.TenantAccountIdentityStatusEntity;
import io.github.kizulog_community.kizulog.infrastructure.persistence.tenantaccount.entity.TenantAccountIdentityStatusId;

/**
 * TenantAccountIdentityStatusRepositoryImpl の統合テスト
 *
 * @author Jun Kobayashi
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import(TenantAccountIdentityStatusRepositoryImpl.class)
class TenantAccountIdentityStatusRepositoryImplIT extends AbstractRepositoryIT {

    /** テスト対象 */
    @Autowired
    private TenantAccountIdentityStatusRepositoryImpl sut;

    /** テストデータ投入用JPAリポジトリ */
    @Autowired
    private TenantAccountIdentityStatusJpaRepository jpaRepository;

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
            String identityId, OffsetDateTime version,
            TenantAccountStatusValue status, String reason, String createdBy) {
        TenantAccountIdentityStatusEntity entity = new TenantAccountIdentityStatusEntity(
                new TenantAccountIdentityStatusId(identityId, version),
                status, reason, version, createdBy);
        jpaRepository.save(entity);
    }

    @Test
    @DisplayName("findLatestByIdentityId: 同一identity_idで複数versionが存在する場合、最大versionを返す")
    void findLatestByIdentityId_returnsLatestVersion() {
        // given - ACTIVE → INACTIVE → ACTIVE のような遷移
        OffsetDateTime v1 = BASE_TIME;
        OffsetDateTime v2 = BASE_TIME.plusHours(1);
        OffsetDateTime v3 = BASE_TIME.plusHours(2);
        saveEntity("id-1", v1, TenantAccountStatusValue.ACTIVE, null, "user:1");
        saveEntity("id-1", v2, TenantAccountStatusValue.INACTIVE, "tmp suspend", "user:2");
        saveEntity("id-1", v3, TenantAccountStatusValue.ACTIVE, null, "user:3");

        // when
        Optional<TenantAccountIdentityStatus> result = sut.findLatestByIdentityId("id-1");

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getVersion()).isEqualTo(v3);
        assertThat(result.get().getStatus()).isEqualTo(TenantAccountStatusValue.ACTIVE);
        assertThat(result.get().getCreatedBy()).isEqualTo("user:3");
    }

    @Test
    @DisplayName("findLatestByIdentityId: 単一versionのみ存在する場合、そのレコードを返す")
    void findLatestByIdentityId_returnsSingleVersion() {
        // given
        saveEntity("id-1", BASE_TIME,
                TenantAccountStatusValue.ACTIVE, null, "system:setup");

        // when
        Optional<TenantAccountIdentityStatus> result = sut.findLatestByIdentityId("id-1");

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getStatus()).isEqualTo(TenantAccountStatusValue.ACTIVE);
    }

    @Test
    @DisplayName("findLatestByIdentityId: 該当identity_idが存在しない場合、空Optional")
    void findLatestByIdentityId_returnsEmpty_whenNotFound() {
        // given
        saveEntity("id-1", BASE_TIME,
                TenantAccountStatusValue.ACTIVE, null, "user:1");

        // when
        Optional<TenantAccountIdentityStatus> result = sut.findLatestByIdentityId("id-X");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findLatestByIdentityId: テーブルが空の場合、空Optional")
    void findLatestByIdentityId_returnsEmpty_whenTableIsEmpty() {
        // when
        Optional<TenantAccountIdentityStatus> result = sut.findLatestByIdentityId("id-1");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findLatestByIdentityId: 複数identity_idが混在する場合、指定IDのみ返す")
    void findLatestByIdentityId_returnsOnlyMatching() {
        // given
        saveEntity("id-1", BASE_TIME,
                TenantAccountStatusValue.ACTIVE, null, "user:1");
        saveEntity("id-2", BASE_TIME.plusHours(1),
                TenantAccountStatusValue.INACTIVE, "x", "user:2");

        // when
        Optional<TenantAccountIdentityStatus> result = sut.findLatestByIdentityId("id-1");

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getIdentityId()).isEqualTo("id-1");
        assertThat(result.get().getStatus()).isEqualTo(TenantAccountStatusValue.ACTIVE);
    }

    @Test
    @DisplayName("save: 新規レコードを保存できる")
    void save_persistsNewRecord() {
        // given
        TenantAccountIdentityStatus status = new TenantAccountIdentityStatus(
                "id-1", BASE_TIME, TenantAccountStatusValue.ACTIVE, null,
                BASE_TIME, "system:setup");

        // when
        sut.save(status);

        // then
        assertThat(jpaRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("save: 同一identity_idで複数バージョンを保存すると全バージョンが残る")
    void save_persistsMultipleVersions() {
        // given
        TenantAccountIdentityStatus s1 = new TenantAccountIdentityStatus(
                "id-1", BASE_TIME, TenantAccountStatusValue.ACTIVE, null,
                BASE_TIME, "user:1");
        TenantAccountIdentityStatus s2 = new TenantAccountIdentityStatus(
                "id-1", BASE_TIME.plusHours(1), TenantAccountStatusValue.INACTIVE,
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
        TenantAccountIdentityStatus status = new TenantAccountIdentityStatus(
                "id-1", BASE_TIME, TenantAccountStatusValue.INACTIVE, "test reason",
                BASE_TIME, "system:test");

        // when
        sut.save(status);

        // then
        Optional<TenantAccountIdentityStatus> loaded = sut.findLatestByIdentityId("id-1");
        assertThat(loaded).isPresent();
        assertThat(loaded.get().getIdentityId()).isEqualTo("id-1");
        assertThat(loaded.get().getVersion()).isEqualTo(BASE_TIME);
        assertThat(loaded.get().getStatus()).isEqualTo(TenantAccountStatusValue.INACTIVE);
        assertThat(loaded.get().getReason()).isEqualTo("test reason");
        assertThat(loaded.get().getCreatedAt()).isEqualTo(BASE_TIME);
        assertThat(loaded.get().getCreatedBy()).isEqualTo("system:test");
    }

}
