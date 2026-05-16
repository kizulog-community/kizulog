package io.github.kizulog_community.kizulog.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import io.github.kizulog_community.kizulog.domain.tenant.port.TenantHostRepository;
import io.github.kizulog_community.kizulog.domain.tenant.port.TenantHostStatusRepository;
import io.github.kizulog_community.kizulog.domain.tenant.port.TenantRepository;
import io.github.kizulog_community.kizulog.domain.tenant.port.TenantStatusRepository;
import io.github.kizulog_community.kizulog.infrastructure.web.tenant.TenantResolverFilter;

/**
 * テナント側Spring Security設定
 *
 * <p>担当URL: /t/**</p>
 * <p>テナント識別方式: https://{host}/t/{slug}/...</p>
 *
 * @author Jun Kobayashi
 */
@Configuration
@EnableWebSecurity
public class TenantSecurityConfig {

    /**
     * テナント識別フィルタの Bean 定義
     *
     * @param tenantRepository テナントリポジトリ
     * @param tenantStatusRepository テナントステータスリポジトリ
     * @param tenantHostRepository テナントホストリポジトリ
     * @param tenantHostStatusRepository テナントホストステータスリポジトリ
     * @return TenantResolverFilter
     */
    @Bean
    TenantResolverFilter tenantResolverFilter(
            TenantRepository tenantRepository,
            TenantStatusRepository tenantStatusRepository,
            TenantHostRepository tenantHostRepository,
            TenantHostStatusRepository tenantHostStatusRepository) {
        return new TenantResolverFilter(
                tenantRepository,
                tenantStatusRepository,
                tenantHostRepository,
                tenantHostStatusRepository);
    }

    /**
     * テナント側のSecurityFilterChain
     *
     * @param http Spring SecurityのHttpSecurity
     * @param tenantResolverFilter テナント識別フィルタ
     * @return SecurityFilterChain
     * @throws Exception 設定エラー
     */
    @Bean
    @Order(0)
    SecurityFilterChain tenantSecurityFilterChain(
            HttpSecurity http,
            TenantResolverFilter tenantResolverFilter) throws Exception {

        http.securityMatcher("/t/**")
            .authorizeHttpRequests(auth -> auth
                // /t/{slug}/login は公開
                .requestMatchers("/t/*/login").permitAll()
                // /t/{slug}/login/** も公開
                .requestMatchers("/t/*/login/**").permitAll()
                // それ以外は認証必須
                .anyRequest().authenticated())
            // テナント識別フィルタを認証フィルタの前に挿入
            .addFilterBefore(tenantResolverFilter,
                    UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

}
