package io.github.kizulog_community.kizulog.domain.systemaccount.port;

import java.util.Optional;

import io.github.kizulog_community.kizulog.domain.systemaccount.model.SystemAccountStatus;

/**
 * システム管理アカウントステータスリポジトリインターフェース（Output Port）
 *
 * @author Jun Kobayashi
 */
public interface SystemAccountStatusRepository {

    /**
     * accountIdで最新バージョンのステータスを取得する。
     *
     * <p>同一accountIdのレコードの中で、versionが最大のレコードを返す。</p>
     *
     * @param accountId アカウントID
     * @return 最新バージョンのステータス。存在しない場合は空のOptional
     */
    Optional<SystemAccountStatus> findLatestByAccountId(String accountId);

    /**
     * システム管理アカウントステータスを保存する。
     *
     * @param systemAccountStatus 保存するステータス
     */
    void save(SystemAccountStatus systemAccountStatus);

}
