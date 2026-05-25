package io.github.kizulog_community.kizulog.domain.tenantaccount.port;

import java.util.List;
import java.util.Optional;

import io.github.kizulog_community.kizulog.domain.tenantaccount.model.TenantAccountIdentity;

/**
 * 業務テナントアカウント認証方法リポジトリインターフェース（Output Port）
 *
 * @author Jun Kobayashi
 */
public interface TenantAccountIdentityRepository {

    /**
     * テナント境界つきで iss/aud/sub から最新バージョンの identity を取得する。
     *
     * @param tenantId テナントID
     * @param iss OIDC Issuer
     * @param aud OIDC Audience
     * @param sub OIDC Subject
     * @return 最新バージョンの業務テナントアカウント認証方法
     */
    Optional<TenantAccountIdentity> findLatestByTenantIdAndIssAndAudAndSub(
            String tenantId, String iss, String aud, String sub);

    /**
     * identity_id で最新バージョンの業務テナントアカウント認証方法を取得する。
     *
     * @param identityId アイデンティティID
     * @return 最新バージョンの業務テナントアカウント認証方法
     */
    Optional<TenantAccountIdentity> findLatestByIdentityId(String identityId);

    /**
     * account_id に紐付く全認証方法の最新バージョンを取得する。
     *
     * @param accountId アカウントID
     * @return 業務テナントアカウント認証方法のリスト（空の場合あり）
     */
    List<TenantAccountIdentity> findLatestByAccountId(String accountId);

    /**
     * 指定テナント・指定 Issuer URI に紐付く ACTIVE な identity の件数を取得する。
     *
     * @param tenantId テナントID
     * @param iss OIDC Issuer URI
     * @return ACTIVE な identity の件数
     */
    int countActiveByTenantIdAndIss(String tenantId, String iss);

    /**
     * 業務テナントアカウント認証方法を保存する。
     *
     * @param identity 保存する業務テナントアカウント認証方法
     */
    void save(TenantAccountIdentity identity);

}
