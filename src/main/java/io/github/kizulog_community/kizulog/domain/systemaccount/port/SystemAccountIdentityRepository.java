package io.github.kizulog_community.kizulog.domain.systemaccount.port;

import java.util.List;
import java.util.Optional;

import io.github.kizulog_community.kizulog.domain.systemaccount.model.SystemAccountIdentity;

/**
 * システム管理アカウント認証方法リポジトリインターフェース（Output Port）
 *
 * @author Jun Kobayashi
 */
public interface SystemAccountIdentityRepository {

    /**
     * iss/aud/subで最新バージョンのidentityを取得する。
     *
     * <p>同一の(iss, aud, sub)の組み合わせを持つレコードの中で、
     * versionが最大のレコードを返す。</p>
     *
     * @param iss OIDC Issuer
     * @param aud OIDC Audience
     * @param sub OIDC Subject
     * @return 最新バージョンのシステム管理アカウント認証方法
     */
    Optional<SystemAccountIdentity> findLatestByIssAndAudAndSub(
            String iss, String aud, String sub);

    /**
     * identityIdで最新バージョンのシステム管理アカウント認証方法を取得する。
     *
     * @param identityId アイデンティティID
     * @return 最新バージョンのシステム管理アカウント認証方法
     */
    Optional<SystemAccountIdentity> findLatestByIdentityId(String identityId);

    /**
     * accountIdに紐付く全システム管理アカウント認証方法の最新バージョンを取得する。
     *
     * <p>1アカウントに複数のシステム管理アカウント認証方法がある場合、
     * 各identity_id毎の最新を返す。</p>
     *
     * @param accountId アカウントID
     * @return システム管理アカウント認証方法のリスト（空の場合あり）
     */
    List<SystemAccountIdentity> findLatestByAccountId(String accountId);

    /**
     * システム管理アカウント認証方法を保存する。
     *
     * @param identity 保存するシステム管理アカウント認証方法
     */
    void save(SystemAccountIdentity identity);

}
