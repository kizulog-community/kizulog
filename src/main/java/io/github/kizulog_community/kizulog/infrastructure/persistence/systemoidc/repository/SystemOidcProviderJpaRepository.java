package io.github.kizulog_community.kizulog.infrastructure.persistence.systemoidc.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.github.kizulog_community.kizulog.infrastructure.persistence.systemoidc.entity.SystemOidcProviderEntity;
import io.github.kizulog_community.kizulog.infrastructure.persistence.systemoidc.entity.SystemOidcProviderId;

/**
 * システムOIDCプロバイダーJPAリポジトリ
 *
 * @author Jun Kobayashi
 */
public interface SystemOidcProviderJpaRepository
        extends JpaRepository<SystemOidcProviderEntity, SystemOidcProviderId> {

    /**
     * provider_idで最新バージョンのプロバイダーを取得する。
     *
     * <p>同一provider_idを持つレコードの中で、versionが最大のものを返す。</p>
     */
    @Query("SELECT e FROM SystemOidcProviderEntity e "
            + "WHERE e.id.providerId = :providerId "
            + "AND e.id.version = ("
            + "    SELECT MAX(e2.id.version) FROM SystemOidcProviderEntity e2 "
            + "    WHERE e2.id.providerId = :providerId"
            + ")")
    Optional<SystemOidcProviderEntity> findLatestByProviderId(
            @Param("providerId") String providerId);

    /**
     * 全プロバイダーの最新バージョンを取得する。
     *
     * <p>各provider_idの最新versionのレコードを返す。</p>
     */
    @Query("SELECT e FROM SystemOidcProviderEntity e "
            + "WHERE e.id.version = ("
            + "    SELECT MAX(e2.id.version) FROM SystemOidcProviderEntity e2 "
            + "    WHERE e2.id.providerId = e.id.providerId"
            + ")")
    List<SystemOidcProviderEntity> findAllLatest();

    /**
     * provider_idが過去含めて存在するかを判定する。
     */
    @Query("SELECT COUNT(e) > 0 FROM SystemOidcProviderEntity e "
            + "WHERE e.id.providerId = :providerId")
    boolean existsByProviderIdAcrossAllVersions(
            @Param("providerId") String providerId);

}
