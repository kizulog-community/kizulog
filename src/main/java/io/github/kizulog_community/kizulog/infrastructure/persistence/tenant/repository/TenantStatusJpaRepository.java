package io.github.kizulog_community.kizulog.infrastructure.persistence.tenant.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.github.kizulog_community.kizulog.infrastructure.persistence.tenant.entity.TenantStatusEntity;
import io.github.kizulog_community.kizulog.infrastructure.persistence.tenant.entity.TenantStatusId;

/**
 * 業務テナントステータスJPAリポジトリ
 *
 * @author Jun Kobayashi
 */
public interface TenantStatusJpaRepository
        extends JpaRepository<TenantStatusEntity, TenantStatusId> {

    /**
     * tenantIdで最新バージョンのステータスを取得する。
     *
     * @param tenantId テナントID
     * @return 最新バージョンのステータス。存在しない場合は空のOptional
     */
    @Query("SELECT e FROM TenantStatusEntity e "
            + "WHERE e.id.tenantId = :tenantId "
            + "AND e.id.version = ("
            + "    SELECT MAX(e2.id.version) FROM TenantStatusEntity e2 "
            + "    WHERE e2.id.tenantId = :tenantId"
            + ")")
    Optional<TenantStatusEntity> findLatestByTenantId(
            @Param("tenantId") String tenantId);

    /**
     * tenantIdでステータス変更履歴を新しい順に取得する。
     *
     * @param tenantId テナントID
     * @return ステータス履歴（version降順）
     */
    @Query("SELECT e FROM TenantStatusEntity e "
            + "WHERE e.id.tenantId = :tenantId "
            + "ORDER BY e.id.version DESC")
    List<TenantStatusEntity> findAllByTenantIdOrderByVersionDesc(
            @Param("tenantId") String tenantId);

}
