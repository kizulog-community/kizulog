package io.github.kizulog_community.kizulog.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestClient;

/**
 * RestClient設定（本番・通常用）
 *
 * <p>localプロファイル以外で適用される。証明書検証を行う標準RestClientを提供する。</p>
 *
 * @author Jun Kobayashi
 */
@Configuration
@Profile("!local")
public class RestClientConfig {

    @Bean
    RestClient restClient() {
        return RestClient.create();
    }

}