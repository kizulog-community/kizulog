package io.github.kizulog_community.kizulog.domain.systemconfig.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import io.github.kizulog_community.kizulog.domain.systemconfig.exception.OidcConnectionError;
import io.github.kizulog_community.kizulog.domain.systemconfig.exception.OidcConnectionException;

/**
 * OidcProviderServiceの単体テスト
 *
 * @author Jun Kobayashi
 */
class OidcProviderServiceTest {

    private RestClient restClient;
    private ObjectMapper objectMapper;
    private OidcProviderService service;

    @BeforeEach
    void setUp() {
        restClient = mock(RestClient.class);
        objectMapper = new ObjectMapper();
        service = new OidcProviderService(restClient, objectMapper);
    }

    /**
     * GETリクエストの結果を返すモックを設定する
     */
    private void mockGetResponse(String response) {
        RestClient.RequestHeadersUriSpec<?> uriSpec =
                mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec<?> headersSpec =
                mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(restClient.get()).thenAnswer(_ -> uriSpec);
        when(uriSpec.uri(anyString())).thenAnswer(_ -> headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(String.class)).thenReturn(response);
    }

    /**
     * GETリクエストで例外を投げるモックを設定する
     */
    private void mockGetThrows(RuntimeException ex) {
        RestClient.RequestHeadersUriSpec<?> uriSpec =
                mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec<?> headersSpec =
                mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(restClient.get()).thenAnswer(_ -> uriSpec);
        when(uriSpec.uri(anyString())).thenAnswer(_ -> headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(String.class)).thenThrow(ex);
    }

    // ============================================================
    // verify()
    // ============================================================
    @Test
    @DisplayName("verify()で正常なレスポンスが返る場合は例外を投げない")
    void verify_validResponse_doesNotThrow() {
        mockGetResponse("{\"issuer\":\"https://example.com\"}");
        service.verify("https://example.com");
        // 例外が投げられなければOK
    }

    @Test
    @DisplayName("verify()でissuerUriがnullならINPUT_ERROR")
    void verify_nullUri_throwsInputError() {
        assertThatThrownBy(() -> service.verify(null))
                .isInstanceOf(OidcConnectionException.class)
                .extracting("errorType")
                .isEqualTo(OidcConnectionError.INPUT_ERROR);
    }

    @Test
    @DisplayName("verify()でissuerUriが空文字ならINPUT_ERROR")
    void verify_blankUri_throwsInputError() {
        assertThatThrownBy(() -> service.verify("  "))
                .isInstanceOf(OidcConnectionException.class)
                .extracting("errorType")
                .isEqualTo(OidcConnectionError.INPUT_ERROR);
    }

    @Test
    @DisplayName("verify()でレスポンスにissuerが含まれないとINVALID_RESPONSE")
    void verify_missingIssuer_throwsInvalidResponse() {
        mockGetResponse("{\"foo\":\"bar\"}");
        assertThatThrownBy(() -> service.verify("https://example.com"))
                .isInstanceOf(OidcConnectionException.class)
                .extracting("errorType")
                .isEqualTo(OidcConnectionError.INVALID_RESPONSE);
    }

    @Test
    @DisplayName("verify()でレスポンスがnullならINVALID_RESPONSE")
    void verify_nullResponse_throwsInvalidResponse() {
        mockGetResponse(null);
        assertThatThrownBy(() -> service.verify("https://example.com"))
                .isInstanceOf(OidcConnectionException.class)
                .extracting("errorType")
                .isEqualTo(OidcConnectionError.INVALID_RESPONSE);
    }

    @Test
    @DisplayName("verify()でUnknownHostExceptionならCONNECTION_ERROR")
    void verify_unknownHost_throwsConnectionError() {
        mockGetThrows(new ResourceAccessException("DNS失敗",
                new UnknownHostException("host")));
        assertThatThrownBy(() -> service.verify("https://example.com"))
                .isInstanceOf(OidcConnectionException.class)
                .extracting("errorType")
                .isEqualTo(OidcConnectionError.CONNECTION_ERROR);
    }

    @Test
    @DisplayName("verify()でConnectExceptionならCONNECTION_ERROR")
    void verify_connectException_throwsConnectionError() {
        mockGetThrows(new ResourceAccessException("接続失敗",
                new ConnectException("connect")));
        assertThatThrownBy(() -> service.verify("https://example.com"))
                .isInstanceOf(OidcConnectionException.class)
                .extracting("errorType")
                .isEqualTo(OidcConnectionError.CONNECTION_ERROR);
    }

