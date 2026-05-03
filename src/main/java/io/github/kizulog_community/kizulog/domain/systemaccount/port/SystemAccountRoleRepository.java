package io.github.kizulog_community.kizulog.domain.systemaccount.port;

import java.util.List;

import io.github.kizulog_community.kizulog.domain.systemaccount.model.SystemAccountRole;

/**
 * システム管理アカウントロールリポジトリインターフェース（Output Port）
 *
 * @author Jun Kobayashi
 */
public interface SystemAccountRoleRepository {

    /**
     * accountIdで最新バージョンのロール一覧を取得する。
     *
     * <p>同一accountIdに紐づくレコードの中で、{@code (account_id, role)}の
     * 組み合わせごとに最新versionのレコードを返す。
     * 1つのアカウントに複数のロールが付与されている場合、すべて返却される。</p>
     *
     * @param accountId アカウントID
     * @return 最新バージョンのロール一覧。該当なしの場合は空リスト
     */
    List<SystemAccountRole> findLatestByAccountId(String accountId);

    /**
     * システム管理アカウントロールを保存する。
     *
     * @param systemAccountRole 保存するロール
     */
    void save(SystemAccountRole systemAccountRole);

}
