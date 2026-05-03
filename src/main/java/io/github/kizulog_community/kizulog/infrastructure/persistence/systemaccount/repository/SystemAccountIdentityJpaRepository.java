package io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccount.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccount.entity.SystemAccountIdentityEntity;
import io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccount.entity.SystemAccountIdentityId;

/**
 * システム管理アカウント認証方法JPAリポジトリ
 *
 * @author Jun Kobayashi
 */
public interface SystemAccountIdentityJpaRepository
        extends JpaRepository<SystemAccountIdentityEntity, SystemAccountIdentityId> {

    /**
     * iss/aud/subで最新バージョンの認証方法を取得する。
     *
     * <p>同一(iss, aud, sub)を持つレコードの中で、versionが最大のものを返す。</p>
     */
    @Query("SELECT e FROM SystemAccountIdentityEntity e "
            + "WHERE e.iss = :iss AND e.aud = :aud AND e.sub = :sub "
            + "AND e.id.version = ("
            + "    SELECT MAX(e2.id.version) FROM SystemAccountIdentityEntity e2 "
            + "    WHERE e2.id.identityId = e.id.identityId"
            + ") "
            + "AND e.id.identityId IN ("
            + "    SELECT e3.id.identityId FROM SystemAccountIdentityEntity e3 "
            + "    WHERE e3.iss = :iss AND e3.aud = :aud AND e3.sub = :sub"
            + ")")
    Optional<SystemAccountIdentityEntity> findLatestByIssAndAudAndSub(
            @Param("iss") String iss,
            @Param("aud") String aud,
            @Param("sub") String sub);

    /**
     * identityIdで最新バージョンを取得する。
     */
    @Query("SELECT e FROM SystemAccountIdentityEntity e "
            + "WHERE e.id.identityId = :identityId "
            + "AND e.id.version = ("
            + "    SELECT MAX(e2.id.version) FROM SystemAccountIdentityEntity e2 "
            + "    WHERE e2.id.identityId = :identityId"
            + ")")
    Optional<SystemAccountIdentityEntity> findLatestByIdentityId(
            @Param("identityId") String identityId);

    /**
     * accountIdに紐付く全認証方法の最新バージョンを取得する。
     *
     * <p>1つのaccountに複数の認証方法がある場合、各認証方法毎の最新を返す。
     * 同じ認証方法がversion違いで複数あっても、最新のみが返る。</p>
     */
    @Query("SELECT e FROM SystemAccountIdentityEntity e "
            + "WHERE e.accountId = :accountId "
            + "AND e.id.version = ("
            + "    SELECT MAX(e2.id.version) FROM SystemAccountIdentityEntity e2 "
            + "    WHERE e2.id.identityId = e.id.identityId"
            + ")")
    List<SystemAccountIdentityEntity> findLatestByAccountId(
            @Param("accountId") String accountId);

}
