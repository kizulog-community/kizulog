package io.github.kizulog_community.kizulog.infrastructure.persistence.tenantaccount.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.github.kizulog_community.kizulog.infrastructure.persistence.tenantaccount.entity.TenantAccountRoleStatusEntity;
import io.github.kizulog_community.kizulog.infrastructure.persistence.tenantaccount.entity.TenantAccountRoleStatusId;

/**
 * 業務テナントアカウントロールステータスJPAリポジトリ
 *
 * @author Jun Kobayashi
 */
public interface TenantAccountRoleStatusJpaRepository
        extends JpaRepository<TenantAccountRoleStatusEntity, TenantAccountRoleStatusId> {

    /**
     * role_id で最新バージョンのステータスを取得する。
     */
    @Query("SELECT e FROM TenantAccountRoleStatusEntity e "
            + "WHERE e.id.roleId = :roleId "
            + "AND e.id.version = ("
            + "    SELECT MAX(e2.id.version) FROM TenantAccountRoleStatusEntity e2 "
            + "    WHERE e2.id.roleId = :roleId"
            + ")")
    Optional<TenantAccountRoleStatusEntity> findLatestByRoleId(
            @Param("roleId") String roleId);

}