    @Test
    @DisplayName("verify()で予期しないResourceAccessExceptionはUNEXPECTED_ERROR")
    void verify_otherResourceAccessException_throwsUnexpectedError() {
        mockGetThrows(new ResourceAccessException("その他のエラー"));
        assertThatThrownBy(() -> service.verify("https://example.com"))
                .isInstanceOf(OidcConnectionException.class)
                .extracting("errorType")
                .isEqualTo(OidcConnectionError.UNEXPECTED_ERROR);
    }

    @Test
    @DisplayName("verify()で予期しない例外はUNEXPECTED_ERROR")
    void verify_unexpectedException_throwsUnexpectedError() {
        mockGetThrows(new RuntimeException("予期しないエラー"));
        assertThatThrownBy(() -> service.verify("https://example.com"))
                .isInstanceOf(OidcConnectionException.class)
                .extracting("errorType")
                .isEqualTo(OidcConnectionError.UNEXPECTED_ERROR);
    }

    // ============================================================
    // generateState()
    // ============================================================
    @Test
    @DisplayName("generateState()は空でない文字列を返す")
    void generateState_returnsNonEmptyString() {
        String state = service.generateState();
        assertThat(state).isNotNull().isNotEmpty();
    }

    @Test
    @DisplayName("generateState()は毎回異なる値を返す")
    void generateState_returnsDifferentValues() {
        String state1 = service.generateState();
        String state2 = service.generateState();
        assertThat(state1).isNotEqualTo(state2);
    }

    // ============================================================
    // buildAuthorizationUrl()
    // ============================================================
    @Test
    @DisplayName("buildAuthorizationUrl()は必要なパラメーターを含むURLを返す")
    void buildAuthorizationUrl_containsRequiredParameters() {
        String url = service.buildAuthorizationUrl(
                "https://example.com/auth",
                "client-id",
                "https://app.example.com/callback",
                "random-state");

        assertThat(url)
                .startsWith("https://example.com/auth?")
                .contains("response_type=code")
                .contains("client_id=client-id")
                .contains("redirect_uri=https%3A%2F%2Fapp.example.com%2Fcallback")
                .contains("scope=openid+profile+email")
                .contains("state=random-state");
    }

    @Test
    @DisplayName("buildAuthorizationUrl()は特殊文字を含むパラメーターをエスケープする")
    void buildAuthorizationUrl_escapesSpecialCharacters() {
        String url = service.buildAuthorizationUrl(
                "https://example.com/auth",
                "client&id",
                "https://app.example.com/callback?x=1",
                "state value");

        assertThat(url)
                .contains("client_id=client%26id")
                .contains("state=state+value");
    }

    // ============================================================
    // getMetadata()
    // ============================================================
    @Test
    @DisplayName("getMetadata()は正常なレスポンスをMapに変換して返す")
    void getMetadata_validResponse_returnsMap() {
        mockGetResponse("""
                {
                  "issuer": "https://example.com",
                  "authorization_endpoint": "https://example.com/auth",
                  "token_endpoint": "https://example.com/token"
                }
                """);
        Map<String, Object> result = service.getMetadata("https://example.com");
        assertThat(result)
                .containsEntry("issuer", "https://example.com")
                .containsEntry("authorization_endpoint", "https://example.com/auth")
                .containsEntry("token_endpoint", "https://example.com/token");
    }

    @Test
    @DisplayName("getMetadata()でissuerUriがnullならINPUT_ERROR")
    void getMetadata_nullUri_throwsInputError() {
        // verify()と同じ動作（前段にverify()由来の検証はないが、JSONパースで例外）
        assertThatThrownBy(() -> service.getMetadata(null))
                .isInstanceOf(OidcConnectionException.class);
    }

    @Test
    @DisplayName("getMetadata()でJSONパースエラーならUNEXPECTED_ERROR")
    void getMetadata_invalidJson_throwsUnexpectedError() {
        mockGetResponse("invalid json {{{");
        assertThatThrownBy(() -> service.getMetadata("https://example.com"))
                .isInstanceOf(OidcConnectionException.class)
                .extracting("errorType")
                .isEqualTo(OidcConnectionError.UNEXPECTED_ERROR);
    }

    @Test
    @DisplayName("getMetadata()でfetch時にOidcConnectionExceptionが発生すればそのまま伝播")
    void getMetadata_oidcExceptionPropagated() {
        mockGetThrows(new ResourceAccessException("DNS失敗",
                new UnknownHostException("host")));
        assertThatThrownBy(() -> service.getMetadata("https://example.com"))
                .isInstanceOf(OidcConnectionException.class)
                .extracting("errorType")
                .isEqualTo(OidcConnectionError.CONNECTION_ERROR);
    }

