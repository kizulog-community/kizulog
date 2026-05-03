package io.github.kizulog_community.kizulog.config;

import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.endpoint.RestClientAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoderFactory;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

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
     * 全証明書を信頼するTrustManager配列を構築
     *
     * @return TrustManager配列
     */
    private static TrustManager[] buildTrustAllManagers() {
        return new TrustManager[] {
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
    }

    /**
     * SSL検証をスキップするSSLContextを構築
     *
     * @return SSLContext
     * @throws Exception SSLContext構築失敗時
     */
    private static SSLContext buildInsecureSslContext() throws Exception {
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, buildTrustAllManagers(), new java.security.SecureRandom());
        return sslContext;
    }

    /**
     * SSL検証をスキップするJdkClientHttpRequestFactoryを構築
     *
     * @return JdkClientHttpRequestFactory
     * @throws Exception SSLContext構築失敗時
     */
    private static JdkClientHttpRequestFactory buildInsecureRequestFactory() throws Exception {
        java.net.http.HttpClient httpClient = java.net.http.HttpClient.newBuilder()
                .sslContext(buildInsecureSslContext())
                .build();
        return new JdkClientHttpRequestFactory(httpClient);
    }

    /**
     * 全証明書を信頼するJVMデフォルトSSLSocketFactoryを設定す
     *
     * @throws Exception SSLContext構築失敗時
     */
    private static void configureGlobalInsecureSsl() throws Exception {
        SSLSocketFactory factory = buildInsecureSslContext().getSocketFactory();
        HttpsURLConnection.setDefaultSSLSocketFactory(factory);
    }

    /**
     * SSL検証をスキップするRestClient Bean
     *
     * @return RestClient
     * @throws Exception SSLContext構築に失敗した場合
     */
    @Bean
    RestClient restClient() throws Exception {
        return RestClient.builder()
                .requestFactory(buildInsecureRequestFactory())
                .build();
    }

    /**
     * OAuth2 Authorization Code Grant用のTokenResponseClient Bean
     *
     * @return OAuth2AccessTokenResponseClient
     * @throws Exception SSLContext構築に失敗した場合
     */
    @Bean
    OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest>
            authorizationCodeTokenResponseClient() throws Exception {

        RestClient insecureRestClient = RestClient.builder()
                .requestFactory(buildInsecureRequestFactory())
                .configureMessageConverters(builder ->
                    builder.configureMessageConvertersList(converters -> {
                        converters.clear();
                        converters.add(new FormHttpMessageConverter());
                        converters.add(new OAuth2AccessTokenResponseHttpMessageConverter());
                    }))
                .defaultStatusHandler(new OAuth2ErrorResponseErrorHandler())
                .build();

        RestClientAuthorizationCodeTokenResponseClient client =
                new RestClientAuthorizationCodeTokenResponseClient();
        client.setRestClient(insecureRestClient);
        return client;
    }

    /**
     * ID Token検証用のJwtDecoderFactory Bean
     *
     * @return JwtDecoderFactory
     * @throws Exception SSLContext構築失敗時
     */
    @Bean
    JwtDecoderFactory<ClientRegistration> idTokenDecoderFactory() throws Exception {
        configureGlobalInsecureSsl();
        RestTemplate insecureRestTemplate = new RestTemplate();

        Map<String, JwtDecoder> decoderCache = new HashMap<String, JwtDecoder>();

        return clientRegistration -> decoderCache.computeIfAbsent(
                clientRegistration.getRegistrationId(),
                _ -> NimbusJwtDecoder
                        .withJwkSetUri(clientRegistration.getProviderDetails().getJwkSetUri())
                        .restOperations(insecureRestTemplate)
                        .build());
    }

}
