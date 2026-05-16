package io.github.kizulog_community.kizulog.infrastructure.web.tenant;

import io.github.kizulog_community.kizulog.domain.tenant.model.Tenant;

/**
 * 現リクエストのテナント情報を保持する ThreadLocal コンテキスト
 *
 * @author Jun Kobayashi
 */
public final class TenantContext {

    /** 現リクエストのテナント情報 */
    private static final ThreadLocal<Tenant> CURRENT_TENANT = new ThreadLocal<>();

    /**
     * インスタンス化禁止
     */
    private TenantContext() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * 現リクエストのテナントを設定する。
     *
     * @param tenant テナント情報
     */
    public static void set(Tenant tenant) {
        CURRENT_TENANT.set(tenant);
    }

    /**
     * 現リクエストのテナントを取得する。
     *
     * @return テナント情報。未設定の場合は null
     */
    public static Tenant current() {
        return CURRENT_TENANT.get();
    }

    /**
     * 現リクエストのテナント情報をクリアする。
     */
    public static void clear() {
        CURRENT_TENANT.remove();
    }

}
