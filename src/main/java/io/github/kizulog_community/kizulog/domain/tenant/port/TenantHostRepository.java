package io.github.kizulog_community.kizulog.domain.tenant.port;

import java.util.List;
import java.util.Optional;

import io.github.kizulog_community.kizulog.domain.tenant.model.TenantHost;

/**
 * 業務テナント識別ホストリポジトリインターフェース（Output Port）
 *
 * @author Jun Kobayashi
 */
public interface TenantHostRepository {

    /**
     * (tenantId, host) で最新バージョンのhost定義を取得する。
     *
     * @param tenantId テナントID
     * @param host ホスト名
     * @return 最新バージョンのhost定義。存在しない場合は空のOptional
     */
    Optional<TenantHost> findLatestByTenantIdAndHost(String tenantId, String host);

    /**
     * 指定tenantIdに紐づく全hostの最新バージョンを取得する。
     *
     * @param tenantId テナントID
     * @return 各 (tenantId, host) の最新バージョンのリスト
     */
    List<TenantHost> findAllLatestByTenantId(String tenantId);

    /**
     * 指定hostに紐づく全tenant_idの最新バージョンを取得する。
     *
     * @param host ホスト名
     * @return 該当する (tenantId, host) の最新バージョンのリスト
     */
    List<TenantHost> findAllLatestByHost(String host);

    /**
     * (tenantId, host) の組が過去含めて存在するかを判定する。
     *
     * @param tenantId テナントID
     * @param host ホスト名
     * @return 存在すればtrue
     */
    boolean existsByTenantIdAndHost(String tenantId, String host);

    /**
     * host定義を保存する。
     *
     * @param tenantHost 保存するhost定義
     */
    void save(TenantHost tenantHost);

}
