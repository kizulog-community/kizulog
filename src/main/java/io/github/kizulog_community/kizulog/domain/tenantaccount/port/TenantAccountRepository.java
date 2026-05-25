package io.github.kizulog_community.kizulog.domain.tenantaccount.port;

import java.util.List;
import java.util.Optional;

import io.github.kizulog_community.kizulog.domain.tenantaccount.model.TenantAccount;

/**
 * 業務テナントアカウントリポジトリインターフェース（Output Port）
 *
 * @author Jun Kobayashi
 */
public interface TenantAccountRepository {

    /**
     * account_id で最新バージョンの業務テナントアカウントを取得する。
     *
     * @param accountId アカウントID
     * @return 最新バージョンの業務テナントアカウント。存在しない場合は空の Optional
     */
    Optional<TenantAccount> findLatestByAccountId(String accountId);

    /**
     * 指定テナントに所属する全アカウントの最新バージョンを取得する。
     *
     * @param tenantId テナントID
     * @return 各 account_id の最新バージョンのアカウント（空リスト返却あり）
     */
    List<TenantAccount> findAllLatestByTenantId(String tenantId);

    /**
     * 業務テナントアカウントを保存する。
     *
     * @param tenantAccount 保存するアカウント
     */
    void save(TenantAccount tenantAccount);

}
