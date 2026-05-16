package io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccount.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccount.entity.SystemAccountEntity;
import io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccount.entity.SystemAccountId;

/**
 * システム管理アカウントJPAリポジトリ
 *
 * @author Jun Kobayashi
 */
public interface SystemAccountJpaRepository
        extends JpaRepository<SystemAccountEntity, SystemAccountId> {

    /**
     * accountIdで最新バージョンのアカウントを取得する。
     *
     * @param accountId アカウントID
     * @return 最新バージョンのアカウント
     */
    @Query("SELECT e FROM SystemAccountEntity e "
            + "WHERE e.id.accountId = :accountId "
            + "AND e.id.version = ("
            + "    SELECT MAX(e2.id.version) FROM SystemAccountEntity e2 "
            + "    WHERE e2.id.accountId = :accountId"
            + ")")
    Optional<SystemAccountEntity> findLatestByAccountId(
            @Param("accountId") String accountId);

    /**
     * 全アカウントの最新バージョンを取得する。
     *
     * <p>各 account_id の最新 version のレコードを返す。</p>
     */
    @Query("SELECT e FROM SystemAccountEntity e "
            + "WHERE e.id.version = ("
            + "    SELECT MAX(e2.id.version) FROM SystemAccountEntity e2 "
            + "    WHERE e2.id.accountId = e.id.accountId"
            + ")")
    List<SystemAccountEntity> findAllLatest();

}
