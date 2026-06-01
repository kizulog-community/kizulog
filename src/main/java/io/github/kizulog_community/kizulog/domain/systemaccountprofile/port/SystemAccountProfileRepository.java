package io.github.kizulog_community.kizulog.domain.systemaccountprofile.port;

import java.util.Optional;

import io.github.kizulog_community.kizulog.domain.systemaccountprofile.model.SystemAccountProfile;

/**
 * システム管理アカウントプロファイルリポジトリインターフェース（Output Port）
 *
 * @author Jun Kobayashi
 */
public interface SystemAccountProfileRepository {

    /**
     * identityIdで最新バージョンのプロファイルを取得する。
     *
     * @param identityId Identity ID
     * @return 最新バージョンのプロファイル。存在しない場合は空のOptional
     */
    Optional<SystemAccountProfile> findLatestByIdentityId(String identityId);

    /**
     * プロファイルを保存する。
     *
     * @param profile 保存するプロファイル
     */
    void save(SystemAccountProfile profile);

}
