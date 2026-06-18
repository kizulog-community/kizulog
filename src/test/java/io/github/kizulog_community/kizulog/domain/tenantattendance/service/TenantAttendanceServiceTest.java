package io.github.kizulog_community.kizulog.domain.tenantattendance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import io.github.kizulog_community.kizulog.domain.tenantattendance.exception.AttendanceError;
import io.github.kizulog_community.kizulog.domain.tenantattendance.exception.AttendanceException;
import io.github.kizulog_community.kizulog.domain.tenantattendance.model.AttendancePunch;
import io.github.kizulog_community.kizulog.domain.tenantattendance.model.AttendanceSession;
import io.github.kizulog_community.kizulog.domain.tenantattendance.model.PunchType;
import io.github.kizulog_community.kizulog.domain.tenantattendance.model.WorkState;
import io.github.kizulog_community.kizulog.domain.tenantattendance.port.AttendancePunchRepository;

/**
 * TenantAttendanceServiceの単体テスト
 *
 * @author Jun Kobayashi
 */
class TenantAttendanceServiceTest {

    private static final String ACCOUNT_ID = "acc-1";
    private static final String TENANT_ID = "tenant-1";
    private static final String CREATED_BY = "acc-1";
    private static final OffsetDateTime BASE_TIME =
            OffsetDateTime.of(2026, 12, 1, 9, 0, 0, 0, ZoneOffset.UTC);

    private AttendancePunchRepository repository;
    private TenantAttendanceService service;

    @BeforeEach
    void setUp() {
        repository = mock(AttendancePunchRepository.class);
        service = new TenantAttendanceService(repository);
    }

    private AttendancePunch punch(PunchType type, OffsetDateTime t) {
        return new AttendancePunch(
                "pid-" + type + "-" + t, t, ACCOUNT_ID, TENANT_ID, type, t, t, CREATED_BY);
    }

    /** 直近の出勤打刻が無い＝NOT_WORKING の状態をスタブする。 */
    private void givenNotWorking() {
        when(repository.findLatestPunchedAtByAccountIdAndType(ACCOUNT_ID, PunchType.CLOCK_IN))
                .thenReturn(Optional.empty());
    }

    /** 出勤のみ＝WORKING の状態をスタブする。 */
    private void givenWorking() {
        when(repository.findLatestPunchedAtByAccountIdAndType(ACCOUNT_ID, PunchType.CLOCK_IN))
                .thenReturn(Optional.of(BASE_TIME));
        when(repository.findByAccountIdSince(ACCOUNT_ID, BASE_TIME))
                .thenReturn(List.of(punch(PunchType.CLOCK_IN, BASE_TIME)));
    }

    /** 出勤→休憩開始＝ON_BREAK の状態をスタブする。 */
    private void givenOnBreak() {
        when(repository.findLatestPunchedAtByAccountIdAndType(ACCOUNT_ID, PunchType.CLOCK_IN))
                .thenReturn(Optional.of(BASE_TIME));
        when(repository.findByAccountIdSince(ACCOUNT_ID, BASE_TIME))
                .thenReturn(List.of(
                        punch(PunchType.CLOCK_IN, BASE_TIME),
                        punch(PunchType.BREAK_START, BASE_TIME.plusHours(1))));
    }

