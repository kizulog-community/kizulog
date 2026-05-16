package io.github.kizulog_community.kizulog.domain.tenant.model;

/**
 * 業務テナント識別ホストの有効性ステータス
 *
 * @author Jun Kobayashi
 */
public enum TenantHostStatusValue {

    /** 有効: このhostでテナントにアクセス可能 */
    ACTIVE,

    /** 無効: このhostでは（過去に有効だったが）現在アクセス不可 */
    INACTIVE;

    /**
     * このステータスのhostがテナント識別に利用可能かを判定する。
     *
     * @return 利用可能ならtrue
     */
    public boolean isUsable() {
        return this == ACTIVE;
    }

}
