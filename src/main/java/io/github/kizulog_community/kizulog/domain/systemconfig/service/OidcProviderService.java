package io.github.kizulog_community.kizulog.domain.systemconfig.service;

import java.net.ConnectException;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import io.github.kizulog_community.kizulog.domain.systemconfig.exception.OidcConnectionError;
import io.github.kizulog_community.kizulog.domain.systemconfig.exception.OidcConnectionException;

/**
 * OIDCプロバイダーサービス
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

    /** RestClient */
    private final RestClient restClient;

    /** ObjectMapper */
    private final ObjectMapper objectMapper;

    /**
     * コンストラクタ
     */
    public OidcProviderService(RestClient restClient, ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
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
        String response = fetchDiscoveryDocument(issuerUri);
        if (!response.contains("issuer")) {
            throw new OidcConnectionException(OidcConnectionError.INVALID_RESPONSE);
        }
    }

    /**
     * OIDCプロバイダーのメタデータを取得
     *
     * @param issuerUri Issuer URI
     * @return メタデータ（authorization_endpoint・token_endpoint等）
     * @throws OidcConnectionException メタデータ取得に失敗した場合
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getMetadata(String issuerUri) {
        try {
            String response = fetchDiscoveryDocument(issuerUri);
            return objectMapper.readValue(response, Map.class);
        } catch (OidcConnectionException e) {
            throw e;
        } catch (Exception e) {
            log.error("OIDCメタデータのパースに失敗しました: issuerUri={}", issuerUri, e);
            throw new OidcConnectionException(OidcConnectionError.UNEXPECTED_ERROR, e);
        }
    }

    /**
     * stateパラメーターを生成
     *
     * @return ランダムなstate文字列
     */
    public String generateState() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * 認証URLを生成
     *
     * @param authorizationEndpoint 認証エンドポイント
     * @param clientId Client ID
     * @param redirectUri コールバックURI
     * @param state CSRF防止用state
     * @return 認証URL
     */
    public String buildAuthorizationUrl(
            String authorizationEndpoint, String clientId
            , String redirectUri, String state) {
        return authorizationEndpoint
                + "?response_type=code"
                + "&client_id=" + encode(clientId)
                + "&redirect_uri=" + encode(redirectUri)
                + "&scope=" + encode("openid profile email")
                + "&state=" + encode(state);
    }

    /**
     * 認可コードをIDトークンのクレームに交換
     *
     * @param tokenEndpoint トークンエンドポイント
     * @param code 認可コード
     * @param clientId Client ID
     * @param clientSecret Client Secret
     * @param redirectUri コールバックURI
     * @return IDトークンのクレーム情報
     * @throws OidcConnectionException トークン交換に失敗した場合
     */
    @SuppressWarnings("unchecked")
    public JWTClaimsSet exchangeCodeForClaims(
            String tokenEndpoint, String code, String clientId
            , String clientSecret, String redirectUri) {
        try {
            String body = "grant_type=authorization_code"
                    + "&code=" + encode(code)
                    + "&client_id=" + encode(clientId)
                    + "&client_secret=" + encode(clientSecret)
                    + "&redirect_uri=" + encode(redirectUri);

            ResponseEntity<String> response = restClient.post()
                    .uri(tokenEndpoint)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .body(body)
                    .retrieve()
                    .toEntity(String.class);

            Map<String, Object> tokenResponse = objectMapper.readValue(
                    response.getBody(), Map.class);

            if (tokenResponse.containsKey("error")) {
                log.error("トークン交換エラー: {}", tokenResponse.get("error_description"));
                throw new OidcConnectionException(OidcConnectionError.UNEXPECTED_ERROR);
            }

            String idToken = (String) tokenResponse.get("id_token");
            if (idToken == null) {
                throw new OidcConnectionException(OidcConnectionError.INVALID_RESPONSE);
            }

            SignedJWT signedJWT = SignedJWT.parse(idToken);
            return signedJWT.getJWTClaimsSet();

        } catch (OidcConnectionException e) {
            throw e;
        } catch (Exception e) {
            log.error("トークン交換に失敗しました", e);
            throw new OidcConnectionException(OidcConnectionError.UNEXPECTED_ERROR, e);
        }
    }

    /**
     * URLエンコード
     *
     * @param value エンコード対象文字列
     * @return エンコード済み文字列
     */
    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /**
     * OIDCディスカバリードキュメントを取得
     *
     * @param issuerUri Issuer URI
     * @return ディスカバリードキュメントのJSON文字列
     * @throws OidcConnectionException 取得に失敗した場合
     */
    private String fetchDiscoveryDocument(String issuerUri) {
        try {
            String discoveryUrl = issuerUri.stripTrailing()
                    + "/.well-known/openid-configuration";
            String response = restClient.get()
                    .uri(discoveryUrl)
                    .retrieve()
                    .body(String.class);
            if (response == null) {
                throw new OidcConnectionException(OidcConnectionError.INVALID_RESPONSE);
            }
            return response;
        } catch (OidcConnectionException e) {
            throw e;
        } catch (ResourceAccessException e) {
            Throwable cause = e.getCause();
            if (cause instanceof UnknownHostException
                    || cause instanceof ConnectException) {
                log.warn("OIDC接続エラー: issuerUri={}, cause={}",
                        issuerUri, cause.getMessage());
                throw new OidcConnectionException(
                        OidcConnectionError.CONNECTION_ERROR, e);
            }
            log.error("OIDC接続確認で予期しないエラー: issuerUri={}", issuerUri, e);
            throw new OidcConnectionException(
                    OidcConnectionError.UNEXPECTED_ERROR, e);
        } catch (Exception e) {
            log.error("OIDC接続確認で予期しないエラー: issuerUri={}", issuerUri, e);
            throw new OidcConnectionException(
                    OidcConnectionError.UNEXPECTED_ERROR, e);
        }
    }

}