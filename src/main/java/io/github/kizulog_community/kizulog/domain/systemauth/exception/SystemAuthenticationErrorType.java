package io.github.kizulog_community.kizulog.domain.systemauth.exception;

/**
 * システム管理認証エラー種別
 *
 * <p>システム管理者の認証処理で発生するエラー種別を表現する。
 * Controller層でこのenumをMessageSourceと組み合わせて表示用メッセージに変換する。
 * Domain層では本enumのみを返却し、メッセージ文字列は保持しない。</p>
 *
 * @author Jun Kobayashi
 */
public enum SystemAuthenticationErrorType {

    /** アカウント未登録（iss/aud/subに一致するsystem_accountsレコードなし） */
    ACCOUNT_NOT_FOUND,

    /** アカウント無効（system_account_status.statusがACTIVE以外） */
    ACCOUNT_INACTIVE,

    /** SYSTEM_ADMINロール未付与 */
    ROLE_NOT_GRANTED;

}
