package io.github.kizulog_community.kizulog.domain.tenantattendance.port;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import io.github.kizulog_community.kizulog.domain.tenantattendance.model.AttendancePunch;
import io.github.kizulog_community.kizulog.domain.tenantattendance.model.PunchType;

/**
 * 打刻イベントリポジトリインターフェース（Output Port）
 *
 * @author Jun Kobayashi
 */
public interface AttendancePunchRepository {

    /**
     * 打刻イベントを保存する。
     *
     * @param punch 保存する打刻イベント
     */
    void save(AttendancePunch punch);

    /**
     * 指定アカウントの、指定打刻種別における最新（最大）の業務時刻を取得する。
     *
     * <p>セッション起点（直近の出勤打刻時刻）の解決に用いる。
     * 各打刻は punch_id 単位で最新バージョンを対象とする。</p>
     *
     * @param accountId アカウントID
     * @param punchType 打刻種別
     * @return 該当する最新の業務時刻。1件も無い場合は空のOptional
     */
    Optional<OffsetDateTime> findLatestPunchedAtByAccountIdAndType(
            String accountId, PunchType punchType);

    /**
     * 指定アカウントの、指定業務時刻以降（境界含む）の打刻イベントを業務時刻昇順で取得する。
     *
     * <p>セッション内訳の取得に用いる。各打刻は punch_id 単位で最新バージョンを対象とする。</p>
     *
     * @param accountId アカウントID
     * @param since 業務時刻の下限（この時刻を含む）
     * @return 業務時刻昇順の打刻イベント一覧（該当なしの場合は空リスト）
     */
    List<AttendancePunch> findByAccountIdSince(String accountId, OffsetDateTime since);

}