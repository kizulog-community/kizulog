package io.github.kizulog_community.kizulog.domain.tenantoidc.port;

import java.util.List;
import java.util.Optional;

import io.github.kizulog_community.kizulog.domain.tenantoidc.model.TenantOidcProvider;

/**
 * 業務テナントOIDCプロバイダーリポジトリ（Output Port）
 *
 * @author Jun Kobayashi
 */
public interface TenantOidcProviderRepository {

    /**
     * 指定tenant_idとprovider_idで最新バージョンのプロバイダーを取得する。
     *
     * @param tenantId テナントID
     * @param providerId プロバイダー識別子
     * @return 最新version、存在しなければ空Optional
     */
    Optional<TenantOidcProvider> findLatestByTenantIdAndProviderId(
            String tenantId, String providerId);

    /**
     * 指定tenant_idの全プロバイダー（各provider_idの最新version）を取得する。
     *
     * @param tenantId テナントID
     * @return プロバイダーリスト（順序は保証しない）
     */
    List<TenantOidcProvider> findAllLatestByTenantId(String tenantId);

    /**
     * 指定(iss, aud)に紐づく全プロバイダーの最新versionを取得する。
     *
     * @param iss OIDC Issuer URI
     * @param aud Audience
     * @return プロバイダーリスト（順序は保証しない）
     */
    List<TenantOidcProvider> findAllLatestByIssAndAud(String iss, String aud);

    /**
     * 同一tenant_id内でprovider_idが過去含めて存在するかを判定する。
     *
     * @param tenantId テナントID
     * @param providerId プロバイダー識別子
     * @return 存在すればtrue
     */
    boolean existsByTenantIdAndProviderId(String tenantId, String providerId);

    /**
     * プロバイダーを保存する（新規 / 新version追加）
     *
     * @param provider 保存するプロバイダー
     */
    void save(TenantOidcProvider provider);

}
