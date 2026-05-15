package io.github.kizulog_community.kizulog.domain.systemaccountlocalization.port;

import java.util.List;
import java.util.Optional;

import io.github.kizulog_community.kizulog.domain.systemaccountlocalization.model.SystemAccountLocalization;

/**
 * システム管理アカウント言語・タイムゾーン設定リポジトリインターフェース（Output Port）
 *
 * @author Jun Kobayashi
 */
public interface SystemAccountLocalizationRepository {

    /**
     * accountIdで最新バージョンのアカウント言語・タイムゾーン設定を取得する。
     *
     * @param accountId アカウントID
     * @return 最新バージョンの設定。存在しない場合は空のOptional
     */
    Optional<SystemAccountLocalization> findLatestByAccountId(String accountId);

    /**
     * 指定された言語コードを最新バージョンで使用しているaccountIdの一覧を取得する。
     *
     * @param languageCode BCP47準拠の言語コード（例："ja"、"en"）
     * @return account_idのリスト（一致なしの場合は空リスト）
     */
    List<String> findAccountIdsUsingLanguage(String languageCode);

    /**
     * 指定されたタイムゾーンIDを最新バージョンで使用しているaccountIdの一覧を取得する。
     *
     * @param timezoneId IANA TZ database準拠のタイムゾーンID（例："Asia/Tokyo"）
     * @return account_idのリスト（一致なしの場合は空リスト）
     */
    List<String> findAccountIdsUsingTimezone(String timezoneId);

    /**
     * アカウント言語・タイムゾーン設定を保存する。
     *
     * @param localization 保存する設定
     */
    void save(SystemAccountLocalization localization);

}
