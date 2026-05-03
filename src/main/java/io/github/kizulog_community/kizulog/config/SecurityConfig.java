package io.github.kizulog_community.kizulog.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security設定クラス（汎用）
 *
 * <p>SystemSecurityConfigでカバーされない汎用URLの設定を担当する。
 * 主にセットアップウィザード（{@code /setup/**}）やトップページなど、
 * システム管理画面以外のリクエストを受け持つ。</p>
 *
 * <p>Order=2 で SystemSecurityConfig Order=1 の後に評価される。</p>
 *
 * @author Jun Kobayashi
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * 汎用セキュリティフィルターチェーンの設定
     *
     * <p>セットアップウィザード画面とその他汎用画面は認証なしでアクセス可能にする。</p>
     *
     * @param http HttpSecurity
     * @return SecurityFilterChain
     * @throws Exception 設定エラー
     */
    @Bean
    @Order(2)
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/setup/**").permitAll()
                .anyRequest().permitAll() // 暫定：その他全許可（テナント機能実装後に変更）
            )
            .csrf(csrf -> csrf.disable()); // 暫定：CSRF無効（テナント機能実装後に有効化）
        return http.build();
    }

}
