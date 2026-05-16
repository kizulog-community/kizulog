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
 * <p>担当するURL:</p>
 * <ul>
 * <li>/setup/**                   - セットアップウィザード（permitAll）</li>
 * <li>/css/**, /js/**, /images/** - 静的リソース</li>
 * <li>/favicon.ico                - ファビコン</li>
 * <li>/error                      - Spring Boot のエラーページ</li>
 * <li>未マッピングURL              - Spring標準動作で自動的に404</li>
 * </ul>
 *
 * @author Jun Kobayashi
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * 汎用セキュリティフィルターチェーンの設定
     *
     * @param http HttpSecurity
     * @return SecurityFilterChain
     * @throws Exception 設定エラー
     */
    @Bean
    @Order(3)
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                // セットアップウィザード（認証不要）
                .requestMatchers("/setup/**").permitAll()
                // 静的リソース
                .requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico")
                    .permitAll()
                // Spring Boot エラーページ
                .requestMatchers("/error").permitAll()
                // それ以外も permitAll：Controllerがマッピングされていない場合は
                // Spring が自動的に 404 を返す
                .anyRequest().permitAll()
            )
            // CSRF有効化（Spring Securityデフォルト）。ただし /setup/check-oidc は
            // AJAX呼び出しでCSRFトークン未送信のため除外（冪等な検証APIに限定）
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/setup/check-oidc"));
        return http.build();
    }

}
