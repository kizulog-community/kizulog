package io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccountprofile.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccountprofile.entity.SystemAccountProfileEntity;
import io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccountprofile.entity.SystemAccountProfileId;

/**
 * システム管理アカウントプロファイル JPAリポジトリ
 *
 * @author Jun Kobayashi
 */
public interface SystemAccountProfileJpaRepository
        extends JpaRepository<SystemAccountProfileEntity, SystemAccountProfileId> {

    /**
     * identityIdで最新バージョンのプロファイルを取得する。
     *
     * @param identityId Identity ID
     * @return 最新バージョンのEntity。存在しない場合は空のOptional
     */
    @Query("SELECT e FROM SystemAccountProfileEntity e "
            + "WHERE e.id.identityId = :identityId "
            + "AND e.id.version = ("
            + "    SELECT MAX(e2.id.version) FROM SystemAccountProfileEntity e2 "
            + "    WHERE e2.id.identityId = :identityId"
            + ")")
    Optional<SystemAccountProfileEntity> findLatestByIdentityId(
            @Param("identityId") String identityId);

}
