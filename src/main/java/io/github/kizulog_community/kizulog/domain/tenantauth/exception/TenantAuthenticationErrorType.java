package io.github.kizulog_community.kizulog.domain.tenantauth.exception;

/**
 * テナント利用者認証のエラー種別
 *
 * @author Jun Kobayashi
 */
public enum TenantAuthenticationErrorType {

    /** 該当するアカウント（identity）が見つからない */
    ACCOUNT_NOT_FOUND,

    /** identity が無効状態（INACTIVE/SUSPENDED） */
    IDENTITY_INACTIVE,

    /** アカウントが無効状態（INACTIVE/SUSPENDED） */
    ACCOUNT_INACTIVE,

    /** 有効なロールが付与されていない（TENANT_ADMIN / EMPLOYEE いずれも無効） */
    ROLE_NOT_GRANTED

}
 