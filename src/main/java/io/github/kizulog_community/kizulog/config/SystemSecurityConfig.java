package io.github.kizulog_community.kizulog.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.AndRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;

import io.github.kizulog_community.kizulog.infrastructure.security.client.DynamicSystemClientRegistrationRepository;
import io.github.kizulog_community.kizulog.infrastructure.security.handler.SystemAuthenticationFailureHandler;
import io.github.kizulog_community.kizulog.infrastructure.security.handler.SystemAuthenticationSuccessHandler;
import io.github.kizulog_community.kizulog.infrastructure.security.matcher.SystemHostMatcher;
import io.github.kizulog_community.kizulog.infrastructure.security.oidc.SystemOidcUserService;
import io.github.kizulog_community.kizulog.infrastructure.security.principal.SystemUserPrincipal;

/**
 * システム管理画面用のSpring Security設定
 *
 * <p>担当URL:
 * <ul>
 * <li>/system/invite/**        - 招待受諾フロー（認証不要）</li>
 * <li>/system/**               - システム管理画面（/system/loginを除き認証必須）</li>
 * <li>/oauth2/authorization/** - OAuth2認可開始（Spring Security固定パス）</li>
 * <li>/login/oauth2/code/**    - OAuth2コールバック（Spring Security固定パス）</li>
 * <li>/logout                  - ログアウト</li>
 * </ul>
 * </p>
 *
 * <p>認証失敗ハンドラ:
 * SystemAuthenticationFailureHandler が
 * OAuth2AuthenticationExceptionのerrorCodeを判定してリダイレクトする。
 * <ul>
 * <li>InvitationError系 → /system/invite/error?code={errorCode}</li>
 * <li>それ以外          → /system/login?error={errorCode}</li>
 * </ul>
 * </p>
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
     * @param systemAuthenticationSuccessHandler 認証成功時のハンドラ
     * @param systemAuthenticationFailureHandler 認証失敗時のハンドラ
     * @param tokenResponseClientProvider オプショナル：トークンエンドポイント呼び出し用クライアント
     * @return SecurityFilterChain
     * @throws Exception 設定エラー
     */
    @Bean
    @Order(1)
    SecurityFilterChain systemSecurityFilterChain(
            HttpSecurity http,
            SystemHostMatcher systemHostMatcher,
            DynamicSystemClientRegistrationRepository clientRegistrationRepository,
            SystemOidcUserService systemOidcUserService,
            SystemAuthenticationSuccessHandler systemAuthenticationSuccessHandler,
            SystemAuthenticationFailureHandler systemAuthenticationFailureHandler,
            ObjectProvider<OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest>>
                    tokenResponseClientProvider) throws Exception {

        // 担当パス（システムホスト宛のときのみ）
        PathPatternRequestMatcher.Builder paths = PathPatternRequestMatcher.withDefaults();
        OrRequestMatcher systemPaths = new OrRequestMatcher(
                paths.matcher("/system/**"),
                paths.matcher("/oauth2/authorization/**"),
                paths.matcher("/login/oauth2/code/**"),
                paths.matcher("/logout"));
        // 「システムホスト宛」かつ「担当パス」のときのみ、このFilterChainが処理する。
        // テナントホスト宛の同一パスはマッチせず、@Order(2)のテナントFilterChainへ流れる。
        http.securityMatcher(new AndRequestMatcher(systemHostMatcher, systemPaths))
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
                .successHandler(systemAuthenticationSuccessHandler)
                .failureHandler(systemAuthenticationFailureHandler))
            .logout(logout -> logout
                .logoutSuccessUrl("/system/login?logout")
                .invalidateHttpSession(true)
                .clearAuthentication(true));

        return http.build();
    }

}
