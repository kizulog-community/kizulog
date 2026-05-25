package io.github.kizulog_community.kizulog.infrastructure.persistence.tenantaccount.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.github.kizulog_community.kizulog.infrastructure.persistence.tenantaccount.entity.TenantAccountIdentityEntity;
import io.github.kizulog_community.kizulog.infrastructure.persistence.tenantaccount.entity.TenantAccountIdentityId;

/**
 * 業務テナントアカウント認証方法JPAリポジトリ
 *
 * @author Jun Kobayashi
 */
public interface TenantAccountIdentityJpaRepository
        extends JpaRepository<TenantAccountIdentityEntity, TenantAccountIdentityId> {

    /**
     * テナント境界つきで iss/aud/sub から最新バージョンの認証方法を取得する。
     */
    @Query("SELECT e FROM TenantAccountIdentityEntity e "
            + "WHERE e.tenantId = :tenantId "
            + "AND e.iss = :iss AND e.aud = :aud AND e.sub = :sub "
            + "AND e.id.version = ("
            + "    SELECT MAX(e2.id.version) FROM TenantAccountIdentityEntity e2 "
            + "    WHERE e2.id.identityId = e.id.identityId"
            + ") "
            + "AND e.id.identityId IN ("
            + "    SELECT e3.id.identityId FROM TenantAccountIdentityEntity e3 "
            + "    WHERE e3.tenantId = :tenantId "
            + "    AND e3.iss = :iss AND e3.aud = :aud AND e3.sub = :sub"
            + ")")
    Optional<TenantAccountIdentityEntity> findLatestByTenantIdAndIssAndAudAndSub(
            @Param("tenantId") String tenantId,
            @Param("iss") String iss,
            @Param("aud") String aud,
            @Param("sub") String sub);

    /**
     * identity_id で最新バージョンを取得する。
     */
    @Query("SELECT e FROM TenantAccountIdentityEntity e "
            + "WHERE e.id.identityId = :identityId "
            + "AND e.id.version = ("
            + "    SELECT MAX(e2.id.version) FROM TenantAccountIdentityEntity e2 "
            + "    WHERE e2.id.identityId = :identityId"
            + ")")
    Optional<TenantAccountIdentityEntity> findLatestByIdentityId(
            @Param("identityId") String identityId);

    /**
     * account_id に紐付く全認証方法の最新バージョンを取得する。
     */
    @Query("SELECT e FROM TenantAccountIdentityEntity e "
            + "WHERE e.accountId = :accountId "
            + "AND e.id.version = ("
            + "    SELECT MAX(e2.id.version) FROM TenantAccountIdentityEntity e2 "
            + "    WHERE e2.id.identityId = e.id.identityId"
            + ")")
    List<TenantAccountIdentityEntity> findLatestByAccountId(
            @Param("accountId") String accountId);

    /**
     * 指定テナント・指定 Issuer URI に紐付く ACTIVE な identity の件数を取得する。
     */
    @Query("SELECT COUNT(e) FROM TenantAccountIdentityEntity e "
            + "WHERE e.tenantId = :tenantId "
            + "AND e.iss = :iss "
            + "AND e.id.version = ("
            + "    SELECT MAX(e2.id.version) FROM TenantAccountIdentityEntity e2 "
            + "    WHERE e2.id.identityId = e.id.identityId"
            + ") "
            + "AND EXISTS ("
            + "    SELECT 1 FROM TenantAccountIdentityStatusEntity s "
            + "    WHERE s.id.identityId = e.id.identityId "
            + "    AND s.status = io.github.kizulog_community.kizulog.domain.tenantaccount.model.TenantAccountStatusValue.ACTIVE "
            + "    AND s.id.version = ("
            + "        SELECT MAX(s2.id.version) FROM TenantAccountIdentityStatusEntity s2 "
            + "        WHERE s2.id.identityId = e.id.identityId"
            + "    )"
            + ")")
    int countActiveByTenantIdAndIss(
            @Param("tenantId") String tenantId,
            @Param("iss") String iss);

}
