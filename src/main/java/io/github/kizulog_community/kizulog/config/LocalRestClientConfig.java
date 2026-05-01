package io.github.kizulog_community.kizulog.config;

import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import jakarta.annotation.PostConstruct;

/**
 * RestClient設定（local開発専用）
 *
 * <p>localプロファイルでのみ適用される。
 * 自己署名証明書を許可するため証明書検証をスキップする。
 * 本番環境では絶対に使用してはならない。</p>
 *
 * @author Jun Kobayashi
 */
@Configuration
@Profile("local")
public class LocalRestClientConfig {

    /** ロガー */
    private static final Logger log =
            LoggerFactory.getLogger(LocalRestClientConfig.class);

    /**
     * 警告ログ出力
     */
    @PostConstruct
    public void warn() {
        log.warn("====================================================");
        log.warn(" SSL証明書検証が無効化されています（local profile）");
        log.warn(" この設定は開発用です。本番環境では絶対に使用しないでください。");
        log.warn("====================================================");
    }

    /**
     * SSL証明書検証をスキップするRestClient Bean
     *
     * @return RestClient
     * @throws Exception SSLContext構築に失敗した場合
     */
    @Bean
    RestClient restClient() throws Exception {
        TrustManager[] trustAllCerts = new TrustManager[] {
            new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) {
                    // 検証スキップ（local開発専用）
                }
                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) {
                    // 検証スキップ（local開発専用）
                }
                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            }
        };

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

        java.net.http.HttpClient httpClient = java.net.http.HttpClient.newBuilder()
                .sslContext(sslContext)
                .build();

        return RestClient.builder()
                .requestFactory(new JdkClientHttpRequestFactory(httpClient))
                .build();
    }

}