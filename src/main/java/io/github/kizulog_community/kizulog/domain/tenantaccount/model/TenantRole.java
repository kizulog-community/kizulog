package io.github.kizulog_community.kizulog.domain.tenantaccount.model;

/**
 * 業務テナントアカウントのロール
 *
 * @author Jun Kobayashi
 */
public enum TenantRole {

    /** テナント管理者ロール: テナント管理画面（/admin/**）の全機能にアクセス可能 */
    TENANT_ADMIN,

    /** 一般従業員ロール: テナント業務機能（打刻、勤怠申請等）にアクセス可能 */
    EMPLOYEE;

}
