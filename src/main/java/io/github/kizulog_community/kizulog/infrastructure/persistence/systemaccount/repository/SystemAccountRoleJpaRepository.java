package io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccount.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccount.entity.SystemAccountRoleEntity;
import io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccount.entity.SystemAccountRoleId;

/**
 * システム管理アカウントロールJPAリポジトリ
 *
 * @author Jun Kobayashi
 */
public interface SystemAccountRoleJpaRepository
        extends JpaRepository<SystemAccountRoleEntity, SystemAccountRoleId> {

    /**
     * accountIdに紐付く全ロールの最新バージョンを取得する。
     *
     * <p>1つのaccountに複数のロール紐付け(role_id)がある場合、
     * 各role_id毎の最新バージョンを返す。</p>
     */
    @Query("SELECT e FROM SystemAccountRoleEntity e "
            + "WHERE e.accountId = :accountId "
            + "AND e.id.version = ("
            + "    SELECT MAX(e2.id.version) FROM SystemAccountRoleEntity e2 "
            + "    WHERE e2.id.roleId = e.id.roleId"
            + ")")
    List<SystemAccountRoleEntity> findLatestByAccountId(
            @Param("accountId") String accountId);

}
