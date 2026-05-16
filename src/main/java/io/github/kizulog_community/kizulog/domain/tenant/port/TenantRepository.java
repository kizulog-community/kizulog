package io.github.kizulog_community.kizulog.domain.tenant.port;

import java.util.List;
import java.util.Optional;

import io.github.kizulog_community.kizulog.domain.tenant.model.Tenant;

/**
 * 業務テナントリポジトリインターフェース（Output Port）
 *
 * @author Jun Kobayashi
 */
public interface TenantRepository {

    /**
     * tenantIdで最新バージョンのテナントを取得する。
     *
     * @param tenantId テナントID
     * @return 最新バージョンのテナント。存在しない場合は空のOptional
     */
    Optional<Tenant> findLatestByTenantId(String tenantId);

    /**
     * slugで最新バージョンのテナントを取得する。
     *
     * @param slug URL用slug
     * @return 最新バージョンのテナント。存在しない場合は空のOptional
     */
    Optional<Tenant> findLatestBySlug(String slug);

    /**
     * 全テナントの最新バージョンを取得する。
     *
     * @return 各tenantIdの最新バージョンのテナント（空リスト返却あり）
     */
    List<Tenant> findAllLatest();

    /**
     * 指定したslugが過去含めて存在するかを判定する。
     *
     * @param slug URL用slug
     * @return 存在すればtrue
     */
    boolean existsBySlug(String slug);

    /**
     * テナントを保存する。
     *
     * @param tenant 保存するテナント
     */
    void save(Tenant tenant);

}