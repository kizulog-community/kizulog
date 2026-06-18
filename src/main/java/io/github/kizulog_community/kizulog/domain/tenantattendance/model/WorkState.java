package io.github.kizulog_community.kizulog.domain.tenantattendance.model;

/**
 * 現在の勤務状態
 *
 * <p>暦日には依存せず、利用者の直近の打刻イベントから導出される。</p>
 *
 * @author Jun Kobayashi
 */
public enum WorkState {

    /** 未出勤（打刻なし、または直近の打刻が退勤） */
    NOT_WORKING,

    /** 出勤中（直近の打刻が出勤、または休憩終了） */
    WORKING,

    /** 休憩中（直近の打刻が休憩開始） */
    ON_BREAK;

}
