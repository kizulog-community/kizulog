package io.github.kizulog_community.kizulog.domain.tenantaccountprofile.port;

import java.util.Optional;

import io.github.kizulog_community.kizulog.domain.tenantaccountprofile.model.TenantAccountProfile;

/**
 * テナント利用者プロファイルリポジトリインターフェース（Output Port）
 *
 * @author Jun Kobayashi
 */
public interface TenantAccountProfileRepository {

    /**
     * identityIdで最新バージョンのプロファイルを取得する。
     *
     * @param identityId Identity ID
     * @return 最新バージョンのプロファイル。存在しない場合は空のOptional
     */
    Optional<TenantAccountProfile> findLatestByIdentityId(String identityId);

    /**
     * プロファイルを保存する。
     *
     * @param profile 保存するプロファイル
     */
    void save(TenantAccountProfile profile);

}
