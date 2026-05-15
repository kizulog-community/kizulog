package io.github.kizulog_community.kizulog.domain.systemaccountlocalization.exception;

/**
 * アカウント単位の言語・タイムゾーン設定のエラー種別
 *
 * @author Jun Kobayashi
 */
public enum AccountLocalizationError {

    /** 言語が未指定 */
    LANGUAGE_REQUIRED,

    /** タイムゾーンが未指定 */
    TIMEZONE_REQUIRED,

    /** 指定された言語がシステムの利用可能言語リストに含まれていない */
    LANGUAGE_NOT_AVAILABLE,

    /** 指定されたタイムゾーンがシステムの利用可能タイムゾーンリストに含まれていない */
    TIMEZONE_NOT_AVAILABLE,

    /** システム側の言語・タイムゾーン設定が未構成（J.0未完了状態） */
    SYSTEM_LOCALIZATION_NOT_CONFIGURED;

}
