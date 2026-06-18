package io.github.kizulog_community.kizulog.infrastructure.web.tenant.attendance;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import io.github.kizulog_community.kizulog.domain.tenantattendance.exception.AttendanceException;
import io.github.kizulog_community.kizulog.domain.tenantattendance.model.PunchType;
import io.github.kizulog_community.kizulog.domain.tenantattendance.service.TenantAttendanceService;
import io.github.kizulog_community.kizulog.infrastructure.security.principal.TenantUserPrincipal;
import lombok.RequiredArgsConstructor;

/**
 * テナント利用者 打刻コントローラー
 *
 * <p>URL: POST /attendance/punch</p>
 *
 * <p>打刻パネル（ダッシュボードに統合）からの打刻要求を受け付ける。
 * 記録の成否いずれも PRG パターンでダッシュボードへリダイレクトし、
 * 結果は flash メッセージキーで通知する。
 * 表示は呼び出し先テンプレートが行う。</p>
 *
 * @author Jun Kobayashi
 */
@Controller
@RequiredArgsConstructor
public class TenantAttendanceController {

    /** flash: 成功メッセージキーの接頭辞 */
    private static final String SUCCESS_KEY_PREFIX = "tenant.attendance.flash.success.";

    /** flash: エラーメッセージキーの接頭辞 */
    private static final String ERROR_KEY_PREFIX = "tenant.attendance.error.";

    /** 打刻サービス */
    private final TenantAttendanceService attendanceService;

    /**
     * 本人の打刻を記録する。
     *
     * @param principal 認証済みプリンシパル
     * @param punchType 打刻種別
     * @param redirectAttrs リダイレクト属性（flash通知用）
     * @return ダッシュボードへのリダイレクト
     */
    @PostMapping("/attendance/punch")
    public String punch(
            @AuthenticationPrincipal TenantUserPrincipal principal,
            @RequestParam PunchType punchType,
            RedirectAttributes redirectAttrs) {

        String accountId = principal.getAccountId();
        String tenantId = principal.getTenantId();
        try {
            attendanceService.punch(accountId, tenantId, punchType, accountId);
            redirectAttrs.addFlashAttribute(
                    "flashSuccessKey", SUCCESS_KEY_PREFIX + punchType.name());
        } catch (AttendanceException e) {
            redirectAttrs.addFlashAttribute(
                    "flashErrorKey", ERROR_KEY_PREFIX + e.getError().name());
        }
        return "redirect:/dashboard";
    }

}
