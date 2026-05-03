package io.github.kizulog_community.kizulog.infrastructure.web.system;

import java.util.Set;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import io.github.kizulog_community.kizulog.domain.systemauth.exception.SystemAuthenticationErrorType;

/**
 * システム管理ログインコントローラー
 *
 * <p>システム管理画面のログインページを表示する。
 * 実際の認証はSpring SecurityのOAuth2 Loginフロー（/oauth2/authorization/master）に委譲。</p>
 *
 * <p>URL設計:
 * <ul>
 *   <li>/system/login: ログインページ表示</li>
 *   <li>/system/login?error: 認証失敗（エラーコード不明）</li>
 *   <li>/system/login?error=ACCOUNT_NOT_FOUND: アカウント未登録</li>
 *   <li>/system/login?error=ACCOUNT_INACTIVE: アカウント無効</li>
 *   <li>/system/login?error=ROLE_NOT_GRANTED: SYSTEM_ADMINロール無し</li>
 *   <li>/system/login?logout: ログアウト完了</li>
 * </ul>
 *
 * <p>エラーコードは SystemAuthenticationErrorType のenum名のホワイトリスト検証を行う。
 * 不明なコードは unknown に正規化して画面に渡す（XSS対策）。</p>
 *
 * @author Jun Kobayashi
 */
@Controller
@RequestMapping("/system")
public class SystemLoginController {

    /** OAuth2認可開始エンドポイント（Spring Security固定パス） */
    private static final String OAUTH2_LOGIN_URL = "/oauth2/authorization/master";

    /** 不明なエラーコードを示すマーカー */
    static final String ERROR_CODE_UNKNOWN = "unknown";

    /** 許容エラーコードのホワイトリスト（SystemAuthenticationErrorType由来） */
    private static final Set<String> ALLOWED_ERROR_CODES = buildAllowedErrorCodes();

    private static Set<String> buildAllowedErrorCodes() {
        Set<String> set = new java.util.HashSet<>();
        for (SystemAuthenticationErrorType type : SystemAuthenticationErrorType.values()) {
            set.add(type.name());
        }
        return java.util.Collections.unmodifiableSet(set);
    }

    /**
     * ログインページを表示する。
     *
     * @param error 空文字または SystemAuthenticationErrorType のenum名。
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
        model.addAttribute("oauth2LoginUrl", OAUTH2_LOGIN_URL);
        return "system/login";
    }

    /**
     * エラーコードをホワイトリスト検証し、不明なコードはunknownに正規化する。
     *
     * <p>クエリ値をそのまま画面に出すとXSSリスクがあるため、
     * 既知のenum名にマッチしない場合は安全な定数値に置き換える。</p>
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
