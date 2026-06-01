package io.github.kizulog_community.kizulog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

import io.github.kizulog_community.kizulog.config.OidcProfileClaimsProperties;

/**
 * KizuLog アプリケーションエントリポイント
 *
 * <p>Spring Bootアプリケーションのメインクラス。</p>
 *
 * @author Jun Kobayashi
 */
@SpringBootApplication(exclude = {UserDetailsServiceAutoConfiguration.class})
@EnableConfigurationProperties({OidcProfileClaimsProperties.class})
public class KizulogApplication {

    public static void main(String[] args) {
        SpringApplication.run(KizulogApplication.class, args);
    }

}
