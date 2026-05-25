package io.github.kizulog_community.kizulog.infrastructure.web.tenant;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import io.github.kizulog_community.kizulog.domain.tenantauth.exception.TenantAuthenticationErrorType;

/**
 * テナント利用者ログインコントローラー
 *
 * <p>テナントログインページを表示する。
 * 実際の認証はSpring SecurityのOAuth2 Loginフロー（/oauth2/authorization/tenant-{tenantId}-{providerId}）
 * に委譲する。</p>
 *
 * <p>URL設計:
 * <ul>
 * <li>/login: ログインページ表示</li>
 * <li>/login?error: 認証失敗（エラーコード不明）</li>
 * <li>/login?error=ACCOUNT_NOT_FOUND: アカウント未登録</li>
 * <li>/login?error=IDENTITY_INACTIVE: identity無効</li>
 * <li>/login?error=ACCOUNT_INACTIVE: アカウント無効</li>
 * <li>/login?error=ROLE_NOT_GRANTED: テナントロール無し</li>
 * <li>/login?logout: ログアウト完了</li>
 * </ul>
 * </p>
 *
 * @author Jun Kobayashi
 */
@Controller
public class TenantLoginController {

    /** 不明なエラーコードを示すマーカー */
    static final String ERROR_CODE_UNKNOWN = "unknown";

    /** 許容エラーコードのホワイトリスト */
    private static final Set<String> ALLOWED_ERROR_CODES = buildAllowedErrorCodes();

    private static Set<String> buildAllowedErrorCodes() {
        Set<String> set = new HashSet<>();
        for (TenantAuthenticationErrorType type : TenantAuthenticationErrorType.values()) {
            set.add(type.name());
        }
        return Collections.unmodifiableSet(set);
    }

    /**
     * ログインページを表示する。
     *
     * @param error 空文字または TenantAuthenticationErrorType のenum名
     * @param logout ログアウト完了を示すクエリパラメータ（存在すれば文字列、なければnull）
     * @param model ビューに渡すモデル
     * @return テンプレート名
     */
    @GetMapping("/login")
    public String showLoginPage(
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String logout,
            Model model) {
        boolean hasError = error != null;
        model.addAttribute("error", hasError);
        model.addAttribute("errorCode", hasError ? normalizeErrorCode(error) : null);
        model.addAttribute("logout", logout != null);
        return "tenant/login";
    }

    /**
     * エラーコードをホワイトリスト検証し、不明なコードはunknownに正規化する。
     *
     * @param raw クエリで受け取ったエラーコード
     * @return 正規化済みのエラーコード
     */
    private String normalizeErrorCode(String raw) {
        if (raw == null || raw.isBlank()) {
            return ERROR_CODE_UNKNOWN;
        }
        if (ALLOWED_ERROR_CODES.contains(raw)) {
            return raw;
        }
        return ERROR_CODE_UNKNOWN;
    }

}
