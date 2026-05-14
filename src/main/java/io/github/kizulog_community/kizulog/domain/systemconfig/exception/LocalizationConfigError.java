package io.github.kizulog_community.kizulog.domain.systemconfig.exception;

/**
 * 言語・タイムゾーン設定のエラー種別
 *
 * @author Jun Kobayashi
 */
public enum LocalizationConfigError {

    /** デフォルト言語が未指定 */
    DEFAULT_LANGUAGE_REQUIRED,

    /** 利用可能言語リストが空 */
    AVAILABLE_LANGUAGES_EMPTY,

    /** デフォルト言語が利用可能言語リストに含まれていない */
    DEFAULT_LANGUAGE_NOT_IN_AVAILABLE,

    /** デフォルトタイムゾーンが未指定 */
    DEFAULT_TIMEZONE_REQUIRED,

    /** 利用可能タイムゾーンリストが空 */
    AVAILABLE_TIMEZONES_EMPTY,

    /** デフォルトタイムゾーンが利用可能タイムゾーンリストに含まれていない */
    DEFAULT_TIMEZONE_NOT_IN_AVAILABLE,

    /** JSONシリアライズ失敗（バグ起因の異常系） */
    SERIALIZATION_FAILED,

    /** JSONデシリアライズ失敗（保存値破損等） */
    DESERIALIZATION_FAILED;

}
