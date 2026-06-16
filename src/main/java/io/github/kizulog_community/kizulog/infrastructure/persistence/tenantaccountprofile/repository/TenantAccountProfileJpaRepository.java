package io.github.kizulog_community.kizulog.infrastructure.persistence.tenantaccountprofile.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.github.kizulog_community.kizulog.infrastructure.persistence.tenantaccountprofile.entity.TenantAccountProfileEntity;
import io.github.kizulog_community.kizulog.infrastructure.persistence.tenantaccountprofile.entity.TenantAccountProfileId;

/**
 * テナント利用者プロファイル JPAリポジトリ
 *
 * @author Jun Kobayashi
 */
public interface TenantAccountProfileJpaRepository
        extends JpaRepository<TenantAccountProfileEntity, TenantAccountProfileId> {

    /**
     * identityIdで最新バージョンのプロファイルを取得する。
     *
     * @param identityId Identity ID
     * @return 最新バージョンのEntity。存在しない場合は空のOptional
     */
    @Query("SELECT e FROM TenantAccountProfileEntity e "
            + "WHERE e.id.identityId = :identityId "
            + "AND e.id.version = ("
            + "    SELECT MAX(e2.id.version) FROM TenantAccountProfileEntity e2 "
            + "    WHERE e2.id.identityId = :identityId"
            + ")")
    Optional<TenantAccountProfileEntity> findLatestByIdentityId(
            @Param("identityId") String identityId);

}
