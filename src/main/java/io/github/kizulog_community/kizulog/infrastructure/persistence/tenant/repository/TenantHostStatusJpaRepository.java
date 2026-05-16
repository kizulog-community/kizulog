package io.github.kizulog_community.kizulog.infrastructure.persistence.tenant.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.github.kizulog_community.kizulog.infrastructure.persistence.tenant.entity.TenantHostStatusEntity;
import io.github.kizulog_community.kizulog.infrastructure.persistence.tenant.entity.TenantHostStatusId;

/**
 * 業務テナント識別ホストステータスJPAリポジトリ
 *
 * @author Jun Kobayashi
 */
public interface TenantHostStatusJpaRepository
        extends JpaRepository<TenantHostStatusEntity, TenantHostStatusId> {

    /**
     * (tenantId, host) で最新バージョンのステータスを取得する。
     *
     * @param tenantId テナントID
     * @param host ホスト名
     * @return 最新バージョンのステータス。存在しない場合は空のOptional
     */
    @Query("SELECT e FROM TenantHostStatusEntity e "
            + "WHERE e.id.tenantId = :tenantId "
            + "AND e.id.host = :host "
            + "AND e.id.version = ("
            + "    SELECT MAX(e2.id.version) FROM TenantHostStatusEntity e2 "
            + "    WHERE e2.id.tenantId = :tenantId "
            + "    AND e2.id.host = :host"
            + ")")
    Optional<TenantHostStatusEntity> findLatestByTenantIdAndHost(
            @Param("tenantId") String tenantId,
            @Param("host") String host);

    /**
     * (tenantId, host) でステータス変更履歴を新しい順に取得する。
     *
     * @param tenantId テナントID
     * @param host ホスト名
     * @return ステータス履歴（version降順）
     */
    @Query("SELECT e FROM TenantHostStatusEntity e "
            + "WHERE e.id.tenantId = :tenantId "
            + "AND e.id.host = :host "
            + "ORDER BY e.id.version DESC")
    List<TenantHostStatusEntity> findAllByTenantIdAndHostOrderByVersionDesc(
            @Param("tenantId") String tenantId,
            @Param("host") String host);

}
