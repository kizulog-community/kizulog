package io.github.kizulog_community.kizulog.domain.tenantaccount.port;

import java.util.Optional;

import io.github.kizulog_community.kizulog.domain.tenantaccount.model.TenantAccountRoleStatus;

/**
 * 業務テナントアカウントロールステータスリポジトリインターフェース（Output Port）
 *
 * @author Jun Kobayashi
 */
public interface TenantAccountRoleStatusRepository {

    /**
     * role_id で最新バージョンのステータスを取得する。
     *
     * @param roleId ロールID
     * @return 最新バージョンのステータス。存在しない場合は空の Optional
     */
    Optional<TenantAccountRoleStatus> findLatestByRoleId(String roleId);

    /**
     * ステータスを保存する。
     *
     * @param status 保存するステータス
     */
    void save(TenantAccountRoleStatus status);

}
