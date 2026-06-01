package io.github.kizulog_community.kizulog.domain.systemaccountprofile.exception;

/**
 * アカウントプロファイル操作のエラー種別
 *
 * @author Jun Kobayashi
 */
public enum AccountProfileError {

    /** Identity IDが未指定 */
    IDENTITY_ID_REQUIRED,

    /** クレームが未指定 */
    CLAIMS_REQUIRED;

}
