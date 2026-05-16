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

import io.github.kizulog_community.kizulog.domain.tenant.model.Tenant;
import io.github.kizulog_community.kizulog.infrastructure.persistence.AbstractRepositoryIT;
import io.github.kizulog_community.kizulog.infrastructure.persistence.tenant.entity.TenantEntity;
import io.github.kizulog_community.kizulog.infrastructure.persistence.tenant.entity.TenantId;

/**
 * TenantRepositoryImpl の統合テスト
 *
 * @author Jun Kobayashi
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import(TenantRepositoryImpl.class)
class TenantRepositoryImplIT extends AbstractRepositoryIT {

    /** テスト対象 */
    @Autowired
    private TenantRepositoryImpl sut;

    /** テストデータ投入用JPAリポジトリ */
    @Autowired
    private TenantJpaRepository jpaRepository;

    /** テスト用基準時刻（UTC） */
    private static final OffsetDateTime BASE_TIME =
            OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

    @BeforeEach
    void setUp() {
        jpaRepository.deleteAll();
    }

    @Test
    @DisplayName("findLatestByTenantId: 同一tenant_idで複数バージョンが存在する場合、最大versionを返す")
    void findLatestByTenantId_returnsLatestVersion_whenMultipleVersionsExist() {
        OffsetDateTime v1 = BASE_TIME;
        OffsetDateTime v2 = BASE_TIME.plusHours(1);
        OffsetDateTime v3 = BASE_TIME.plusHours(2);
        saveEntity("t1", v1, "Name v1", "slug-aaaaaaaaaaaaaaaaa");
        saveEntity("t1", v3, "Name v3", "slug-aaaaaaaaaaaaaaaaa");
        saveEntity("t1", v2, "Name v2", "slug-aaaaaaaaaaaaaaaaa");

        Optional<Tenant> result = sut.findLatestByTenantId("t1");

        assertThat(result).isPresent();
        assertThat(result.get().getTenantId()).isEqualTo("t1");
        assertThat(result.get().getVersion()).isEqualTo(v3);
        assertThat(result.get().getName()).isEqualTo("Name v3");
    }

    @Test
    @DisplayName("findLatestByTenantId: 単一バージョンしか存在しない場合、そのレコードを返す")
    void findLatestByTenantId_returnsSingleVersion_whenOnlyOneExists() {
        saveEntity("t1", BASE_TIME, "Name", "slug-aaaaaaaaaaaaaaaaa");

        Optional<Tenant> result = sut.findLatestByTenantId("t1");

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Name");
    }

    @Test
    @DisplayName("findLatestByTenantId: tenant_idが存在しない場合、空Optionalを返す")
    void findLatestByTenantId_returnsEmpty_whenNotExist() {
        Optional<Tenant> result = sut.findLatestByTenantId("missing");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findLatestBySlug: 存在するslugで最新バージョンのテナントを返す")
    void findLatestBySlug_returnsTenant_whenSlugExists() {
        saveEntity("t1", BASE_TIME, "Name", "target-slug-aaaaaaaaa");

        Optional<Tenant> result = sut.findLatestBySlug("target-slug-aaaaaaaaa");

        assertThat(result).isPresent();
        assertThat(result.get().getTenantId()).isEqualTo("t1");
        assertThat(result.get().getSlug()).isEqualTo("target-slug-aaaaaaaaa");
    }

    @Test
    @DisplayName("findLatestBySlug: 存在しないslugで空Optionalを返す")
    void findLatestBySlug_returnsEmpty_whenSlugNotExist() {
        saveEntity("t1", BASE_TIME, "Name", "other-slug-bbbbbbbbbb");

        Optional<Tenant> result = sut.findLatestBySlug("missing-slug");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findLatestBySlug: 同一tenant_idで複数versionあってもslug指定で1件返す")
    void findLatestBySlug_returnsLatestVersionForSlug() {
        OffsetDateTime v1 = BASE_TIME;
        OffsetDateTime v2 = BASE_TIME.plusHours(1);
        // slug は不変なので両バージョンとも同じ slug
        saveEntity("t1", v1, "Name v1", "fixed-slug-cccccccccccc");
        saveEntity("t1", v2, "Name v2", "fixed-slug-cccccccccccc");

        Optional<Tenant> result = sut.findLatestBySlug("fixed-slug-cccccccccccc");

        assertThat(result).isPresent();
        assertThat(result.get().getTenantId()).isEqualTo("t1");
        assertThat(result.get().getVersion()).isEqualTo(v2);
        assertThat(result.get().getName()).isEqualTo("Name v2");
    }

    @Test
    @DisplayName("findAllLatest: 0件の場合、空リストを返す")
    void findAllLatest_returnsEmptyList_whenNoTenants() {
        List<Tenant> result = sut.findAllLatest();

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findAllLatest: 複数tenantの各最新versionを返す")
    void findAllLatest_returnsLatestPerTenant() {
        OffsetDateTime v1 = BASE_TIME;
        OffsetDateTime v2 = BASE_TIME.plusHours(1);

        saveEntity("t1", v1, "T1 v1", "slug-t1-aaaaaaaaaaaaaa");
        saveEntity("t1", v2, "T1 v2", "slug-t1-aaaaaaaaaaaaaa");
        saveEntity("t2", v1, "T2 v1", "slug-t2-bbbbbbbbbbbbbb");

        List<Tenant> result = sut.findAllLatest();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(Tenant::getTenantId)
                .containsExactlyInAnyOrder("t1", "t2");
        // t1 は v2 が返ること
        Tenant t1Result = result.stream()
                .filter(t -> "t1".equals(t.getTenantId())).findFirst().orElseThrow();
        assertThat(t1Result.getVersion()).isEqualTo(v2);
        assertThat(t1Result.getName()).isEqualTo("T1 v2");
    }

    @Test
    @DisplayName("existsBySlug: 存在するslugでtrue")
    void existsBySlug_returnsTrue_whenSlugExists() {
        saveEntity("t1", BASE_TIME, "Name", "exists-slug-dddddddddd");

        boolean result = sut.existsBySlug("exists-slug-dddddddddd");

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("existsBySlug: 存在しないslugでfalse")
    void existsBySlug_returnsFalse_whenSlugNotExist() {
        saveEntity("t1", BASE_TIME, "Name", "other-slug-eeeeeeeeee");

        boolean result = sut.existsBySlug("missing-slug-fffffffff");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("save: 新規エントリを保存して後続のfindLatestByTenantIdで取得できる")
    void save_persistsTenant() {
        Tenant tenant = new Tenant(
                "new-tenant", BASE_TIME, "New Tenant", "new-slug-gggggggggggg",
                BASE_TIME, "creator-user");

        sut.save(tenant);

        Optional<Tenant> result = sut.findLatestByTenantId("new-tenant");
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("New Tenant");
        assertThat(result.get().getSlug()).isEqualTo("new-slug-gggggggggggg");
        assertThat(result.get().getCreatedBy()).isEqualTo("creator-user");
    }

    /**
     * 直接JPA経由でTenantEntityを保存する（テストデータ投入用）
     */
    private void saveEntity(
            String tenantId, OffsetDateTime version, String name, String slug) {
        TenantEntity entity = new TenantEntity(
                new TenantId(tenantId, version),
                name,
                slug,
                version,
                "creator");
        jpaRepository.save(entity);
    }

}
