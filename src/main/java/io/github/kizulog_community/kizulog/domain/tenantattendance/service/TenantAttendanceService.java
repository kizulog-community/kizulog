package io.github.kizulog_community.kizulog.domain.tenantattendance.service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.kizulog_community.kizulog.domain.tenantattendance.exception.AttendanceError;
import io.github.kizulog_community.kizulog.domain.tenantattendance.exception.AttendanceException;
import io.github.kizulog_community.kizulog.domain.tenantattendance.model.AttendancePunch;
import io.github.kizulog_community.kizulog.domain.tenantattendance.model.AttendanceSession;
import io.github.kizulog_community.kizulog.domain.tenantattendance.model.PunchType;
import io.github.kizulog_community.kizulog.domain.tenantattendance.model.WorkState;
import io.github.kizulog_community.kizulog.domain.tenantattendance.port.AttendancePunchRepository;
import lombok.RequiredArgsConstructor;

/**
 * テナント利用者 打刻サービス
 *
 * <p>本人の打刻記録、現在の勤務状態の導出、進行中（または直近完了）セッションの構成、
 * および状態遷移チェックを担う。
 * すべて業務時刻はUTCで扱い、タイムゾーンには関与しない。
 * 日跨ぎ夜勤を許容するため暦日での制約は設けない。</p>
 *
 * <p>状態遷移ルール:
 * <ul>
 * <li>NOT_WORKING: 出勤のみ可</li>
 * <li>WORKING: 退勤・休憩開始 可（二重出勤・休憩終了 不可）</li>
 * <li>ON_BREAK: 休憩終了・退勤 可（出勤・二重休憩開始 不可）</li>
 * </ul>
 * </p>
 *
 * @author Jun Kobayashi
 */
@Service
@RequiredArgsConstructor
public class TenantAttendanceService {

    /** ロガー */
    private static final Logger log = LoggerFactory.getLogger(TenantAttendanceService.class);

    /** 打刻イベントリポジトリ */
    private final AttendancePunchRepository repository;

    /**
     * 本人の打刻を記録する。
     *
     * <p>現在の勤務状態に対する遷移チェックを行い、許可される打刻のみ記録する。
     * 打刻の業務時刻・バージョン・作成日時はサーバー受信時刻（UTC）とする。</p>
     *
     * @param accountId アカウントID
     * @param tenantId テナントID
     * @param punchType 打刻種別
     * @param createdBy 作成者
     * @return 記録した打刻イベント
     * @throws AttendanceException 打刻種別が未指定、または不正な状態遷移の場合
     */
    @Transactional
    public AttendancePunch punch(
            String accountId, String tenantId, PunchType punchType, String createdBy) {
        if (punchType == null) {
            throw new AttendanceException(AttendanceError.PUNCH_TYPE_REQUIRED);
        }

        WorkState currentState = getCurrentSession(accountId).getState();
        validateTransition(currentState, punchType);

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        AttendancePunch punch = new AttendancePunch(
                UUID.randomUUID().toString(),
                now,
                accountId,
                tenantId,
                punchType,
                now,
                now,
                createdBy);
        repository.save(punch);

        log.info("打刻を記録しました: accountId={}, tenantId={}, punchType={}, punchedAt={}",
                accountId, tenantId, punchType, now);
        return punch;
    }

    /**
     * 指定アカウントの現在の勤務状態と、進行中（または直近完了）セッションを取得する。
     *
     * <p>セッションは「直近の出勤打刻以降」と定義する。
     * 出勤打刻が無い場合は NOT_WORKING・空セッションを返す。</p>
     *
     * @param accountId アカウントID
     * @return 勤務状態とセッション打刻群
     */
    @Transactional(readOnly = true)
    public AttendanceSession getCurrentSession(String accountId) {
        Optional<OffsetDateTime> sessionStart =
                repository.findLatestPunchedAtByAccountIdAndType(accountId, PunchType.CLOCK_IN);
        if (sessionStart.isEmpty()) {
            return new AttendanceSession(WorkState.NOT_WORKING, List.of());
        }

        List<AttendancePunch> punches =
                repository.findByAccountIdSince(accountId, sessionStart.get());
        if (punches.isEmpty()) {
            // 通常は到達しない（出勤打刻が存在するため）。
            return new AttendanceSession(WorkState.NOT_WORKING, List.of());
        }

        AttendancePunch latest = punches.get(punches.size() - 1);
        WorkState state = deriveState(latest.getPunchType());
        return new AttendanceSession(state, punches);
    }

    /**
     * 直近の打刻種別から現在の勤務状態を導出する。
     *
     * @param latestType 直近の打刻種別
     * @return 勤務状態
     */
    private WorkState deriveState(PunchType latestType) {
        return switch (latestType) {
            case CLOCK_IN, BREAK_END -> WorkState.WORKING;
            case BREAK_START -> WorkState.ON_BREAK;
            case CLOCK_OUT -> WorkState.NOT_WORKING;
        };
    }

    /**
     * 現在の勤務状態に対して打刻種別の遷移が妥当か検証する。
     *
     * @param currentState 現在の勤務状態
     * @param punchType 打刻種別
     * @throws AttendanceException 不正な遷移の場合
     */
    private void validateTransition(WorkState currentState, PunchType punchType) {
        switch (punchType) {
            case CLOCK_IN -> {
                if (currentState != WorkState.NOT_WORKING) {
                    throw new AttendanceException(AttendanceError.ALREADY_CLOCKED_IN);
                }
            }
            case CLOCK_OUT -> {
                if (currentState == WorkState.NOT_WORKING) {
                    throw new AttendanceException(AttendanceError.NOT_CLOCKED_IN);
                }
            }
            case BREAK_START -> {
                if (currentState == WorkState.NOT_WORKING) {
                    throw new AttendanceException(AttendanceError.NOT_CLOCKED_IN);
                }
                if (currentState == WorkState.ON_BREAK) {
                    throw new AttendanceException(AttendanceError.ALREADY_ON_BREAK);
                }
            }
            case BREAK_END -> {
                if (currentState == WorkState.NOT_WORKING) {
                    throw new AttendanceException(AttendanceError.NOT_CLOCKED_IN);
                }
                if (currentState == WorkState.WORKING) {
                    throw new AttendanceException(AttendanceError.NOT_ON_BREAK);
                }
            }
        }
    }

}
