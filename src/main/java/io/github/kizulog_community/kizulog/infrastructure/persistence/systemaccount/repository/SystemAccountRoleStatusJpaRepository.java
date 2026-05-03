package io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccount.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccount.entity.SystemAccountRoleStatusEntity;
import io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccount.entity.SystemAccountRoleStatusId;

/**
 * システム管理アカウントロールステータスJPAリポジトリ
 *
 * @author Jun Kobayashi
 */
public interface SystemAccountRoleStatusJpaRepository
        extends JpaRepository<SystemAccountRoleStatusEntity, SystemAccountRoleStatusId> {

    /**
     * roleIdで最新バージョンのステータスを取得する。
     */
    @Query("SELECT e FROM SystemAccountRoleStatusEntity e "
            + "WHERE e.id.roleId = :roleId "
            + "AND e.id.version = ("
            + "    SELECT MAX(e2.id.version) FROM SystemAccountRoleStatusEntity e2 "
            + "    WHERE e2.id.roleId = :roleId"
            + ")")
    Optional<SystemAccountRoleStatusEntity> findLatestByRoleId(
            @Param("roleId") String roleId);

}
