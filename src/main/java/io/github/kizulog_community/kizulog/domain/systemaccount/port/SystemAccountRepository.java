package io.github.kizulog_community.kizulog.domain.systemaccount.port;

import java.util.List;
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
     * 全システム管理アカウントの最新バージョンを取得する。
     *
     * <p>各 account_id 毎の最新 version のレコードを返す。
     * アカウント一覧画面で利用する。</p>
     *
     * @return 各accountIdの最新バージョンのアカウント（空リスト返却あり）
     */
    List<SystemAccount> findAllLatest();

    /**
     * システム管理アカウントを保存する。
     *
     * @param systemAccount 保存するアカウント
     */
    void save(SystemAccount systemAccount);

}