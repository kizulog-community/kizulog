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
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;

import io.github.kizulog_community.kizulog.domain.tenant.port.TenantHostRepository;
import io.github.kizulog_community.kizulog.domain.tenant.port.TenantHostStatusRepository;
import io.github.kizulog_community.kizulog.domain.tenant.port.TenantRepository;
import io.github.kizulog_community.kizulog.domain.tenant.port.TenantStatusRepository;
import io.github.kizulog_community.kizulog.infrastructure.security.client.DynamicTenantClientRegistrationRepository;
import io.github.kizulog_community.kizulog.infrastructure.security.handler.TenantAuthenticationFailureHandler;
import io.github.kizulog_community.kizulog.infrastructure.security.handler.TenantAuthenticationSuccessHandler;
import io.github.kizulog_community.kizulog.infrastructure.security.matcher.SystemHostMatcher;
import io.github.kizulog_community.kizulog.infrastructure.security.oidc.TenantOidcUserService;
import io.github.kizulog_community.kizulog.infrastructure.security.principal.TenantUserPrincipal;
import io.github.kizulog_community.kizulog.infrastructure.web.tenant.TenantResolverFilter;

/**
 * テナント利用者画面用のSpring Security設定
 *
 * <p>テナント識別方式: ホストベース（案S2）。
 * リクエストのホストがシステムホスト（kizulog.system.host）でない場合、
 * このFilterChain（@Order(2)）がテナント候補として処理する。</p>
 *
 * <p>担当範囲（システムホストでない全リクエスト）:
 * <ul>
 * <li>/login                   - テナントログイン画面（認証不要）</li>
 * <li>/oauth2/authorization/** - OAuth2認可開始（Spring Security固定パス）</li>
 * <li>/login/oauth2/code/**    - OAuth2コールバック（Spring Security固定パス）</li>
 * <li>/admin/**                - テナント管理画面（ROLE_TENANT_ADMIN必須）</li>
 * <li>/                        - テナントトップ（認証必須）</li>
 * <li>静的リソース・/error      - 認証不要</li>
 * </ul>
 * </p>
 *
 * <p>OAuth2のregistrationIdは tenant-{tenantId}-{providerId} 形式。
 * DynamicTenantClientRegistrationRepository が動的にClientRegistrationを構築する。</p>
 *
 * <p>FilterChainの評価順:
 * <ol>
 * <li>@Order(1) SystemSecurityConfig: システムホスト かつ システムパス</li>
 * <li>@Order(2) TenantSecurityConfig（本クラス）: システムホストでない全リクエスト</li>
 * <li>@Order(3) SecurityConfig: フォールバック（システムホスト宛のsetup・静的等）</li>
 * </ol>
 * </p>
 *
 * @author Jun Kobayashi
 */
@Configuration
@EnableWebSecurity
public class TenantSecurityConfig {

    /**
     * テナント識別フィルタの Bean 定義
     *
     * @param systemHostMatcher システムホスト判定マッチャ
     * @param tenantRepository テナントリポジトリ
     * @param tenantStatusRepository テナントステータスリポジトリ
     * @param tenantHostRepository テナントホストリポジトリ
     * @param tenantHostStatusRepository テナントホストステータスリポジトリ
     * @return TenantResolverFilter
     */
    @Bean
    TenantResolverFilter tenantResolverFilter(
            SystemHostMatcher systemHostMatcher,
            TenantRepository tenantRepository,
            TenantStatusRepository tenantStatusRepository,
            TenantHostRepository tenantHostRepository,
            TenantHostStatusRepository tenantHostStatusRepository) {
        return new TenantResolverFilter(
                systemHostMatcher,
                tenantRepository,
                tenantStatusRepository,
                tenantHostRepository,
                tenantHostStatusRepository);
    }

    /**
     * テナント側のSecurityFilterChain
     *
     * @param http Spring SecurityのHttpSecurity
     * @param systemHostMatcher システムホスト判定マッチャ
     * @param tenantResolverFilter テナント識別フィルタ
     * @param clientRegistrationRepository OIDC設定をDBから動的構築するリポジトリ
     * @param tenantOidcUserService 認証完了時にテナントアカウントと突合するOidcUserService
     * @param tenantAuthenticationSuccessHandler 認証成功時のハンドラ
     * @param tenantAuthenticationFailureHandler 認証失敗時のハンドラ
     * @param tokenResponseClientProvider オプショナル：トークンエンドポイント呼び出し用クライアント
     * @return SecurityFilterChain
     * @throws Exception 設定エラー
     */
    @Bean
    @Order(2)
    SecurityFilterChain tenantSecurityFilterChain(
            HttpSecurity http,
            SystemHostMatcher systemHostMatcher,
            TenantResolverFilter tenantResolverFilter,
            DynamicTenantClientRegistrationRepository clientRegistrationRepository,
            TenantOidcUserService tenantOidcUserService,
            TenantAuthenticationSuccessHandler tenantAuthenticationSuccessHandler,
            TenantAuthenticationFailureHandler tenantAuthenticationFailureHandler,
            ObjectProvider<OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest>>
                    tokenResponseClientProvider) throws Exception {

        // システムホストでない全リクエストをこのFilterChainが担当（テナント候補）
        http.securityMatcher(new NegatedRequestMatcher(systemHostMatcher))
            .authorizeHttpRequests(auth -> auth
                // ログイン画面・OAuth2フロー関連は認証不要
                .requestMatchers("/login").permitAll()
                .requestMatchers("/oauth2/authorization/**").permitAll()
                .requestMatchers("/login/oauth2/code/**").permitAll()
                // 静的リソース・エラーページは認証不要
                .requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico")
                    .permitAll()
                .requestMatchers("/error").permitAll()
                // 管理画面はTENANT_ADMINロール必須
                .requestMatchers("/admin/**")
                    .hasAuthority(TenantUserPrincipal.ROLE_TENANT_ADMIN)
                // それ以外は認証必須
                .anyRequest().authenticated())
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/login")
                .clientRegistrationRepository(clientRegistrationRepository)
                .userInfoEndpoint(userInfo -> userInfo
                    .oidcUserService(tenantOidcUserService))
                // tokenResponseClient Beanが提供されている場合、tokenエンドポイント呼び出しに使用
                // 提供されていない場合（本番）はSpring Securityのデフォルトを使用
                .tokenEndpoint(token -> {
                    OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> client =
                            tokenResponseClientProvider.getIfAvailable();
                    if (client != null) {
                        token.accessTokenResponseClient(client);
                    }
                })
                .successHandler(tenantAuthenticationSuccessHandler)
                .failureHandler(tenantAuthenticationFailureHandler))
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .clearAuthentication(true))
            // テナント識別フィルタを認証フィルタの前に挿入
            .addFilterBefore(tenantResolverFilter,
                    UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

}
