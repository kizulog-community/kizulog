package io.github.kizulog_community.kizulog.domain.systemconfig.service;

import java.net.ConnectException;
import java.net.UnknownHostException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import io.github.kizulog_community.kizulog.domain.systemconfig.exception.OidcConnectionError;
import io.github.kizulog_community.kizulog.domain.systemconfig.exception.OidcConnectionException;

/**
 * OIDCプロバイダーサービス.
 *
 * <p>OIDCプロバイダーとの接続・通信を全般的に担う。</p>
 *
 * @author Jun Kobayashi
 */
@Service
public class OidcProviderService {

    /** ロガー */
    private static final Logger log =
            LoggerFactory.getLogger(OidcProviderService.class);

    /** RestClient. */
    private final RestClient restClient;

    /**
     * コンストラクタ.
     */
    public OidcProviderService() {
        this.restClient = RestClient.create();
    }

    /**
     * OIDCプロバイダーへの接続確認
     *
     * <p>Issuer URIの/.well-known/openid-configurationにアクセスして
     * OIDCプロバイダーとして正しく動作しているか確認する。
     * このエンドポイントはOpenID Connect Discovery 1.0で標準化されている。</p>
     *
     * @param issuerUri OIDCプロバイダーのIssuerURI
     * @throws OidcConnectionException 接続確認に失敗した場合
     */
    public void verify(String issuerUri) {
        if (issuerUri == null || issuerUri.isBlank()) {
            throw new OidcConnectionException(OidcConnectionError.INPUT_ERROR);
        }
        try {
            String discoveryUrl = issuerUri.stripTrailing()
                    + "/.well-known/openid-configuration";
            String response = restClient.get()
                    .uri(discoveryUrl)
                    .retrieve()
                    .body(String.class);

            if (response == null || !response.contains("issuer")) {
                throw new OidcConnectionException(
                        OidcConnectionError.INVALID_RESPONSE);
            }

        } catch (OidcConnectionException e) {
            throw e;

        } catch (ResourceAccessException e) {
            Throwable cause = e.getCause();
            
            if (cause instanceof UnknownHostException
                    || cause instanceof ConnectException) {
                log.warn("OIDC connection error: issuerUri={}, cause={}", issuerUri, cause.getMessage());
                throw new OidcConnectionException(OidcConnectionError.CONNECTION_ERROR, e);
            }
            
            log.error("Unexpected OIDC connection error: issuerUri={}", issuerUri, e);
            throw new OidcConnectionException(OidcConnectionError.UNEXPECTED_ERROR, e);

        } catch (Exception e) {
            log.error("Unexpected OIDC connection error: issuerUri={}", issuerUri, e);
            throw new OidcConnectionException(OidcConnectionError.UNEXPECTED_ERROR, e);
        }
    }

}