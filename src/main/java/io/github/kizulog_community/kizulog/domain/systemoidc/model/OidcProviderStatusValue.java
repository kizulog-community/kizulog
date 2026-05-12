package io.github.kizulog_community.kizulog.domain.systemoidc.model;

/**
 * OIDCプロバイダーの有効性ステータス
 *
 * <p>system_oidc_provider_status.status カラムに永続化される値を表す。</p>
 *
 * @author Jun Kobayashi
 */
public enum OidcProviderStatusValue {

    /** 有効: 認証フローで使用可能 */
    ENABLED,

    /** 無効: 認証フローで使用不可（履歴保持のため削除はしない） */
    DISABLED;

    /**
     * このステータスのプロバイダーが認証フローで使用可能かを判定する。
     *
     * @return 使用可能ならtrue
     */
    public boolean isUsable() {
        return this == ENABLED;
    }

}
