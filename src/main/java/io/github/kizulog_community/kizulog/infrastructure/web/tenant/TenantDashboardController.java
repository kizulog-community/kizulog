package io.github.kizulog_community.kizulog.infrastructure.web.tenant;

import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import io.github.kizulog_community.kizulog.domain.shared.SupportedTimezone;
import io.github.kizulog_community.kizulog.domain.shared.TenantTimezoneResolver;
import io.github.kizulog_community.kizulog.domain.tenant.model.Tenant;
import io.github.kizulog_community.kizulog.domain.tenantattendance.model.AttendanceSession;
import io.github.kizulog_community.kizulog.domain.tenantattendance.model.WorkState;
import io.github.kizulog_community.kizulog.domain.tenantattendance.service.TenantAttendanceService;
import io.github.kizulog_community.kizulog.infrastructure.security.principal.TenantUserPrincipal;
import io.github.kizulog_community.kizulog.infrastructure.web.tenant.attendance.dto.AttendancePanelView;
import io.github.kizulog_community.kizulog.infrastructure.web.tenant.attendance.dto.AttendancePunchRowView;
import lombok.RequiredArgsConstructor;

/**
 * テナント利用者ダッシュボード画面のコントローラー
 *
 * <p>/dashboardでログイン後のダッシュボードを表示する。
 * テナントホスト配下で認証済みの利用者のみアクセス可能。
 * 打刻パネル（現在状態・各打刻ボタンの活性・進行中セッション）を統合表示する。</p>
 *
 * @author Jun Kobayashi
 */
@Controller
@RequiredArgsConstructor
public class TenantDashboardController {

    /** セッション打刻時刻の表示フォーマット（表示TZ変換後に適用） */
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("MM/dd HH:mm");

    /** 打刻サービス */
    private final TenantAttendanceService attendanceService;

    /** 実効タイムゾーン解決 */
    private final TenantTimezoneResolver timezoneResolver;

    /**
     * ダッシュボード画面を表示する。
     *
     * @param principal ログイン中のテナント利用者
     * @param model モデル
     * @return ビュー名
     */
    @GetMapping("/dashboard")
    public String dashboard(
            @AuthenticationPrincipal TenantUserPrincipal principal,
            Model model) {
        // 受諾アクセス元ホストから解決したテナント（TenantResolverFilter が設定済み）
        Tenant tenant = TenantContext.current();
        model.addAttribute("principal", principal);
        model.addAttribute("tenant", tenant);

        // /dashboard は認証必須のため通常 principal は非null。
        // 防御的に null の場合は打刻パネルを積まない（テンプレートは attendancePanel!=null で制御）。
        if (principal != null) {
            String accountId = principal.getAccountId();
            String tenantId = principal.getTenantId();
            AttendanceSession session = attendanceService.getCurrentSession(accountId);
            SupportedTimezone timezone = timezoneResolver.resolve(tenantId, accountId);
            model.addAttribute("attendancePanel", buildPanel(session, timezone));
        }
        return "tenant/dashboard";
    }

    /**
     * セッションと表示タイムゾーンから打刻パネルのビューを構築する。
     *
     * @param session 勤務状態とセッション打刻群
     * @param timezone 表示タイムゾーン
     * @return 打刻パネルビュー
     */
    private AttendancePanelView buildPanel(
            AttendanceSession session, SupportedTimezone timezone) {
        WorkState state = session.getState();

        List<AttendancePunchRowView> rows = session.getPunches().stream()
                .map(punch -> new AttendancePunchRowView(
                        punch.getPunchType(),
                        punch.getPunchedAt()
                                .atZoneSameInstant(timezone.getZoneId())
                                .format(TIME_FORMATTER)))
                .toList();

        boolean canClockIn = state == WorkState.NOT_WORKING;
        boolean canClockOut = state == WorkState.WORKING || state == WorkState.ON_BREAK;
        boolean canBreakStart = state == WorkState.WORKING;
        boolean canBreakEnd = state == WorkState.ON_BREAK;

        return new AttendancePanelView(
                state,
                canClockIn,
                canClockOut,
                canBreakStart,
                canBreakEnd,
                rows,
                timezone.getId());
    }

}
