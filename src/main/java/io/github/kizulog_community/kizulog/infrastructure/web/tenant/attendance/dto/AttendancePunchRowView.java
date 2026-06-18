package io.github.kizulog_community.kizulog.infrastructure.web.tenant.attendance.dto;

import io.github.kizulog_community.kizulog.domain.tenantattendance.model.PunchType;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 打刻パネルのセッション一覧における1行の表示用ビュー
 *
 * @author Jun Kobayashi
 */
@Getter
@AllArgsConstructor
public class AttendancePunchRowView {

    /** 打刻種別 */
    private final PunchType punchType;

    /** 表示用の打刻時刻 */
    private final String timeText;

}