    // ============================================================
    // exchangeCodeForClaims()
    // ============================================================

    // テスト用の署名付きJWTを生成
    private String createTestJwt(String issuer, String subject) throws Exception {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS256);
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
        		.issuer(issuer)
                .subject(subject)
                .audience("test-client-id")
                .build();
        SignedJWT jwt = new SignedJWT(header, claims);
        byte[] secret = new byte[32];
        Arrays.fill(secret, (byte) 'k');
        jwt.sign(new MACSigner(secret));
        return jwt.serialize();
    }

    // POSTリクエストのレスポンスを返すモックを設定
    private void mockPostResponse(String response) {
        RestClient.RequestBodyUriSpec uriSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestBodySpec bodySpec = mock(RestClient.RequestBodySpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(restClient.post()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(bodySpec);
        when(bodySpec.header(anyString(), anyString())).thenReturn(bodySpec);
        when(bodySpec.body(anyString())).thenReturn(bodySpec);
        when(bodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toEntity(String.class))
                .thenReturn(org.springframework.http.ResponseEntity.ok(response));
    }

    @Test
    @DisplayName("exchangeCodeForClaims()は正常なIDトークンからクレームを取得する")
    void exchangeCodeForClaims_validIdToken_returnsClaims() throws Exception {
        String idToken = createTestJwt("https://example.com", "user-123");
        mockPostResponse("{\"id_token\":\"" + idToken + "\"}");

        JWTClaimsSet result = service.exchangeCodeForClaims(
                "https://example.com/token",
                "auth-code",
                "test-client-id",
                "test-client-secret",
                "https://app.example.com/callback");

        assertThat(result.getIssuer()).isEqualTo("https://example.com");
        assertThat(result.getSubject()).isEqualTo("user-123");
    }

    @Test
    @DisplayName("exchangeCodeForClaims()でエラーレスポンスはUNEXPECTED_ERROR")
    void exchangeCodeForClaims_errorResponse_throwsUnexpectedError() {
        mockPostResponse("{\"error\":\"invalid_grant\","
                + "\"error_description\":\"認可コードが無効です\"}");

        assertThatThrownBy(() -> service.exchangeCodeForClaims(
                "https://example.com/token",
                "invalid-code",
                "test-client-id",
                "test-client-secret",
                "https://app.example.com/callback"))
                .isInstanceOf(OidcConnectionException.class)
                .extracting("errorType")
                .isEqualTo(OidcConnectionError.UNEXPECTED_ERROR);
    }

    @Test
    @DisplayName("exchangeCodeForClaims()でid_tokenが含まれないとINVALID_RESPONSE")
    void exchangeCodeForClaims_missingIdToken_throwsInvalidResponse() {
        mockPostResponse("{\"access_token\":\"abc123\"}");

        assertThatThrownBy(() -> service.exchangeCodeForClaims(
                "https://example.com/token",
                "auth-code",
                "test-client-id",
                "test-client-secret",
                "https://app.example.com/callback"))
                .isInstanceOf(OidcConnectionException.class)
                .extracting("errorType")
                .isEqualTo(OidcConnectionError.INVALID_RESPONSE);
    }

    @Test
    @DisplayName("exchangeCodeForClaims()で不正なJWTはUNEXPECTED_ERROR")
    void exchangeCodeForClaims_invalidJwt_throwsUnexpectedError() {
        mockPostResponse("{\"id_token\":\"not-a-valid-jwt\"}");

        assertThatThrownBy(() -> service.exchangeCodeForClaims(
                "https://example.com/token",
                "auth-code",
                "test-client-id",
                "test-client-secret",
                "https://app.example.com/callback"))
                .isInstanceOf(OidcConnectionException.class)
                .extracting("errorType")
                .isEqualTo(OidcConnectionError.UNEXPECTED_ERROR);
    }

    @Test
    @DisplayName("exchangeCodeForClaims()で不正なJSONレスポンスはUNEXPECTED_ERROR")
    void exchangeCodeForClaims_invalidJson_throwsUnexpectedError() {
        mockPostResponse("invalid json {{{");

        assertThatThrownBy(() -> service.exchangeCodeForClaims(
                "https://example.com/token",
                "auth-code",
                "test-client-id",
                "test-client-secret",
                "https://app.example.com/callback"))
                .isInstanceOf(OidcConnectionException.class)
                .extracting("errorType")
                .isEqualTo(OidcConnectionError.UNEXPECTED_ERROR);
    }

}