    @Test
    @DisplayName("punch: punchTypeがnullならPUNCH_TYPE_REQUIRED、保存しない")
    void punch_nullPunchType_throwsAndDoesNotSave() {
        assertThatThrownBy(() -> service.punch(ACCOUNT_ID, TENANT_ID, null, CREATED_BY))
                .isInstanceOf(AttendanceException.class)
                .extracting(e -> ((AttendanceException) e).getError())
                .isEqualTo(AttendanceError.PUNCH_TYPE_REQUIRED);

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("punch: NOT_WORKING + CLOCK_IN は許可され保存される")
    void punch_notWorking_clockIn_succeeds() {
        givenNotWorking();
        service.punch(ACCOUNT_ID, TENANT_ID, PunchType.CLOCK_IN, CREATED_BY);
        verify(repository).save(any(AttendancePunch.class));
    }

    @Test
    @DisplayName("punch: WORKING + CLOCK_OUT は許可され保存される")
    void punch_working_clockOut_succeeds() {
        givenWorking();
        service.punch(ACCOUNT_ID, TENANT_ID, PunchType.CLOCK_OUT, CREATED_BY);
        verify(repository).save(any(AttendancePunch.class));
    }

    @Test
    @DisplayName("punch: WORKING + BREAK_START は許可され保存される")
    void punch_working_breakStart_succeeds() {
        givenWorking();
        service.punch(ACCOUNT_ID, TENANT_ID, PunchType.BREAK_START, CREATED_BY);
        verify(repository).save(any(AttendancePunch.class));
    }

    @Test
    @DisplayName("punch: ON_BREAK + BREAK_END は許可され保存される")
    void punch_onBreak_breakEnd_succeeds() {
        givenOnBreak();
        service.punch(ACCOUNT_ID, TENANT_ID, PunchType.BREAK_END, CREATED_BY);
        verify(repository).save(any(AttendancePunch.class));
    }

    @Test
    @DisplayName("punch: ON_BREAK + CLOCK_OUT は許可され保存される（休憩中からの退勤可）")
    void punch_onBreak_clockOut_succeeds() {
        givenOnBreak();
        service.punch(ACCOUNT_ID, TENANT_ID, PunchType.CLOCK_OUT, CREATED_BY);
        verify(repository).save(any(AttendancePunch.class));
    }

    @Test
    @DisplayName("punch: NOT_WORKING + CLOCK_OUT は NOT_CLOCKED_IN で拒否")
    void punch_notWorking_clockOut_rejected() {
        givenNotWorking();
        assertReject(PunchType.CLOCK_OUT, AttendanceError.NOT_CLOCKED_IN);
    }

    @Test
    @DisplayName("punch: NOT_WORKING + BREAK_START は NOT_CLOCKED_IN で拒否")
    void punch_notWorking_breakStart_rejected() {
        givenNotWorking();
        assertReject(PunchType.BREAK_START, AttendanceError.NOT_CLOCKED_IN);
    }

    @Test
    @DisplayName("punch: NOT_WORKING + BREAK_END は NOT_CLOCKED_IN で拒否")
    void punch_notWorking_breakEnd_rejected() {
        givenNotWorking();
        assertReject(PunchType.BREAK_END, AttendanceError.NOT_CLOCKED_IN);
    }

    @Test
    @DisplayName("punch: WORKING + CLOCK_IN は ALREADY_CLOCKED_IN で拒否")
    void punch_working_clockIn_rejected() {
        givenWorking();
        assertReject(PunchType.CLOCK_IN, AttendanceError.ALREADY_CLOCKED_IN);
    }

    @Test
    @DisplayName("punch: WORKING + BREAK_END は NOT_ON_BREAK で拒否")
    void punch_working_breakEnd_rejected() {
        givenWorking();
        assertReject(PunchType.BREAK_END, AttendanceError.NOT_ON_BREAK);
    }

    @Test
    @DisplayName("punch: ON_BREAK + CLOCK_IN は ALREADY_CLOCKED_IN で拒否")
    void punch_onBreak_clockIn_rejected() {
        givenOnBreak();
        assertReject(PunchType.CLOCK_IN, AttendanceError.ALREADY_CLOCKED_IN);
    }

    @Test
    @DisplayName("punch: ON_BREAK + BREAK_START は ALREADY_ON_BREAK で拒否")
    void punch_onBreak_breakStart_rejected() {
        givenOnBreak();
        assertReject(PunchType.BREAK_START, AttendanceError.ALREADY_ON_BREAK);
    }

    /** 指定種別が指定エラーで拒否され、保存されないことを検証する。 */
    private void assertReject(PunchType type, AttendanceError expected) {
        assertThatThrownBy(() -> service.punch(ACCOUNT_ID, TENANT_ID, type, CREATED_BY))
                .isInstanceOf(AttendanceException.class)
                .extracting(e -> ((AttendanceException) e).getError())
                .isEqualTo(expected);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("punch: 記録はUUIDのpunchIdを持ち、version=punchedAt=createdAt（サーバー時刻）")
    void punch_records_serverTimeAndUuid() {
        givenNotWorking();

        OffsetDateTime before = OffsetDateTime.now(ZoneOffset.UTC);
        service.punch(ACCOUNT_ID, TENANT_ID, PunchType.CLOCK_IN, CREATED_BY);
        OffsetDateTime after = OffsetDateTime.now(ZoneOffset.UTC);

        ArgumentCaptor<AttendancePunch> captor = ArgumentCaptor.forClass(AttendancePunch.class);
        verify(repository).save(captor.capture());
        AttendancePunch saved = captor.getValue();

        assertThat(saved.getAccountId()).isEqualTo(ACCOUNT_ID);
        assertThat(saved.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(saved.getPunchType()).isEqualTo(PunchType.CLOCK_IN);
        assertThat(saved.getCreatedBy()).isEqualTo(CREATED_BY);
        // punchId は UUID 文字列
        assertThat(UUID.fromString(saved.getPunchId())).isNotNull();
        // version=punchedAt=createdAt が同一のサーバー受信時刻
        assertThat(saved.getPunchedAt()).isEqualTo(saved.getCreatedAt());
        assertThat(saved.getVersion()).isEqualTo(saved.getPunchedAt());
        // 受信時刻は呼び出し前後の範囲内（UTC）
        assertThat(saved.getPunchedAt()).isBetween(before, after);
        assertThat(saved.getPunchedAt().getOffset()).isEqualTo(ZoneOffset.UTC);
    }

    @Test
    @DisplayName("getCurrentSession: 出勤打刻が無ければNOT_WORKING・空、内訳取得は呼ばない")
    void getCurrentSession_noClockIn_returnsNotWorkingEmpty() {
        when(repository.findLatestPunchedAtByAccountIdAndType(ACCOUNT_ID, PunchType.CLOCK_IN))
                .thenReturn(Optional.empty());

        AttendanceSession session = service.getCurrentSession(ACCOUNT_ID);

        assertThat(session.getState()).isEqualTo(WorkState.NOT_WORKING);
        assertThat(session.getPunches()).isEmpty();
        verify(repository, never()).findByAccountIdSince(any(), any());
    }

    @Test
    @DisplayName("getCurrentSession: 直近が出勤ならWORKING、内訳を昇順保持")
    void getCurrentSession_lastClockIn_returnsWorking() {
        givenWorking();

        AttendanceSession session = service.getCurrentSession(ACCOUNT_ID);

        assertThat(session.getState()).isEqualTo(WorkState.WORKING);
        assertThat(session.getPunches()).hasSize(1);
        assertThat(session.getPunches().get(0).getPunchType()).isEqualTo(PunchType.CLOCK_IN);
    }

    @Test
    @DisplayName("getCurrentSession: 直近が休憩開始ならON_BREAK")
    void getCurrentSession_lastBreakStart_returnsOnBreak() {
        givenOnBreak();

        AttendanceSession session = service.getCurrentSession(ACCOUNT_ID);

        assertThat(session.getState()).isEqualTo(WorkState.ON_BREAK);
        assertThat(session.getPunches()).hasSize(2);
    }

    @Test
    @DisplayName("getCurrentSession: 直近が休憩終了ならWORKING")
    void getCurrentSession_lastBreakEnd_returnsWorking() {
        when(repository.findLatestPunchedAtByAccountIdAndType(ACCOUNT_ID, PunchType.CLOCK_IN))
                .thenReturn(Optional.of(BASE_TIME));
        when(repository.findByAccountIdSince(ACCOUNT_ID, BASE_TIME))
                .thenReturn(List.of(
                        punch(PunchType.CLOCK_IN, BASE_TIME),
                        punch(PunchType.BREAK_START, BASE_TIME.plusHours(1)),
                        punch(PunchType.BREAK_END, BASE_TIME.plusHours(2))));

        AttendanceSession session = service.getCurrentSession(ACCOUNT_ID);

        assertThat(session.getState()).isEqualTo(WorkState.WORKING);
        assertThat(session.getPunches()).hasSize(3);
    }

    @Test
    @DisplayName("getCurrentSession: 直近が退勤ならNOT_WORKING（直近完了セッションを保持）")
    void getCurrentSession_lastClockOut_returnsNotWorking() {
        when(repository.findLatestPunchedAtByAccountIdAndType(ACCOUNT_ID, PunchType.CLOCK_IN))
                .thenReturn(Optional.of(BASE_TIME));
        when(repository.findByAccountIdSince(ACCOUNT_ID, BASE_TIME))
                .thenReturn(List.of(
                        punch(PunchType.CLOCK_IN, BASE_TIME),
                        punch(PunchType.CLOCK_OUT, BASE_TIME.plusHours(8))));

        AttendanceSession session = service.getCurrentSession(ACCOUNT_ID);

        assertThat(session.getState()).isEqualTo(WorkState.NOT_WORKING);
        assertThat(session.getPunches()).hasSize(2);
    }

    @Test
    @DisplayName("getCurrentSession: 出勤時刻はあるが内訳が空なら防御的にNOT_WORKING・空")
    void getCurrentSession_sinceEmpty_defensiveNotWorking() {
        when(repository.findLatestPunchedAtByAccountIdAndType(ACCOUNT_ID, PunchType.CLOCK_IN))
                .thenReturn(Optional.of(BASE_TIME));
        when(repository.findByAccountIdSince(ACCOUNT_ID, BASE_TIME))
                .thenReturn(List.of());

        AttendanceSession session = service.getCurrentSession(ACCOUNT_ID);

        assertThat(session.getState()).isEqualTo(WorkState.NOT_WORKING);
        assertThat(session.getPunches()).isEmpty();
    }

    @Test
    @DisplayName("getCurrentSession: 日跨ぎセッション（深夜出勤→翌朝休憩）でもON_BREAKを導出")
    void getCurrentSession_dayCrossing_derivesFromLast() {
        OffsetDateTime clockIn = OffsetDateTime.of(2026, 12, 1, 23, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime breakStart = OffsetDateTime.of(2026, 12, 2, 1, 0, 0, 0, ZoneOffset.UTC);
        when(repository.findLatestPunchedAtByAccountIdAndType(ACCOUNT_ID, PunchType.CLOCK_IN))
                .thenReturn(Optional.of(clockIn));
        when(repository.findByAccountIdSince(ACCOUNT_ID, clockIn))
                .thenReturn(List.of(
                        punch(PunchType.CLOCK_IN, clockIn),
                        punch(PunchType.BREAK_START, breakStart)));

        AttendanceSession session = service.getCurrentSession(ACCOUNT_ID);

        assertThat(session.getState()).isEqualTo(WorkState.ON_BREAK);
        assertThat(session.getPunches()).hasSize(2);
        // 暦日を跨いでも1セッションとして扱われる
        assertThat(session.getPunches().get(0).getPunchedAt()).isEqualTo(clockIn);
        assertThat(session.getPunches().get(1).getPunchedAt()).isEqualTo(breakStart);
    }

}
