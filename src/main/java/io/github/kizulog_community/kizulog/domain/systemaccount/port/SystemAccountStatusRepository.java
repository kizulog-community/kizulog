package io.github.kizulog_community.kizulog.domain.systemaccount.port;

import java.util.List;
import java.util.Optional;

import io.github.kizulog_community.kizulog.domain.systemaccount.model.SystemAccountStatus;

/**
 * システム管理アカウントステータスリポジトリインターフェース（Output Port）
 *
 * @author Jun Kobayashi
 */
public interface SystemAccountStatusRepository {

    /**
     * accountIdで最新バージョンのステータスを取得する。
     *
     * <p>同一accountIdのレコードの中で、versionが最大のレコードを返す。</p>
     *
     * @param accountId アカウントID
     * @return 最新バージョンのステータス。存在しない場合は空のOptional
     */
    Optional<SystemAccountStatus> findLatestByAccountId(String accountId);

    /**
     * accountIdに紐付くステータス履歴を全件取得する。
     *
     * <p>並び順は version 降順（新しいものが先頭）。
     * 詳細画面のステータス履歴表示で利用する。</p>
     *
     * @param accountId アカウントID
     * @return 履歴リスト（version降順、空リスト返却あり）
     */
    List<SystemAccountStatus> findAllByAccountIdOrderByVersionDesc(String accountId);

    /**
     * システム管理アカウントステータスを保存する。
     *
     * @param systemAccountStatus 保存するステータス
     */
    void save(SystemAccountStatus systemAccountStatus);

}
