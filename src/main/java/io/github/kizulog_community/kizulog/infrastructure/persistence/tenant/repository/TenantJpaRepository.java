package io.github.kizulog_community.kizulog.infrastructure.persistence.tenant.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.github.kizulog_community.kizulog.infrastructure.persistence.tenant.entity.TenantEntity;
import io.github.kizulog_community.kizulog.infrastructure.persistence.tenant.entity.TenantId;

/**
 * 業務テナントJPAリポジトリ
 *
 * @author Jun Kobayashi
 */
public interface TenantJpaRepository
        extends JpaRepository<TenantEntity, TenantId> {

    /**
     * tenantIdで最新バージョンのテナントを取得する。
     *
     * @param tenantId テナントID
     * @return 最新バージョンのテナント。存在しない場合は空のOptional
     */
    @Query("SELECT e FROM TenantEntity e "
            + "WHERE e.id.tenantId = :tenantId "
            + "AND e.id.version = ("
            + "    SELECT MAX(e2.id.version) FROM TenantEntity e2 "
            + "    WHERE e2.id.tenantId = :tenantId"
            + ")")
    Optional<TenantEntity> findLatestByTenantId(
            @Param("tenantId") String tenantId);

    /**
     * slugで最新バージョンのテナントを取得する。
     *
     * @param slug URL用slug
     * @return 該当する最新バージョンのテナント。存在しない場合は空のOptional
     */
    @Query("SELECT e FROM TenantEntity e "
            + "WHERE e.slug = :slug "
            + "AND e.id.version = ("
            + "    SELECT MAX(e2.id.version) FROM TenantEntity e2 "
            + "    WHERE e2.id.tenantId = e.id.tenantId"
            + ")")
    Optional<TenantEntity> findLatestBySlug(@Param("slug") String slug);

    /**
     * 全テナントの最新バージョンを取得する。
     *
     * @return 各tenantIdの最新バージョンのリスト
     */
    @Query("SELECT e FROM TenantEntity e "
            + "WHERE e.id.version = ("
            + "    SELECT MAX(e2.id.version) FROM TenantEntity e2 "
            + "    WHERE e2.id.tenantId = e.id.tenantId"
            + ")")
    List<TenantEntity> findAllLatest();

    /**
     * 指定slugが過去含めて存在するかを判定する。
     *
     * @param slug URL用slug
     * @return 存在すればtrue
     */
    @Query("SELECT COUNT(e) > 0 FROM TenantEntity e "
            + "WHERE e.slug = :slug")
    boolean existsBySlugAcrossAllVersions(@Param("slug") String slug);

}
