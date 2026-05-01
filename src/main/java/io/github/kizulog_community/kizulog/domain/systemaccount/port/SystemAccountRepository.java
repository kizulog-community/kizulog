package io.github.kizulog_community.kizulog.domain.systemaccount.port;

import io.github.kizulog_community.kizulog.domain.systemaccount.model.SystemAccount;

/**
 * システム管理アカウントリポジトリインターフェース（Output Port）
 *
 * @author Jun Kobayashi
 */
public interface SystemAccountRepository {

    /**
     * システム管理アカウントを保存する。
     *
     * @param systemAccount 保存するアカウント
     */
    void save(SystemAccount systemAccount);

}