package io.github.kizulog_community.kizulog.domain.systemadmininvitation.exception;

/**
 * 管理者招待機能のエラー種別
 *
 * @author Jun Kobayashi
 */
public enum InvitationError {

    /** トークンが存在しない（誤URL、改ざん） */
    INVALID_TOKEN,

    /** トークンが有効期限切れ */
    EXPIRED,

    /** トークンが既に使用済み（status = USED） */
    ALREADY_USED,

    /** 招待が取消済み（status = CANCELLED） */
    CANCELLED,

    /** OIDC ログイン失敗（プロバイダー側のエラー） */
    OIDC_FAILED,

    /** 受諾者の OIDC identity が既に登録されている */
    IDENTITY_EXISTS,

    /** 招待が見つからない */
    INVITATION_NOT_FOUND,

    /** 既に USED 状態（取消操作時の制約違反） */
    ALREADY_USED_FOR_CANCEL,

    /** 既に CANCELLED 状態（取消操作時の制約違反） */
    ALREADY_CANCELLED

}
