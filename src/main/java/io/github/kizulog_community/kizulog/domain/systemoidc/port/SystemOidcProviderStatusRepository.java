package io.github.kizulog_community.kizulog.domain.systemoidc.port;

import java.util.List;
import java.util.Optional;

import io.github.kizulog_community.kizulog.domain.systemoidc.model.OidcProviderStatusValue;
import io.github.kizulog_community.kizulog.domain.systemoidc.model.SystemOidcProviderStatus;

/**
 * システムOIDCプロバイダーステータスリポジトリインターフェース（Output Port）
 *
 * @author Jun Kobayashi
 */
public interface SystemOidcProviderStatusRepository {

    /**
     * provider_idで最新バージョンのステータスを取得する。
     *
     * @param providerId プロバイダーID
     * @return 最新バージョンのステータス
     */
    Optional<SystemOidcProviderStatus> findLatestByProviderId(String providerId);

    /**
     * 指定ステータスの全プロバイダーIDを取得する（最新バージョンの判定）
     *
     * <p>「ENABLED な全プロバイダーID」を取得する用途で使用する。
     * 各provider_idの最新versionが指定ステータスのものに絞り込む。</p>
     *
     * @param status 検索対象ステータス
     * @return 該当するprovider_idのリスト
     */
    List<String> findProviderIdsByLatestStatus(OidcProviderStatusValue status);

    /**
     * OIDCプロバイダーステータスを保存する。
     *
     * @param status 保存するOIDCプロバイダーステータス
     */
    void save(SystemOidcProviderStatus status);

}
