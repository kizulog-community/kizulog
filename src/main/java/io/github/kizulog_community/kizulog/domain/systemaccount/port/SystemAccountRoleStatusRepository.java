package io.github.kizulog_community.kizulog.domain.systemaccount.port;

import java.util.Optional;

import io.github.kizulog_community.kizulog.domain.systemaccount.model.SystemAccountRoleStatus;

/**
 * システム管理アカウントロールステータスリポジトリインターフェース（Output Port）
 *
 * @author Jun Kobayashi
 */
public interface SystemAccountRoleStatusRepository {

    /**
     * roleIdで最新バージョンのステータスを取得する。
     *
     * @param roleId ロールID
     * @return 最新バージョンのステータス。存在しない場合は空のOptional
     */
    Optional<SystemAccountRoleStatus> findLatestByRoleId(String roleId);

    /**
     * ステータスを保存する。
     *
     * @param status 保存するステータス
     */
    void save(SystemAccountRoleStatus status);

}
