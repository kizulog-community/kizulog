package io.github.kizulog_community.kizulog.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security設定クラス
 *
 * @author Jun Kobayashi
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * セキュリティフィルターチェーンの設定
     *
     * <p>セットアップウィザード画面は認証なしでアクセス可能にする。</p>
     *
     * @param http HttpSecurity
     * @return SecurityFilterChain
     * @throws Exception 設定エラー
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
            .requestMatchers("/setup/**").permitAll()
            .anyRequest().permitAll()  // 暫定：全許可（OIDC実装後に変更）
         )
         .csrf(csrf -> csrf.disable()); // 暫定：CSRF無効（OIDC実装後に有効化）
        return http.build();
    }

}