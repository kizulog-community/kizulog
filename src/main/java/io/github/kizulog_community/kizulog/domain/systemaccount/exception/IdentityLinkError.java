package io.github.kizulog_community.kizulog.domain.systemaccount.exception;

/**
 * Identityリンク（追加・解除）エラー種別
 *
 * @author Jun Kobayashi
 */
public enum IdentityLinkError {

    /** 指定provider_idが存在しない・無効化済み */
    PROVIDER_NOT_FOUND,

    /** 現在のaccountに、追加しようとしたproviderのACTIVE identityが既に存在 */
    PROVIDER_ALREADY_LINKED,

    /** iss/aud/subが既に他accountまたは自accountで使用中 */
    IDENTITY_ALREADY_LINKED,

    /** 解除対象のidentityが存在しない */
    IDENTITY_NOT_FOUND,

    /** 解除対象のidentityが現在のaccountのものではない（権限エラー） */
    IDENTITY_NOT_OWNED,

    /** 解除対象のidentityが現在ログイン中のセッションで使用中 */
    CANNOT_UNLINK_CURRENT_SESSION,

    /** 解除すると当該accountのACTIVE identityが0になる */
    CANNOT_UNLINK_LAST_ACTIVE,

    /** 解除対象のidentityが既にINACTIVE */
    ALREADY_INACTIVE,

}
