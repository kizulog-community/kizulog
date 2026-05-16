package io.github.kizulog_community.kizulog.domain.tenant.model;

/**
 * 業務テナントの有効性ステータス
 *
 * @author Jun Kobayashi
 */
public enum TenantStatusValue {

    /** 有効: テナント側のアクセス・認証が可能 */
    ACTIVE,

    /** 無効: 契約終了等で利用停止された状態 */
    INACTIVE,

    /** 一時停止中: 何らかの理由で一時的に停止された状態 */
    SUSPENDED;

    /**
     * このステータスのテナントが認証可能かを判定する。
     *
     * <p>現状はACTIVEのみ認証を許可する。INACTIVEやSUSPENDEDは認証不可。</p>
     *
     * @return 認証可能ならtrue
     */
    public boolean isAuthenticatable() {
        return this == ACTIVE;
    }

}
