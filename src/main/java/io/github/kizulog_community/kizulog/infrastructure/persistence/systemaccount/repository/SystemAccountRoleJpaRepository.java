package io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccount.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccount.entity.SystemAccountRoleEntity;
import io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccount.entity.SystemAccountRoleId;

/**
 * @author Jun Kobayashi
 */
public interface SystemAccountRoleJpaRepository
        extends JpaRepository<SystemAccountRoleEntity, SystemAccountRoleId> {

    /*
     * 相関サブクエリで(account_id, role)の組み合わせごとに最新versionを取得する。
     * 内側のs2で外側のs.id.roleを参照することで、role単位での最新を取得できる。
     */
    @Query("""
            SELECT s FROM SystemAccountRoleEntity s
            WHERE s.id.accountId = :accountId
            AND s.id.version = (
                SELECT MAX(s2.id.version)
                FROM SystemAccountRoleEntity s2
                WHERE s2.id.accountId = :accountId
                AND s2.id.role = s.id.role
            )
            """)
    List<SystemAccountRoleEntity> findLatestByAccountId(
            @Param("accountId") String accountId);

}
