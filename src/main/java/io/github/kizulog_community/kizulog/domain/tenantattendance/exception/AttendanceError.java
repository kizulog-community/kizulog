package io.github.kizulog_community.kizulog.domain.tenantattendance.exception;

/**
 * 打刻操作のエラー種別
 *
 * <p>状態遷移チェック（緩め）に基づく不正遷移と、入力不備を表す。
 * メッセージ化は Controller 層で MessageSource により行い、ドメイン層は本enumのみを返す。</p>
 *
 * @author Jun Kobayashi
 */
public enum AttendanceError {

    /** 打刻種別が未指定 */
    PUNCH_TYPE_REQUIRED,

    /** 未出勤の状態で退勤・休憩開始・休憩終了を打刻しようとした */
    NOT_CLOCKED_IN,

    /** 既に出勤中（または休憩中）の状態で出勤を打刻しようとした */
    ALREADY_CLOCKED_IN,

    /** 休憩中でない状態で休憩終了を打刻しようとした */
    NOT_ON_BREAK,

    /** 既に休憩中の状態で休憩開始を打刻しようとした */
    ALREADY_ON_BREAK;

}
