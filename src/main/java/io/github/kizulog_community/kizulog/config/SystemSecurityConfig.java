package io.github.kizulog_community.kizulog.config;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import io.github.kizulog_community.kizulog.domain.systemadmininvitation.exception.InvitationError;
import io.github.kizulog_community.kizulog.infrastructure.security.client.DynamicSystemClientRegistrationRepository;
import io.github.kizulog_community.kizulog.infrastructure.security.oidc.SystemOidcUserService;
import io.github.kizulog_community.kizulog.infrastructure.security.principal.SystemUserPrincipal;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * システム管理画面用のSpring Security設定
 *
 * <p>/system/**配下のシステム管理画面と、その認証に関連するURLを担当する。
 * OAuth2 / OIDCでの認証を必須とし、SYSTEM_ADMINロールを持つアカウントのみアクセス可能。</p>
 *
 * <p>Order=1で既存の汎用SecurityConfigより先に評価される。
 * securityMatcherでマッチしないリクエストは次のFilterChainにフォールバックする。</p>
 *
 * <p>担当URL:
 * <ul>
 * <li>/system/invite/**        - 招待受諾フロー（認証不要）</li>
 * <li>/system/**               - システム管理画面（/system/loginを除き認証必須）</li>
 * <li>/oauth2/authorization/** - OAuth2認可開始（Spring Security固定パス）</li>
 * <li>/login/oauth2/code/**    - OAuth2コールバック（Spring Security固定パス）</li>
 * <li>/logout                  - ログアウト</li>
 * </ul>
 *
 * <p>認証失敗ハンドラ:
 * OAuth2AuthenticationExceptionのerrorCodeを判定して、
 * <ul>
 * <li>InvitationError系（IDENTITY_EXISTS, INVITATION_NOT_FOUND等）→ /system/invite/error?code={errorCode}にリダイレクト</li>
 * <li>それ以外                                                    → /system/login?error={errorCode}にリダイレクト</li>
 * </ul>
 *
 * @author Jun Kobayashi
 */
@Configuration
@EnableWebSecurity
public class SystemSecurityConfig {

    /**
     * システム管理画面用のSecurityFilterChain
     *
     * @param http Spring SecurityのHttpSecurity
     * @param clientRegistrationRepository OIDC設定をDBから動的構築するリポジトリ
     * @param systemOidcUserService 認証完了時にKizuLogアカウントと突合するOidcUserService
     * @param tokenResponseClientProvider オプショナル：トークンエンドポイント呼び出し用クライアント
     * @return SecurityFilterChain
     * @throws Exception 設定エラー
     */
    @Bean
    @Order(1)
    SecurityFilterChain systemSecurityFilterChain(
            HttpSecurity http,
            DynamicSystemClientRegistrationRepository clientRegistrationRepository,
            SystemOidcUserService systemOidcUserService,
            ObjectProvider<OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest>>
                    tokenResponseClientProvider) throws Exception {

        http.securityMatcher(
                "/system/**",
                "/oauth2/authorization/**",
                "/login/oauth2/code/**",
                "/logout")
            .authorizeHttpRequests(auth -> auth
                // ログインページとOAuth2フロー関連は認証不要
                .requestMatchers("/system/login").permitAll()
                // 招待受諾フロー(I.0)は認証不要
                .requestMatchers("/system/invite/**").permitAll()
                .requestMatchers("/oauth2/authorization/**").permitAll()
                .requestMatchers("/login/oauth2/code/**").permitAll()
                // それ以外の/system/**はSYSTEM_ADMINロール必須
                .requestMatchers("/system/**")
                    .hasAuthority(SystemUserPrincipal.ROLE_SYSTEM_ADMIN)
                .anyRequest().authenticated())
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/system/login")
                .clientRegistrationRepository(clientRegistrationRepository)
                .userInfoEndpoint(userInfo -> userInfo
                    .oidcUserService(systemOidcUserService))
                // tokenResponseClient Beanが提供されている場合、tokenエンドポイント呼び出しに使用
                // 提供されていない場合（本番）はSpring Securityのデフォルトを使用
                .tokenEndpoint(token -> {
                    OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> client =
                            tokenResponseClientProvider.getIfAvailable();
                    if (client != null) {
                        token.accessTokenResponseClient(client);
                    }
                })
                .defaultSuccessUrl("/system/dashboard", true)
                .failureHandler(authenticationFailureHandler()))
            .logout(logout -> logout
                .logoutSuccessUrl("/system/login?logout")
                .invalidateHttpSession(true)
                .clearAuthentication(true));

        return http.build();
    }

    /**
     * 認証失敗時のハンドラ
     *
     * <p>OAuth2AuthenticationExceptionのerrorCodeで宛先を分岐する。
     * <ul>
     * <li>InvitationError系 → /system/invite/error?code={errorCode}</li>
     * <li>それ以外 → /system/login?error={errorCode}</li>
     * </ul>
     * クエリ値はURLエンコードされる。各遷移先側でホワイトリスト検証する前提。</p>
     *
     * @return AuthenticationFailureHandler
     */
    private AuthenticationFailureHandler authenticationFailureHandler() {
        return new SystemAuthenticationFailureHandler();
    }

    /**
     * 認証失敗時のハンドラ実装
     *
     * <p>OAuth2AuthenticationExceptionからerrorCodeを抽出してリダイレクトURLに付与する。
     * InvitationError由来のerrorCodeは招待エラー画面へ、それ以外はログイン画面エラーへ振り分ける。</p>
     */
    static class SystemAuthenticationFailureHandler implements AuthenticationFailureHandler {

        /** デフォルトのリダイレクト先（エラーコード不明時） */
        private static final String DEFAULT_FAILURE_URL = "/system/login?error";

        /** ログインエラー時のリダイレクト先テンプレート */
        private static final String LOGIN_FAILURE_URL_WITH_CODE = "/system/login?error=";

        /** 招待エラー時のリダイレクト先テンプレート */
        private static final String INVITE_FAILURE_URL_WITH_CODE = "/system/invite/error?code=";

        @Override
        public void onAuthenticationFailure(
                HttpServletRequest request,
                HttpServletResponse response,
                AuthenticationException exception) throws IOException, ServletException {

            String redirectUrl = DEFAULT_FAILURE_URL;
            if (exception instanceof OAuth2AuthenticationException oae) {
                String errorCode = oae.getError().getErrorCode();
                if (errorCode != null && !errorCode.isBlank()) {
                    String encoded = URLEncoder.encode(errorCode, StandardCharsets.UTF_8);
                    if (isInvitationError(errorCode)) {
                        redirectUrl = INVITE_FAILURE_URL_WITH_CODE + encoded;
                    } else {
                        redirectUrl = LOGIN_FAILURE_URL_WITH_CODE + encoded;
                    }
                }
            }
            response.sendRedirect(request.getContextPath() + redirectUrl);
        }

        /**
         * エラーコードが InvitationError 由来か判定する。
         *
         * <p>InvitationError のenum名と完全一致する場合のみ true。</p>
         */
        private boolean isInvitationError(String errorCode) {
            for (InvitationError e : InvitationError.values()) {
                if (e.name().equals(errorCode)) {
                    return true;
                }
            }
            return false;
        }

    }

}