package io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccountlocalization.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccountlocalization.entity.SystemAccountLocalizationEntity;
import io.github.kizulog_community.kizulog.infrastructure.persistence.systemaccountlocalization.entity.SystemAccountLocalizationId;

/**
 * システム管理アカウント言語・タイムゾーン設定JPAリポジトリ
 *
 * @author Jun Kobayashi
 */
public interface SystemAccountLocalizationJpaRepository
        extends JpaRepository<SystemAccountLocalizationEntity, SystemAccountLocalizationId> {

    /**
     * accountIdで最新バージョンの設定を取得する。
     */
    @Query("SELECT e FROM SystemAccountLocalizationEntity e "
            + "WHERE e.id.accountId = :accountId "
            + "AND e.id.version = ("
            + "    SELECT MAX(e2.id.version) FROM SystemAccountLocalizationEntity e2 "
            + "    WHERE e2.id.accountId = :accountId"
            + ")")
    Optional<SystemAccountLocalizationEntity> findLatestByAccountId(
            @Param("accountId") String accountId);

    /**
     * 指定された言語コードを最新バージョンで使用しているaccountIdの一覧を取得する。
     */
    @Query("SELECT e.id.accountId FROM SystemAccountLocalizationEntity e "
            + "WHERE e.languageCode = :languageCode "
            + "AND e.id.version = ("
            + "    SELECT MAX(e2.id.version) FROM SystemAccountLocalizationEntity e2 "
            + "    WHERE e2.id.accountId = e.id.accountId"
            + ")")
    List<String> findAccountIdsUsingLanguage(@Param("languageCode") String languageCode);

    /**
     * 指定されたタイムゾーンIDを最新バージョンで使用しているaccountIdの一覧を取得する。
     */
    @Query("SELECT e.id.accountId FROM SystemAccountLocalizationEntity e "
            + "WHERE e.timezoneId = :timezoneId "
            + "AND e.id.version = ("
            + "    SELECT MAX(e2.id.version) FROM SystemAccountLocalizationEntity e2 "
            + "    WHERE e2.id.accountId = e.id.accountId"
            + ")")
    List<String> findAccountIdsUsingTimezone(@Param("timezoneId") String timezoneId);

}
