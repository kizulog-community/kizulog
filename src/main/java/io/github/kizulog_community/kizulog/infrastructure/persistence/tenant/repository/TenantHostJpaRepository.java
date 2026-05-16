package io.github.kizulog_community.kizulog.infrastructure.persistence.tenant.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.github.kizulog_community.kizulog.infrastructure.persistence.tenant.entity.TenantHostEntity;
import io.github.kizulog_community.kizulog.infrastructure.persistence.tenant.entity.TenantHostId;

/**
 * 業務テナント識別ホストJPAリポジトリ
 *
 * @author Jun Kobayashi
 */
public interface TenantHostJpaRepository
        extends JpaRepository<TenantHostEntity, TenantHostId> {

    /**
     * (tenantId, host) で最新バージョンのhost定義を取得する。
     *
     * @param tenantId テナントID
     * @param host ホスト名
     * @return 最新バージョンのhost定義。存在しない場合は空のOptional
     */
    @Query("SELECT e FROM TenantHostEntity e "
            + "WHERE e.id.tenantId = :tenantId "
            + "AND e.id.host = :host "
            + "AND e.id.version = ("
            + "    SELECT MAX(e2.id.version) FROM TenantHostEntity e2 "
            + "    WHERE e2.id.tenantId = :tenantId "
            + "    AND e2.id.host = :host"
            + ")")
    Optional<TenantHostEntity> findLatestByTenantIdAndHost(
            @Param("tenantId") String tenantId,
            @Param("host") String host);

    /**
     * 指定tenantIdに紐づく全hostの最新バージョンを取得する。
     *
     * @param tenantId テナントID
     * @return 各 (tenantId, host) の最新バージョンのリスト
     */
    @Query("SELECT e FROM TenantHostEntity e "
            + "WHERE e.id.tenantId = :tenantId "
            + "AND e.id.version = ("
            + "    SELECT MAX(e2.id.version) FROM TenantHostEntity e2 "
            + "    WHERE e2.id.tenantId = e.id.tenantId "
            + "    AND e2.id.host = e.id.host"
            + ")")
    List<TenantHostEntity> findAllLatestByTenantId(
            @Param("tenantId") String tenantId);

    /**
     * 指定hostに紐づく全tenant_idの最新バージョンを取得する。
     *
     * @param host ホスト名
     * @return 該当する (tenantId, host) の最新バージョンのリスト
     */
    @Query("SELECT e FROM TenantHostEntity e "
            + "WHERE e.id.host = :host "
            + "AND e.id.version = ("
            + "    SELECT MAX(e2.id.version) FROM TenantHostEntity e2 "
            + "    WHERE e2.id.tenantId = e.id.tenantId "
            + "    AND e2.id.host = e.id.host"
            + ")")
    List<TenantHostEntity> findAllLatestByHost(@Param("host") String host);

    /**
     * (tenantId, host) の組み合わせが過去含めて存在するかを判定する。
     *
     * @param tenantId テナントID
     * @param host ホスト名
     * @return 存在すればtrue
     */
    @Query("SELECT COUNT(e) > 0 FROM TenantHostEntity e "
            + "WHERE e.id.tenantId = :tenantId "
            + "AND e.id.host = :host")
    boolean existsByTenantIdAndHostAcrossAllVersions(
            @Param("tenantId") String tenantId,
            @Param("host") String host);

}
