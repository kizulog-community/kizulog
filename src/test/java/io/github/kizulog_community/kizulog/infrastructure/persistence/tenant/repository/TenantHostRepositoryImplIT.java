package io.github.kizulog_community.kizulog.infrastructure.persistence.tenant.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

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

import io.github.kizulog_community.kizulog.domain.tenant.model.TenantHost;
import io.github.kizulog_community.kizulog.infrastructure.persistence.AbstractRepositoryIT;
import io.github.kizulog_community.kizulog.infrastructure.persistence.tenant.entity.TenantHostEntity;
import io.github.kizulog_community.kizulog.infrastructure.persistence.tenant.entity.TenantHostId;

/**
 * TenantHostRepositoryImpl の統合テスト
 *
 * @author Jun Kobayashi
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import(TenantHostRepositoryImpl.class)
class TenantHostRepositoryImplIT extends AbstractRepositoryIT {

    /** テスト対象 */
    @Autowired
    private TenantHostRepositoryImpl sut;

    /** テストデータ投入用JPAリポジトリ */
    @Autowired
    private TenantHostJpaRepository jpaRepository;

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
        saveEntity("t1", "example.com", v1);
        saveEntity("t1", "example.com", v3);
        saveEntity("t1", "example.com", v2);

        Optional<TenantHost> result =
                sut.findLatestByTenantIdAndHost("t1", "example.com");

        assertThat(result).isPresent();
        assertThat(result.get().getTenantId()).isEqualTo("t1");
        assertThat(result.get().getHost()).isEqualTo("example.com");
        assertThat(result.get().getVersion()).isEqualTo(v3);
    }

    @Test
    @DisplayName("findLatestByTenantIdAndHost: 同一tenant_idでも別hostは別レコードとして区別される")
    void findLatestByTenantIdAndHost_distinguishesByHost() {
        OffsetDateTime v1 = BASE_TIME;
        OffsetDateTime v2 = BASE_TIME.plusHours(1);
        saveEntity("t1", "a.example.com", v1);
        saveEntity("t1", "b.example.com", v2);

        Optional<TenantHost> resultA =
                sut.findLatestByTenantIdAndHost("t1", "a.example.com");
        Optional<TenantHost> resultB =
                sut.findLatestByTenantIdAndHost("t1", "b.example.com");

        assertThat(resultA).isPresent();
        assertThat(resultA.get().getVersion()).isEqualTo(v1);
        assertThat(resultB).isPresent();
        assertThat(resultB.get().getVersion()).isEqualTo(v2);
    }

    @Test
    @DisplayName("findLatestByTenantIdAndHost: 存在しない(tenantId,host)で空Optionalを返す")
    void findLatestByTenantIdAndHost_returnsEmpty_whenNotExist() {
        saveEntity("t1", "example.com", BASE_TIME);

        Optional<TenantHost> result =
                sut.findLatestByTenantIdAndHost("t1", "missing.example.com");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findAllLatestByTenantId: 0件の場合、空リストを返す")
    void findAllLatestByTenantId_returnsEmptyList_whenNoHosts() {
        List<TenantHost> result = sut.findAllLatestByTenantId("missing");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findAllLatestByTenantId: 同一tenant_idの複数hostで各最新versionを返す")
    void findAllLatestByTenantId_returnsLatestPerHost() {
        OffsetDateTime v1 = BASE_TIME;
        OffsetDateTime v2 = BASE_TIME.plusHours(1);
        saveEntity("t1", "a.example.com", v1);
        saveEntity("t1", "a.example.com", v2); // a の最新は v2
        saveEntity("t1", "b.example.com", v1);

        List<TenantHost> result = sut.findAllLatestByTenantId("t1");

        assertThat(result).hasSize(2);
        assertThat(result).extracting(TenantHost::getHost, TenantHost::getVersion)
                .containsExactlyInAnyOrder(
                        tuple("a.example.com", v2),
                        tuple("b.example.com", v1));
    }

    @Test
    @DisplayName("findAllLatestByTenantId: 他テナント混在環境で指定tenant_idのみ返す")
    void findAllLatestByTenantId_filtersByTenantId() {
        OffsetDateTime v1 = BASE_TIME;
        saveEntity("t1", "a.example.com", v1);
        saveEntity("t2", "b.example.com", v1);
        saveEntity("t1", "c.example.com", v1);

        List<TenantHost> result = sut.findAllLatestByTenantId("t1");

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(h -> "t1".equals(h.getTenantId()));
        assertThat(result).extracting(TenantHost::getHost)
                .containsExactlyInAnyOrder("a.example.com", "c.example.com");
    }

    @Test
    @DisplayName("findAllLatestByHost: 同一hostに複数tenantが紐づく場合、全tenantの最新を返す（要件2）")
    void findAllLatestByHost_returnsAllTenantsForSharedHost() {
        OffsetDateTime v1 = BASE_TIME;
        saveEntity("t1", "shared.example.com", v1);
        saveEntity("t2", "shared.example.com", v1);
        saveEntity("t3", "shared.example.com", v1);

        List<TenantHost> result = sut.findAllLatestByHost("shared.example.com");

        assertThat(result).hasSize(3);
        assertThat(result).extracting(TenantHost::getTenantId)
                .containsExactlyInAnyOrder("t1", "t2", "t3");
    }

    @Test
    @DisplayName("findAllLatestByHost: 同一(tenantId,host)で複数versionがある場合、最新のみ返す")
    void findAllLatestByHost_returnsLatestVersionPerTenant() {
        OffsetDateTime v1 = BASE_TIME;
        OffsetDateTime v2 = BASE_TIME.plusHours(1);
        saveEntity("t1", "shared.example.com", v1);
        saveEntity("t1", "shared.example.com", v2); // t1 の最新は v2
        saveEntity("t2", "shared.example.com", v1);

        List<TenantHost> result = sut.findAllLatestByHost("shared.example.com");

        assertThat(result).hasSize(2);
        assertThat(result).extracting(TenantHost::getTenantId, TenantHost::getVersion)
                .containsExactlyInAnyOrder(
                        tuple("t1", v2),
                        tuple("t2", v1));
    }

    @Test
    @DisplayName("findAllLatestByHost: 存在しないhostで空リストを返す")
    void findAllLatestByHost_returnsEmptyList_whenHostNotExist() {
        saveEntity("t1", "other.example.com", BASE_TIME);

        List<TenantHost> result = sut.findAllLatestByHost("missing.example.com");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("existsByTenantIdAndHost: 存在する(tenantId,host)でtrue")
    void existsByTenantIdAndHost_returnsTrue_whenExists() {
        saveEntity("t1", "exists.example.com", BASE_TIME);

        boolean result = sut.existsByTenantIdAndHost("t1", "exists.example.com");

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("existsByTenantIdAndHost: 存在しない(tenantId,host)でfalse")
    void existsByTenantIdAndHost_returnsFalse_whenNotExists() {
        saveEntity("t1", "other.example.com", BASE_TIME);

        boolean result =
                sut.existsByTenantIdAndHost("t1", "missing.example.com");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("existsByTenantIdAndHost: 別テナントの同一hostはfalse（要件2）")
    void existsByTenantIdAndHost_returnsFalse_whenSameHostDifferentTenant() {
        saveEntity("t1", "shared.example.com", BASE_TIME);

        boolean result =
                sut.existsByTenantIdAndHost("t2", "shared.example.com");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("save: 新規エントリを保存して後続の検索で取得できる")
    void save_persistsTenantHost() {
        TenantHost tenantHost = new TenantHost(
                "t1", "new.example.com", BASE_TIME,
                BASE_TIME, "creator-user");

        sut.save(tenantHost);

        Optional<TenantHost> result =
                sut.findLatestByTenantIdAndHost("t1", "new.example.com");
        assertThat(result).isPresent();
        assertThat(result.get().getCreatedBy()).isEqualTo("creator-user");
    }

    /**
     * 直接JPA経由でTenantHostEntityを保存する（テストデータ投入用）
     */
    private void saveEntity(
            String tenantId, String host, OffsetDateTime version) {
        TenantHostEntity entity = new TenantHostEntity(
                new TenantHostId(tenantId, host, version),
                version,
                "creator");
        jpaRepository.save(entity);
    }

}
