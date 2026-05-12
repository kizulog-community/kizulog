package io.github.kizulog_community.kizulog.infrastructure.persistence.systemoidc.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.github.kizulog_community.kizulog.domain.systemoidc.model.OidcProviderStatusValue;
import io.github.kizulog_community.kizulog.infrastructure.persistence.systemoidc.entity.SystemOidcProviderStatusEntity;
import io.github.kizulog_community.kizulog.infrastructure.persistence.systemoidc.entity.SystemOidcProviderStatusId;

/**
 * システムOIDCプロバイダーステータスJPAリポジトリ
 *
 * @author Jun Kobayashi
 */
public interface SystemOidcProviderStatusJpaRepository
        extends JpaRepository<SystemOidcProviderStatusEntity, SystemOidcProviderStatusId> {

    /**
     * provider_idで最新バージョンのステータスを取得する。
     */
    @Query("SELECT e FROM SystemOidcProviderStatusEntity e "
            + "WHERE e.id.providerId = :providerId "
            + "AND e.id.version = ("
            + "    SELECT MAX(e2.id.version) FROM SystemOidcProviderStatusEntity e2 "
            + "    WHERE e2.id.providerId = :providerId"
            + ")")
    Optional<SystemOidcProviderStatusEntity> findLatestByProviderId(
            @Param("providerId") String providerId);

    /**
     * 指定ステータスを持つ全プロバイダーIDを取得する（最新バージョンの判定）
     *
     * <p>各provider_idの最新versionが指定ステータスのものに絞り込んで、
     * provider_idのリストを返す。</p>
     */
    @Query("SELECT e.id.providerId FROM SystemOidcProviderStatusEntity e "
            + "WHERE e.status = :status "
            + "AND e.id.version = ("
            + "    SELECT MAX(e2.id.version) FROM SystemOidcProviderStatusEntity e2 "
            + "    WHERE e2.id.providerId = e.id.providerId"
            + ")")
    List<String> findProviderIdsByLatestStatus(
            @Param("status") OidcProviderStatusValue status);

}
