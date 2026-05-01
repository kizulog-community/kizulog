package io.github.kizulog_community.kizulog.domain.systemaccount.port;

import io.github.kizulog_community.kizulog.domain.systemaccount.model.SystemAccountStatus;

/**
 * システム管理アカウントステータスリポジトリインターフェース（Output Port）
 *
 * @author Jun Kobayashi
 */
public interface SystemAccountStatusRepository {

    /**
     * システム管理アカウントステータスを保存する。
     *
     * @param systemAccountStatus 保存するステータス
     */
    void save(SystemAccountStatus systemAccountStatus);

}