package io.github.kizulog_community.kizulog.infrastructure.persistence.tenantaccount.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.github.kizulog_community.kizulog.infrastructure.persistence.tenantaccount.entity.TenantAccountIdentityStatusEntity;
import io.github.kizulog_community.kizulog.infrastructure.persistence.tenantaccount.entity.TenantAccountIdentityStatusId;

/**
 * 業務テナントアカウント認証方法ステータスJPAリポジトリ
 *
 * @author Jun Kobayashi
 */
public interface TenantAccountIdentityStatusJpaRepository
        extends JpaRepository<TenantAccountIdentityStatusEntity, TenantAccountIdentityStatusId> {

    /**
     * identity_id で最新バージョンのステータスを取得する。
     */
    @Query("SELECT e FROM TenantAccountIdentityStatusEntity e "
            + "WHERE e.id.identityId = :identityId "
            + "AND e.id.version = ("
            + "    SELECT MAX(e2.id.version) FROM TenantAccountIdentityStatusEntity e2 "
            + "    WHERE e2.id.identityId = :identityId"
            + ")")
    Optional<TenantAccountIdentityStatusEntity> findLatestByIdentityId(
            @Param("identityId") String identityId);

}
