package io.github.kizulog_community.kizulog.domain.tenant.port;

import java.util.List;
import java.util.Optional;

import io.github.kizulog_community.kizulog.domain.tenant.model.TenantStatus;

/**
 * 業務テナントステータスリポジトリインターフェース（Output Port）
 *
 * @author Jun Kobayashi
 */
public interface TenantStatusRepository {

    /**
     * tenantIdで最新バージョンのステータスを取得する。
     *
     * @param tenantId テナントID
     * @return 最新バージョンのステータス。存在しない場合は空のOptional
     */
    Optional<TenantStatus> findLatestByTenantId(String tenantId);

    /**
     * tenantIdでステータス変更履歴を新しい順に全件取得する。
     *
     * @param tenantId テナントID
     * @return ステータス履歴（version降順）
     */
    List<TenantStatus> findAllByTenantIdOrderByVersionDesc(String tenantId);

    /**
     * ステータスを保存する。
     *
     * @param status 保存するステータス
     */
    void save(TenantStatus status);

}
