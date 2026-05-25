package io.github.kizulog_community.kizulog.infrastructure.persistence.tenantaccount.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.github.kizulog_community.kizulog.infrastructure.persistence.tenantaccount.entity.TenantAccountStatusEntity;
import io.github.kizulog_community.kizulog.infrastructure.persistence.tenantaccount.entity.TenantAccountStatusId;

/**
 * 業務テナントアカウントステータスJPAリポジトリ
 *
 * @author Jun Kobayashi
 */
public interface TenantAccountStatusJpaRepository
        extends JpaRepository<TenantAccountStatusEntity, TenantAccountStatusId> {

    /**
     * account_id で最新バージョンのステータスを取得する。
     */
    @Query("""
            SELECT s FROM TenantAccountStatusEntity s
            WHERE s.id.accountId = :accountId
            AND s.id.version = (
                SELECT MAX(s2.id.version)
                FROM TenantAccountStatusEntity s2
                WHERE s2.id.accountId = :accountId
            )
            """)
    Optional<TenantAccountStatusEntity> findLatestByAccountId(
            @Param("accountId") String accountId);

    /**
     * 指定 account_id のステータス履歴を全件 version 降順で取得する。
     */
    @Query("""
            SELECT s FROM TenantAccountStatusEntity s
            WHERE s.id.accountId = :accountId
            ORDER BY s.id.version DESC
            """)
    List<TenantAccountStatusEntity> findAllByAccountIdOrderByVersionDesc(
            @Param("accountId") String accountId);

}
