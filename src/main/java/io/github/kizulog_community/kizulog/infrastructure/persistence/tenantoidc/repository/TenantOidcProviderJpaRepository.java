package io.github.kizulog_community.kizulog.infrastructure.persistence.tenantoidc.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.github.kizulog_community.kizulog.infrastructure.persistence.tenantoidc.entity.TenantOidcProviderEntity;
import io.github.kizulog_community.kizulog.infrastructure.persistence.tenantoidc.entity.TenantOidcProviderId;

/**
 * 業務テナントOIDCプロバイダーJPAリポジトリ
 *
 * @author Jun Kobayashi
 */
public interface TenantOidcProviderJpaRepository
        extends JpaRepository<TenantOidcProviderEntity, TenantOidcProviderId> {

    /**
     * (tenantId, providerId) で最新バージョンのプロバイダーを取得する。
     */
    @Query("SELECT e FROM TenantOidcProviderEntity e "
            + "WHERE e.id.tenantId = :tenantId "
            + "AND e.id.providerId = :providerId "
            + "AND e.id.version = ("
            + "    SELECT MAX(e2.id.version) FROM TenantOidcProviderEntity e2 "
            + "    WHERE e2.id.tenantId = :tenantId "
            + "    AND e2.id.providerId = :providerId"
            + ")")
    Optional<TenantOidcProviderEntity> findLatestByTenantIdAndProviderId(
            @Param("tenantId") String tenantId,
            @Param("providerId") String providerId);

    /**
     * 指定tenantIdの全プロバイダー（各provider_idの最新version）を取得する。
     */
    @Query("SELECT e FROM TenantOidcProviderEntity e "
            + "WHERE e.id.tenantId = :tenantId "
            + "AND e.id.version = ("
            + "    SELECT MAX(e2.id.version) FROM TenantOidcProviderEntity e2 "
            + "    WHERE e2.id.tenantId = :tenantId "
            + "    AND e2.id.providerId = e.id.providerId"
            + ")")
    List<TenantOidcProviderEntity> findAllLatestByTenantId(
            @Param("tenantId") String tenantId);

    /**
     * 指定(iss, aud)に紐づく全テナントの最新プロバイダーを取得する。
     */
    @Query("SELECT e FROM TenantOidcProviderEntity e "
            + "WHERE e.iss = :iss "
            + "AND e.aud = :aud "
            + "AND e.id.version = ("
            + "    SELECT MAX(e2.id.version) FROM TenantOidcProviderEntity e2 "
            + "    WHERE e2.id.tenantId = e.id.tenantId "
            + "    AND e2.id.providerId = e.id.providerId"
            + ")")
    List<TenantOidcProviderEntity> findAllLatestByIssAndAud(
            @Param("iss") String iss,
            @Param("aud") String aud);

    /**
     * (tenantId, providerId)が過去含めて存在するかを判定する。
     */
    @Query("SELECT COUNT(e) > 0 FROM TenantOidcProviderEntity e "
            + "WHERE e.id.tenantId = :tenantId "
            + "AND e.id.providerId = :providerId")
    boolean existsByTenantIdAndProviderIdAcrossAllVersions(
            @Param("tenantId") String tenantId,
            @Param("providerId") String providerId);

}
