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

import io.github.kizulog_community.kizulog.domain.tenant.model.TenantHostStatus;
import io.github.kizulog_community.kizulog.domain.tenant.model.TenantHostStatusValue;
import io.github.kizulog_community.kizulog.infrastructure.persistence.AbstractRepositoryIT;
import io.github.kizulog_community.kizulog.infrastructure.persistence.tenant.entity.TenantHostStatusEntity;
import io.github.kizulog_community.kizulog.infrastructure.persistence.tenant.entity.TenantHostStatusId;

/**
 * TenantHostStatusRepositoryImpl の統合テスト
 *
 * @author Jun Kobayashi
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import(TenantHostStatusRepositoryImpl.class)
class TenantHostStatusRepositoryImplIT extends AbstractRepositoryIT {

    /** テスト対象 */
    @Autowired
    private TenantHostStatusRepositoryImpl sut;

    /** テストデータ投入用JPAリポジトリ */
    @Autowired
    private TenantHostStatusJpaRepository jpaRepository;

    /** テスト用基準時刻（UTC） */
    private static final OffsetDateTime BASE_TIME =
            OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

    @BeforeEach
    void setUp() {
        jpaRepository.deleteAll();
    }

    @Test
    @DisplayName("findLatestByTenantIdAndHost: 同一(tenantId,host)で複数versionがある場合、最大versionを返す")
    void findLatestByTenantIdAndHost_returnsLatestVersion_whenMultipleVersionsExist() {
        OffsetDateTime v1 = BASE_TIME;
        OffsetDateTime v2 = BASE_TIME.plusHours(1);
        OffsetDateTime v3 = BASE_TIME.plusHours(2);
        saveEntity("t1", "example.com", v1,
                TenantHostStatusValue.ACTIVE, "initial");
        saveEntity("t1", "example.com", v3,
                TenantHostStatusValue.ACTIVE, "re-enabled");
        saveEntity("t1", "example.com", v2,
                TenantHostStatusValue.INACTIVE, "disabled");

        Optional<TenantHostStatus> result =
                sut.findLatestByTenantIdAndHost("t1", "example.com");

        assertThat(result).isPresent();
        assertThat(result.get().getTenantId()).isEqualTo("t1");
        assertThat(result.get().getHost()).isEqualTo("example.com");
        assertThat(result.get().getVersion()).isEqualTo(v3);
        assertThat(result.get().getStatus()).isEqualTo(TenantHostStatusValue.ACTIVE);
        assertThat(result.get().getReason()).isEqualTo("re-enabled");
    }

    @Test
    @DisplayName("findLatestByTenantIdAndHost: 存在しない(tenantId,host)で空Optionalを返す")
    void findLatestByTenantIdAndHost_returnsEmpty_whenNotExist() {
        Optional<TenantHostStatus> result =
                sut.findLatestByTenantIdAndHost("t1", "missing.example.com");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findLatestByTenantIdAndHost: 同一tenant_idでも別hostは区別される")
    void findLatestByTenantIdAndHost_distinguishesByHost() {
        OffsetDateTime v1 = BASE_TIME;
        saveEntity("t1", "a.example.com", v1,
                TenantHostStatusValue.ACTIVE, "a-reason");
        saveEntity("t1", "b.example.com", v1,
                TenantHostStatusValue.INACTIVE, "b-reason");

        Optional<TenantHostStatus> resultA =
                sut.findLatestByTenantIdAndHost("t1", "a.example.com");
        Optional<TenantHostStatus> resultB =
                sut.findLatestByTenantIdAndHost("t1", "b.example.com");

        assertThat(resultA).isPresent();
        assertThat(resultA.get().getStatus()).isEqualTo(TenantHostStatusValue.ACTIVE);
        assertThat(resultA.get().getReason()).isEqualTo("a-reason");
        assertThat(resultB).isPresent();
        assertThat(resultB.get().getStatus()).isEqualTo(TenantHostStatusValue.INACTIVE);
        assertThat(resultB.get().getReason()).isEqualTo("b-reason");
    }

    @Test
    @DisplayName("findAllByTenantIdAndHostOrderByVersionDesc: 履歴0件で空リスト")
    void findAllByTenantIdAndHostOrderByVersionDesc_returnsEmptyList_whenNoHistory() {
        List<TenantHostStatus> result =
                sut.findAllByTenantIdAndHostOrderByVersionDesc("t1", "missing.example.com");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findAllByTenantIdAndHostOrderByVersionDesc: 全レコードをversion降順で返す")
    void findAllByTenantIdAndHostOrderByVersionDesc_returnsAllInDescOrder() {
        OffsetDateTime v1 = BASE_TIME;
        OffsetDateTime v2 = BASE_TIME.plusHours(1);
        OffsetDateTime v3 = BASE_TIME.plusHours(2);
        saveEntity("t1", "example.com", v1,
                TenantHostStatusValue.ACTIVE, "initial");
        saveEntity("t1", "example.com", v2,
                TenantHostStatusValue.INACTIVE, "disabled");
        saveEntity("t1", "example.com", v3,
                TenantHostStatusValue.ACTIVE, "re-enabled");

        List<TenantHostStatus> result =
                sut.findAllByTenantIdAndHostOrderByVersionDesc("t1", "example.com");

        assertThat(result).hasSize(3);
        assertThat(result).extracting(TenantHostStatus::getVersion)
                .containsExactly(v3, v2, v1);
        assertThat(result).extracting(TenantHostStatus::getStatus)
                .containsExactly(
                        TenantHostStatusValue.ACTIVE,
                        TenantHostStatusValue.INACTIVE,
                        TenantHostStatusValue.ACTIVE);
    }

    @Test
    @DisplayName("findAllByTenantIdAndHostOrderByVersionDesc: 他host混在から指定(tenantId,host)のみ返す")
    void findAllByTenantIdAndHostOrderByVersionDesc_filtersByTenantAndHost() {
        OffsetDateTime v1 = BASE_TIME;
        OffsetDateTime v2 = BASE_TIME.plusHours(1);
        saveEntity("t1", "a.example.com", v1,
                TenantHostStatusValue.ACTIVE, "a-r1");
        saveEntity("t1", "a.example.com", v2,
                TenantHostStatusValue.INACTIVE, "a-r2");
        saveEntity("t1", "b.example.com", v1,
                TenantHostStatusValue.ACTIVE, "b-r1");
        saveEntity("t2", "a.example.com", v1,
                TenantHostStatusValue.ACTIVE, "t2-r1");

        List<TenantHostStatus> result =
                sut.findAllByTenantIdAndHostOrderByVersionDesc("t1", "a.example.com");

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(s ->
                "t1".equals(s.getTenantId()) && "a.example.com".equals(s.getHost()));
        assertThat(result).extracting(TenantHostStatus::getReason)
                .containsExactly("a-r2", "a-r1");
    }

    @Test
    @DisplayName("save: 新規エントリを保存し、status enum と reason が正しく取得できる")
    void save_persistsStatusWithEnumConversion() {
        TenantHostStatus status = new TenantHostStatus(
                "t1", "new.example.com", BASE_TIME,
                TenantHostStatusValue.ACTIVE,
                "registration reason",
                BASE_TIME, "creator-user");

        sut.save(status);

        Optional<TenantHostStatus> result =
                sut.findLatestByTenantIdAndHost("t1", "new.example.com");
        assertThat(result).isPresent();
        assertThat(result.get().getStatus()).isEqualTo(TenantHostStatusValue.ACTIVE);
        assertThat(result.get().getReason()).isEqualTo("registration reason");
        assertThat(result.get().getCreatedBy()).isEqualTo("creator-user");
    }

    /**
     * 直接JPA経由でTenantHostStatusEntityを保存する（テストデータ投入用）
     */
    private void saveEntity(
            String tenantId, String host, OffsetDateTime version,
            TenantHostStatusValue status, String reason) {
        TenantHostStatusEntity entity = new TenantHostStatusEntity(
                new TenantHostStatusId(tenantId, host, version),
                status.name(),
                reason,
                version,
                "creator");
        jpaRepository.save(entity);
    }

}
