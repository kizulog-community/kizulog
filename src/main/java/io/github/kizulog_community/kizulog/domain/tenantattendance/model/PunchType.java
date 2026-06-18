package io.github.kizulog_community.kizulog.domain.tenantattendance.model;

/**
 * 打刻種別
 *
 * <p>出勤・退勤・休憩開始・休憩終了の4種をサポートする。
 * 規則による固定休憩（控除）は打刻として記録せず、集計側の概念として扱う。
 * 外出・戻り等の業務多様化には将来の定数追加で対応する。</p>
 *
 * @author Jun Kobayashi
 */
public enum PunchType {

    /** 出勤打刻 */
    CLOCK_IN,

    /** 退勤打刻 */
    CLOCK_OUT,

    /** 休憩開始打刻 */
    BREAK_START,

    /** 休憩終了打刻 */
    BREAK_END;

}