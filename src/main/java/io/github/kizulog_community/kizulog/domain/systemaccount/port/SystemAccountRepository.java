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
     * iss/aud/subで最新バージョンのシステム管理アカウントを取得する。
     *
     * <p>同一の(iss, aud, sub)の組み合わせを持つレコードの中で、
     * versionが最大のレコードを返す。</p>
     *
     * @param iss OIDC Issuer
     * @param aud OIDC Audience
     * @param sub OIDC Subject
     * @return 最新バージョンのシステム管理アカウント。存在しない場合は空のOptional
     */
    Optional<SystemAccount> findLatestByIssAndAudAndSub(String iss, String aud, String sub);

    /**
     * システム管理アカウントを保存する。
     *
     * @param systemAccount 保存するアカウント
     */
    void save(SystemAccount systemAccount);

}
