package io.github.kizulog_community.kizulog.domain.tenantattendance.model;

import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * テナント利用者 打刻イベントドメインモデル
 *
 * <p>イベントログ型・追記専用の1打刻を表す不変モデル。
 * テーブル tenant_attendance_punches に対応する。</p>
 *
 * @author Jun Kobayashi
 */
@Getter
@AllArgsConstructor
public class AttendancePunch {

    /** 打刻ID（UUID） */
    private final String punchId;

    /** バージョン */
    private final OffsetDateTime version;

    /** アカウントID（tenant_accounts.account_id） */
    private final String accountId;

    /** 所属テナントID（tenants.tenant_id） */
    private final String tenantId;

    /** 打刻種別 */
    private final PunchType punchType;

    /** 打刻の業務時刻（UTC） */
    private final OffsetDateTime punchedAt;

    /** 作成日時＝記録時刻（UTC） */
    private final OffsetDateTime createdAt;

    /** 作成者 */
    private final String createdBy;

}
