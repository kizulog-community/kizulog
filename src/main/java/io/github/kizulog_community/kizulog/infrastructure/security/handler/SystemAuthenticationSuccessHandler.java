package io.github.kizulog_community.kizulog.infrastructure.security.handler;

import java.io.IOException;
import java.util.Optional;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.i18n.SimpleTimeZoneAwareLocaleContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

import io.github.kizulog_community.kizulog.domain.shared.SupportedLanguage;
import io.github.kizulog_community.kizulog.domain.shared.SupportedTimezone;
import io.github.kizulog_community.kizulog.domain.systemaccountlocalization.service.AccountLocalizationApplicationService;
import io.github.kizulog_community.kizulog.infrastructure.security.principal.SystemUserPrincipal;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * OIDCログイン成功時に、
 * 認証されたアカウントの言語・タイムゾーン設定をセッションに反映するハンドラ
 *
 * @author Jun Kobayashi
 */
@Component
public class SystemAuthenticationSuccessHandler
        extends SavedRequestAwareAuthenticationSuccessHandler {

    /** ロガー */
    private static final Logger log =
            LoggerFactory.getLogger(SystemAuthenticationSuccessHandler.class);

    /** ログイン成功後のデフォルト遷移先 */
    private static final String DEFAULT_TARGET_URL = "/system/dashboard";

    /** アカウントlocalizationサービス */
    private final AccountLocalizationApplicationService accountLocalizationApplicationService;

    /** ロケールリゾルバ */
    private final SessionLocaleResolver localeResolver;

    /**
     * コンストラクタ
     *
     * @param accountLocalizationApplicationService アカウントlocalizationサービス
     * @param localeResolver ロケールリゾルバ
     */
    public SystemAuthenticationSuccessHandler(
            AccountLocalizationApplicationService accountLocalizationApplicationService,
            SessionLocaleResolver localeResolver) {
        this.accountLocalizationApplicationService = accountLocalizationApplicationService;
        this.localeResolver = localeResolver;
        setDefaultTargetUrl(DEFAULT_TARGET_URL);
        setAlwaysUseDefaultTargetUrl(true);
    }

    /**
     * 認証成功時の処理
     *
     * @param request HTTPリクエスト
     * @param response HTTPレスポンス
     * @param authentication 認証情報
     * @throws IOException IO例外
     * @throws ServletException Servlet例外
     */
    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        try {
            applyLocaleAndTimezone(request, response, authentication);
        } catch (RuntimeException e) {
            // Locale反映の失敗で認証成功を妨げない（ログイン続行）
            log.warn("ログイン成功時のLocale/TimeZone反映に失敗しました。"
                    + "デフォルト設定で続行します。", e);
        }
        super.onAuthenticationSuccess(request, response, authentication);
    }

    /**
     * アカウントの有効な言語・TZをセッションに反映する。
     *
     * @param request HTTPリクエスト
     * @param response HTTPレスポンス
     * @param authentication 認証情報
     */
    private void applyLocaleAndTimezone(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) {

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof SystemUserPrincipal systemPrincipal)) {
            log.warn("Principalが想定外の型のためLocale反映をスキップします: type={}",
                    principal == null ? "null" : principal.getClass().getName());
            return;
        }

        String accountId = systemPrincipal.getAccountId();
        if (accountId == null || accountId.isBlank()) {
            log.warn("PrincipalにaccountIdが含まれていないためLocale反映をスキップします");
            return;
        }

        Optional<SupportedLanguage> langOpt =
                accountLocalizationApplicationService.resolveEffectiveLanguage(accountId);
        Optional<SupportedTimezone> tzOpt =
                accountLocalizationApplicationService.resolveEffectiveTimezone(accountId);

        // 両方が解決できる場合のみセッション反映
        // 片方でも空（システム設定未登録）の場合は LocaleConfig のフォールバックに委ねる
        if (langOpt.isEmpty() || tzOpt.isEmpty()) {
            log.info("有効な言語またはタイムゾーンが解決できないためLocale反映をスキップします: "
                            + "accountId={}, languagePresent={}, timezonePresent={}",
                    accountId, langOpt.isPresent(), tzOpt.isPresent());
            return;
        }

        SupportedLanguage language = langOpt.get();
        SupportedTimezone timezone = tzOpt.get();
        TimeZone javaTz = TimeZone.getTimeZone(timezone.getZoneId());

        localeResolver.setLocaleContext(
                request, response,
                new SimpleTimeZoneAwareLocaleContext(language.getLocale(), javaTz));

        log.info("ログイン成功時にLocale/TimeZoneをセッションに反映しました: "
                        + "accountId={}, language={}, timezone={}",
                accountId, language.getCode(), timezone.getId());
    }

}
