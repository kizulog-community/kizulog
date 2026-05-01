package io.github.kizulog_community.kizulog.domain.systemaccount.port;

import io.github.kizulog_community.kizulog.domain.systemaccount.model.SystemAccountRole;

/**
 * システム管理アカウントロールリポジトリインターフェース（Output Port）
 *
 * @author Jun Kobayashi
 */
public interface SystemAccountRoleRepository {

    /**
     * システム管理アカウントロールを保存する。
     *
     * @param systemAccountRole 保存するロール
     */
    void save(SystemAccountRole systemAccountRole);

}