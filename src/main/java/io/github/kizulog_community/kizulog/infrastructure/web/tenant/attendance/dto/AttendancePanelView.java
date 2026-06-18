package io.github.kizulog_community.kizulog.infrastructure.web.tenant.attendance.dto;

import java.util.List;

import io.github.kizulog_community.kizulog.domain.tenantattendance.model.WorkState;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * ダッシュボードの打刻パネル表示用ビュー
 *
 * @author Jun Kobayashi
 */
@Getter
@AllArgsConstructor
public class AttendancePanelView {

    /** 現在の勤務状態（状態ラベル解決に使用） */
    private final WorkState state;

    /** 出勤打刻が可能か */
    private final boolean canClockIn;

    /** 退勤打刻が可能か */
    private final boolean canClockOut;

    /** 休憩開始打刻が可能か */
    private final boolean canBreakStart;

    /** 休憩終了打刻が可能か */
    private final boolean canBreakEnd;

    /** セッションの打刻一覧（業務時刻昇順・空可） */
    private final List<AttendancePunchRowView> punches;

    /** 表示タイムゾーンID（例：Asia/Tokyo） */
    private final String timezoneId;

}
