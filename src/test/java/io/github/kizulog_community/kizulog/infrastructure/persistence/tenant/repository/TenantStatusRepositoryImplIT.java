package io.github.kizulog_community.kizulog.infrastructure.persistence.tenant.repository;

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

import io.github.kizulog_community.kizulog.domain.tenant.model.TenantStatus;
import io.github.kizulog_community.kizulog.domain.tenant.model.TenantStatusValue;
import io.github.kizulog_community.kizulog.infrastructure.persistence.AbstractRepositoryIT;
import io.github.kizulog_community.kizulog.infrastructure.persistence.tenant.entity.TenantStatusEntity;
import io.github.kizulog_community.kizulog.infrastructure.persistence.tenant.entity.TenantStatusId;

/**
 * TenantStatusRepositoryImpl の統合テスト
 *
 * @author Jun Kobayashi
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import(TenantStatusRepositoryImpl.class)
class TenantStatusRepositoryImplIT extends AbstractRepositoryIT {

    /** テスト対象 */
    @Autowired
    private TenantStatusRepositoryImpl sut;

    /** テストデータ投入用JPAリポジトリ */
    @Autowired
    private TenantStatusJpaRepository jpaRepository;

    /** テスト用基準時刻（UTC） */
    private static final OffsetDateTime BASE_TIME =
            OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

    @BeforeEach
    void setUp() {
        jpaRepository.deleteAll();
    }

    @Test
    @DisplayName("findLatestByTenantId: 同一tenant_idで複数versionが存在する場合、最大versionを返す")
    void findLatestByTenantId_returnsLatestVersion_whenMultipleVersionsExist() {
        OffsetDateTime v1 = BASE_TIME;
        OffsetDateTime v2 = BASE_TIME.plusHours(1);
        OffsetDateTime v3 = BASE_TIME.plusHours(2);
        saveEntity("t1", v1, TenantStatusValue.ACTIVE, "initial");
        saveEntity("t1", v3, TenantStatusValue.INACTIVE, "deactivated");
        saveEntity("t1", v2, TenantStatusValue.SUSPENDED, "suspended");

        Optional<TenantStatus> result = sut.findLatestByTenantId("t1");

        assertThat(result).isPresent();
        assertThat(result.get().getTenantId()).isEqualTo("t1");
        assertThat(result.get().getVersion()).isEqualTo(v3);
        assertThat(result.get().getStatus()).isEqualTo(TenantStatusValue.INACTIVE);
        assertThat(result.get().getReason()).isEqualTo("deactivated");
    }

    @Test
    @DisplayName("findLatestByTenantId: tenant_idが存在しない場合、空Optionalを返す")
    void findLatestByTenantId_returnsEmpty_whenNotExist() {
        Optional<TenantStatus> result = sut.findLatestByTenantId("missing");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findAllByTenantIdOrderByVersionDesc: 履歴が0件の場合、空リストを返す")
    void findAllByTenantIdOrderByVersionDesc_returnsEmptyList_whenNoHistory() {
        List<TenantStatus> result =
                sut.findAllByTenantIdOrderByVersionDesc("missing");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findAllByTenantIdOrderByVersionDesc: 全レコードをversion降順で返す")
    void findAllByTenantIdOrderByVersionDesc_returnsAllRecordsInDescOrder() {
        OffsetDateTime v1 = BASE_TIME;
        OffsetDateTime v2 = BASE_TIME.plusHours(1);
        OffsetDateTime v3 = BASE_TIME.plusHours(2);
        saveEntity("t1", v1, TenantStatusValue.ACTIVE, "initial");
        saveEntity("t1", v2, TenantStatusValue.SUSPENDED, "suspended");
        saveEntity("t1", v3, TenantStatusValue.ACTIVE, "reactivated");

        List<TenantStatus> result =
                sut.findAllByTenantIdOrderByVersionDesc("t1");

        assertThat(result).hasSize(3);
        assertThat(result).extracting(TenantStatus::getVersion)
                .containsExactly(v3, v2, v1);
        assertThat(result).extracting(TenantStatus::getStatus)
                .containsExactly(
                        TenantStatusValue.ACTIVE,
                        TenantStatusValue.SUSPENDED,
                        TenantStatusValue.ACTIVE);
    }

    @Test
    @DisplayName("findAllByTenantIdOrderByVersionDesc: 他テナント混在でも指定tenant_idのみ返す")
    void findAllByTenantIdOrderByVersionDesc_filtersByTenantId() {
        OffsetDateTime v1 = BASE_TIME;
        OffsetDateTime v2 = BASE_TIME.plusHours(1);
        saveEntity("t1", v1, TenantStatusValue.ACTIVE, "t1 initial");
        saveEntity("t2", v1, TenantStatusValue.ACTIVE, "t2 initial");
        saveEntity("t1", v2, TenantStatusValue.INACTIVE, "t1 inactive");
        saveEntity("t2", v2, TenantStatusValue.SUSPENDED, "t2 suspended");

        List<TenantStatus> result =
                sut.findAllByTenantIdOrderByVersionDesc("t1");

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(s -> "t1".equals(s.getTenantId()));
    }

    @Test
    @DisplayName("save: 新規エントリを保存して後続のfindLatestByTenantIdで取得できる")
    void save_persistsStatus() {
        TenantStatus status = new TenantStatus(
                "new-tenant", BASE_TIME,
                TenantStatusValue.ACTIVE,
                "registration",
                BASE_TIME, "creator-user");

        sut.save(status);

        Optional<TenantStatus> result = sut.findLatestByTenantId("new-tenant");
        assertThat(result).isPresent();
        assertThat(result.get().getStatus()).isEqualTo(TenantStatusValue.ACTIVE);
        assertThat(result.get().getReason()).isEqualTo("registration");
        assertThat(result.get().getCreatedBy()).isEqualTo("creator-user");
    }

    /**
     * 直接JPA経由でTenantStatusEntityを保存する（テストデータ投入用）
     */
    private void saveEntity(
            String tenantId, OffsetDateTime version,
            TenantStatusValue status, String reason) {
        TenantStatusEntity entity = new TenantStatusEntity(
                new TenantStatusId(tenantId, version),
                status.name(),
                reason,
                version,
                "creator");
        jpaRepository.save(entity);
    }

}
