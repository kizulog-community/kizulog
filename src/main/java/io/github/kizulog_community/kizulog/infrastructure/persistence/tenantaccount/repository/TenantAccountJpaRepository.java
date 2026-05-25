package io.github.kizulog_community.kizulog.infrastructure.persistence.tenantaccount.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.github.kizulog_community.kizulog.infrastructure.persistence.tenantaccount.entity.TenantAccountEntity;
import io.github.kizulog_community.kizulog.infrastructure.persistence.tenantaccount.entity.TenantAccountId;

/**
 * 業務テナントアカウントJPAリポジトリ
 *
 * @author Jun Kobayashi
 */
public interface TenantAccountJpaRepository
        extends JpaRepository<TenantAccountEntity, TenantAccountId> {

    /**
     * account_id で最新バージョンのアカウントを取得する。
     */
    @Query("SELECT e FROM TenantAccountEntity e "
            + "WHERE e.id.accountId = :accountId "
            + "AND e.id.version = ("
            + "    SELECT MAX(e2.id.version) FROM TenantAccountEntity e2 "
            + "    WHERE e2.id.accountId = :accountId"
            + ")")
    Optional<TenantAccountEntity> findLatestByAccountId(
            @Param("accountId") String accountId);

    /**
     * 指定テナントに所属する全アカウントの最新バージョンを取得する。
     */
    @Query("SELECT e FROM TenantAccountEntity e "
            + "WHERE e.tenantId = :tenantId "
            + "AND e.id.version = ("
            + "    SELECT MAX(e2.id.version) FROM TenantAccountEntity e2 "
            + "    WHERE e2.id.accountId = e.id.accountId"
            + ")")
    List<TenantAccountEntity> findAllLatestByTenantId(
            @Param("tenantId") String tenantId);

}
