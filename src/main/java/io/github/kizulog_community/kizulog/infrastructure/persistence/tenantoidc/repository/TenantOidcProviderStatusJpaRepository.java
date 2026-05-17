package io.github.kizulog_community.kizulog.infrastructure.persistence.tenantoidc.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.github.kizulog_community.kizulog.domain.tenantoidc.model.TenantOidcProviderStatusValue;
import io.github.kizulog_community.kizulog.infrastructure.persistence.tenantoidc.entity.TenantOidcProviderStatusEntity;
import io.github.kizulog_community.kizulog.infrastructure.persistence.tenantoidc.entity.TenantOidcProviderStatusId;

/**
 * 業務テナントOIDCプロバイダーステータスJPAリポジトリ
 *
 * @author Jun Kobayashi
 */
public interface TenantOidcProviderStatusJpaRepository
        extends JpaRepository<TenantOidcProviderStatusEntity, TenantOidcProviderStatusId> {

    /**
     * (tenantId, providerId) で最新バージョンのステータスを取得する。
     */
    @Query("SELECT e FROM TenantOidcProviderStatusEntity e "
            + "WHERE e.id.tenantId = :tenantId "
            + "AND e.id.providerId = :providerId "
            + "AND e.id.version = ("
            + "    SELECT MAX(e2.id.version) FROM TenantOidcProviderStatusEntity e2 "
            + "    WHERE e2.id.tenantId = :tenantId "
            + "    AND e2.id.providerId = :providerId"
            + ")")
    Optional<TenantOidcProviderStatusEntity> findLatestByTenantIdAndProviderId(
            @Param("tenantId") String tenantId,
            @Param("providerId") String providerId);

    /**
     * (tenantId, providerId) の全ステータス履歴をversion降順で取得する。
     */
    @Query("SELECT e FROM TenantOidcProviderStatusEntity e "
            + "WHERE e.id.tenantId = :tenantId "
            + "AND e.id.providerId = :providerId "
            + "ORDER BY e.id.version DESC")
    List<TenantOidcProviderStatusEntity> findAllByTenantIdAndProviderIdOrderByVersionDesc(
            @Param("tenantId") String tenantId,
            @Param("providerId") String providerId);

    /**
     * 指定tenantIdのプロバイダーで、最新versionが指定ステータスのものを取得する。
     */
    @Query("SELECT e FROM TenantOidcProviderStatusEntity e "
            + "WHERE e.id.tenantId = :tenantId "
            + "AND e.status = :status "
            + "AND e.id.version = ("
            + "    SELECT MAX(e2.id.version) FROM TenantOidcProviderStatusEntity e2 "
            + "    WHERE e2.id.tenantId = :tenantId "
            + "    AND e2.id.providerId = e.id.providerId"
            + ")")
    List<TenantOidcProviderStatusEntity> findAllLatestByTenantIdAndStatus(
            @Param("tenantId") String tenantId,
            @Param("status") TenantOidcProviderStatusValue status);

}
