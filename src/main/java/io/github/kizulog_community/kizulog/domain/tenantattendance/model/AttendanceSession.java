package io.github.kizulog_community.kizulog.domain.tenantattendance.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 現在の勤務状態と、進行中（または直近完了）セッションの打刻群を表すドメイン結果
 *
 * <p>セッションは「直近の出勤打刻以降」と定義され、暦日には依存しない（夜勤・日跨ぎ対応）。
 * 出勤打刻がまだ無い場合は NOT_WORKING・punches は空。
 * 直近が退勤の場合は直近完了セッション（出勤〜退勤）を保持する。</p>
 *
 * <p>業務時刻昇順。
 * 時刻はすべてUTCで、表示時にタイムゾーン変換する。</p>
 *
 * @author Jun Kobayashi
 */
@Getter
@AllArgsConstructor
public class AttendanceSession {

    /** 現在の勤務状態 */
    private final WorkState state;

    /** セッションの打刻群（業務時刻昇順・空可・UTC） */
    private final List<AttendancePunch> punches;

}
