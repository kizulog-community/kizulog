package io.github.kizulog_community.kizulog.infrastructure.persistence.tenantaccount.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.github.kizulog_community.kizulog.infrastructure.persistence.tenantaccount.entity.TenantAccountRoleEntity;
import io.github.kizulog_community.kizulog.infrastructure.persistence.tenantaccount.entity.TenantAccountRoleId;

/**
 * 業務テナントアカウントロールJPAリポジトリ
 *
 * @author Jun Kobayashi
 */
public interface TenantAccountRoleJpaRepository
        extends JpaRepository<TenantAccountRoleEntity, TenantAccountRoleId> {

    /**
     * account_id に紐付く全ロールの最新バージョンを取得する。
     */
    @Query("SELECT e FROM TenantAccountRoleEntity e "
            + "WHERE e.accountId = :accountId "
            + "AND e.id.version = ("
            + "    SELECT MAX(e2.id.version) FROM TenantAccountRoleEntity e2 "
            + "    WHERE e2.id.roleId = e.id.roleId"
            + ")")
    List<TenantAccountRoleEntity> findLatestByAccountId(
            @Param("accountId") String accountId);

}
