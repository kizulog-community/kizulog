package io.github.kizulog_community.kizulog.domain.tenantoidc.port;

import java.util.List;
import java.util.Optional;

import io.github.kizulog_community.kizulog.domain.tenantoidc.model.TenantOidcProviderStatus;

/**
 * 業務テナントOIDCプロバイダーステータスリポジトリ（Output Port）
 *
 * @author Jun Kobayashi
 */
public interface TenantOidcProviderStatusRepository {

    /**
     * 指定(tenantId, providerId)で最新バージョンのステータスを取得する。
     *
     * @param tenantId テナントID
     * @param providerId プロバイダー識別子
     * @return 最新ステータス、存在しなければ空Optional
     */
    Optional<TenantOidcProviderStatus> findLatestByTenantIdAndProviderId(
            String tenantId, String providerId);

    /**
     * 指定(tenantId, providerId)の全ステータス履歴をversion降順で取得する。
     *
     * @param tenantId テナントID
     * @param providerId プロバイダー識別子
     * @return ステータス履歴リスト
     */
    List<TenantOidcProviderStatus> findAllByTenantIdAndProviderIdOrderByVersionDesc(
            String tenantId, String providerId);

    /**
     * 指定tenant_idの全プロバイダー中、最新ステータスがENABLEDのものを取得する。
     *
     * @param tenantId テナントID
     * @return ENABLEDステータスのリスト
     */
    List<TenantOidcProviderStatus> findAllLatestEnabledByTenantId(String tenantId);

    /**
     * ステータスを保存する（新規 / 新version追加）。
     *
     * @param status 保存するステータス
     */
    void save(TenantOidcProviderStatus status);

}
