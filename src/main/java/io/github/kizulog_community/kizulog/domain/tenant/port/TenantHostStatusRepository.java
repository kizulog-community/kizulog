package io.github.kizulog_community.kizulog.domain.tenant.port;

import java.util.List;
import java.util.Optional;

import io.github.kizulog_community.kizulog.domain.tenant.model.TenantHostStatus;

/**
 * 業務テナント識別ホストステータスリポジトリインターフェース（Output Port）
 *
 * @author Jun Kobayashi
 */
public interface TenantHostStatusRepository {

    /**
     * (tenantId, host) で最新バージョンのステータスを取得する。
     *
     * @param tenantId テナントID
     * @param host ホスト名
     * @return 最新バージョンのステータス。存在しない場合は空のOptional
     */
    Optional<TenantHostStatus> findLatestByTenantIdAndHost(String tenantId, String host);

    /**
     * (tenantId, host) でステータス変更履歴を新しい順に全件取得する。
     *
     * @param tenantId テナントID
     * @param host ホスト名
     * @return ステータス履歴（version降順）
     */
    List<TenantHostStatus> findAllByTenantIdAndHostOrderByVersionDesc(
            String tenantId, String host);

    /**
     * ステータスを保存する。
     *
     * @param status 保存するステータス
     */
    void save(TenantHostStatus status);

}
