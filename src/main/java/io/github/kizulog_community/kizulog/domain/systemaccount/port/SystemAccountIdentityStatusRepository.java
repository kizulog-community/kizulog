package io.github.kizulog_community.kizulog.domain.systemaccount.port;

import java.util.Optional;

import io.github.kizulog_community.kizulog.domain.systemaccount.model.SystemAccountIdentityStatus;

/**
 * システム管理アカウント認証方法ステータスリポジトリインターフェース（Output Port）
 *
 * @author Jun Kobayashi
 */
public interface SystemAccountIdentityStatusRepository {

    /**
     * identityIdで最新バージョンのステータスを取得する。
     *
     * @param identityId アイデンティティID
     * @return 最新バージョンのステータス。存在しない場合は空のOptional
     */
    Optional<SystemAccountIdentityStatus> findLatestByIdentityId(String identityId);

    /**
     * ステータスを保存する。
     *
     * @param status 保存するステータス
     */
    void save(SystemAccountIdentityStatus status);

}
