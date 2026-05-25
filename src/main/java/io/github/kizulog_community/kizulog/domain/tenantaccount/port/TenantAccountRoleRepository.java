package io.github.kizulog_community.kizulog.domain.tenantaccount.port;

import java.util.List;

import io.github.kizulog_community.kizulog.domain.tenantaccount.model.TenantAccountRole;

/**
 * 業務テナントアカウントロールリポジトリインターフェース（Output Port）
 *
 * @author Jun Kobayashi
 */
public interface TenantAccountRoleRepository {

    /**
     * account_id に紐付く全ロールの最新バージョンを取得する。
     *
     * @param accountId アカウントID
     * @return ロールのリスト（空の場合あり）
     */
    List<TenantAccountRole> findLatestByAccountId(String accountId);

    /**
     * ロールを保存する。
     *
     * @param tenantAccountRole 保存するロール
     */
    void save(TenantAccountRole tenantAccountRole);

}
