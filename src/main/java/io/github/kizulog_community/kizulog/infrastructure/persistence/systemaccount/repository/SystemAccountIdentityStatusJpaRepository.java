package io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccount.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccount.entity.SystemAccountIdentityStatusEntity;
import io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccount.entity.SystemAccountIdentityStatusId;

/**
 * システム管理アカウント認証方法ステータスJPAリポジトリ
 *
 * @author Jun Kobayashi
 */
public interface SystemAccountIdentityStatusJpaRepository
        extends JpaRepository<SystemAccountIdentityStatusEntity, SystemAccountIdentityStatusId> {

    /**
     * identityIdで最新バージョンのステータスを取得する。
     */
    @Query("SELECT e FROM SystemAccountIdentityStatusEntity e "
            + "WHERE e.id.identityId = :identityId "
            + "AND e.id.version = ("
            + "    SELECT MAX(e2.id.version) FROM SystemAccountIdentityStatusEntity e2 "
            + "    WHERE e2.id.identityId = :identityId"
            + ")")
    Optional<SystemAccountIdentityStatusEntity> findLatestByIdentityId(
            @Param("identityId") String identityId);

}
