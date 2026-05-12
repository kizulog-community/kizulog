package io.github.kizulog_community.kizulog.domain.systemoidc.port;

import java.util.List;
import java.util.Optional;

import io.github.kizulog_community.kizulog.domain.systemoidc.model.SystemOidcProvider;

/**
 * システムOIDCプロバイダーリポジトリインターフェース（Output Port）
 *
 * @author Jun Kobayashi
 */
public interface SystemOidcProviderRepository {

    /**
     * provider_idで最新バージョンのOIDCプロバイダーを取得する。
     *
     * @param providerId プロバイダーID
     * @return 最新バージョンのOIDCプロバイダー
     */
    Optional<SystemOidcProvider> findLatestByProviderId(String providerId);

    /**
     * 全OIDCプロバイダーの最新バージョンを取得する。
     *
     * @return OIDCプロバイダーのリスト
     */
    List<SystemOidcProvider> findAllLatest();

    /**
     * 指定したprovider_idが過去含めて存在するかを判定する。
     *
     * @param providerId プロバイダーID
     * @return 存在すればtrue
     */
    boolean existsByProviderId(String providerId);

    /**
     * OIDCプロバイダーが何らかのproviderIdで1件以上登録されているかを判定する。
     *
     * @return 1件以上存在すればtrue
     */
    boolean existsAny();

    /**
     * OIDCプロバイダーを保存する。
     *
     * @param provider 保存するOIDCプロバイダー
     */
    void save(SystemOidcProvider provider);

}
