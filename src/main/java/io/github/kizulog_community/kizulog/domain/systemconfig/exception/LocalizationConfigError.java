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

    /** 削除しようとしている言語がACTIVEなアカウントで使用中 */
    LANGUAGE_IN_USE_BY_ACCOUNT,

    /** 削除しようとしているタイムゾーンがACTIVEなアカウントで使用中 */
    TIMEZONE_IN_USE_BY_ACCOUNT,

    /** JSONシリアライズ失敗（バグ起因の異常系） */
    SERIALIZATION_FAILED,

    /** JSONデシリアライズ失敗（保存値破損等） */
    DESERIALIZATION_FAILED;

}
