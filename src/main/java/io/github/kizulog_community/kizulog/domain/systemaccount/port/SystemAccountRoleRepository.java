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
     * accountIdに紐付く全ロールの最新バージョンを取得する。
     *
     * <p>1アカウントに複数ロールがある場合、各role_id毎の最新を返す。
     * ロールが ACTIVE か INACTIVE かはここでは判定しない（呼び出し側で判定する）</p>
     *
     * @param accountId アカウントID
     * @return ロールのリスト（空の場合あり）
     */
    List<SystemAccountRole> findLatestByAccountId(String accountId);

    /**
     * ロールを保存する。
     *
     * @param systemAccountRole 保存するロール
     */
    void save(SystemAccountRole systemAccountRole);

}
