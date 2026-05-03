package io.github.kizulog_community.kizulog.domain.systemaccount.port;

import java.util.Optional;

import io.github.kizulog_community.kizulog.domain.systemaccount.model.SystemAccount;

/**
 * システム管理アカウントリポジトリインターフェース（Output Port）
 *
 * @author Jun Kobayashi
 */
public interface SystemAccountRepository {

    /**
     * accountIdで最新バージョンのシステム管理アカウントを取得する。
     *
     * @param accountId アカウントID
     * @return 最新バージョンのシステム管理アカウント。存在しない場合は空のOptional
     */
    Optional<SystemAccount> findLatestByAccountId(String accountId);

    /**
     * システム管理アカウントを保存する。
     *
     * @param systemAccount 保存するアカウント
     */
    void save(SystemAccount systemAccount);

}
