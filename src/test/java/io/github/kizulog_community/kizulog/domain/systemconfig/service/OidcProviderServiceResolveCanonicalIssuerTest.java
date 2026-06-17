package io.github.kizulog_community.kizulog.domain.systemconfig.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.kizulog_community.kizulog.domain.systemconfig.exception.OidcConnectionError;
import io.github.kizulog_community.kizulog.domain.systemconfig.exception.OidcConnectionException;

/**
 * OidcProviderService#resolveCanonicalIssuer の単体テスト
 *
 * @author Jun Kobayashi
 */
class OidcProviderServiceResolveCanonicalIssuerTest {

    private OidcProviderService service;

    @BeforeEach
    void setUp() {
        // RestClient は getMetadata をスタブするため実際には使用されない
        service = spy(new OidcProviderService(mock(RestClient.class), new ObjectMapper()));
    }

    @Test
    @DisplayName("resolveCanonicalIssuer: メタデータの issuer をそのまま返す（末尾スラッシュ入力でも正準値）")
    void resolveCanonicalIssuer_returnsMetadataIssuer() {
        String input = "https://auth.example/realms/main/";
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("issuer", "https://auth.example/realms/main");
        doReturn(metadata).when(service).getMetadata(input);

        String result = service.resolveCanonicalIssuer(input);

        assertThat(result).isEqualTo("https://auth.example/realms/main");
    }

    @Test
    @DisplayName("resolveCanonicalIssuer: issuer の前後空白はトリムされる")
    void resolveCanonicalIssuer_trimsIssuer() {
        String input = "https://auth.example/realms/main";
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("issuer", "  https://auth.example/realms/main  ");
        doReturn(metadata).when(service).getMetadata(input);

        assertThat(service.resolveCanonicalIssuer(input))
                .isEqualTo("https://auth.example/realms/main");
    }

    @Test
    @DisplayName("resolveCanonicalIssuer: 入力が空白の場合 INPUT_ERROR")
    void resolveCanonicalIssuer_throwsInputError_whenBlank() {
        assertThatThrownBy(() -> service.resolveCanonicalIssuer("   "))
                .isInstanceOf(OidcConnectionException.class)
                .extracting(e -> ((OidcConnectionException) e).getErrorType())
                .isEqualTo(OidcConnectionError.INPUT_ERROR);
    }

    @Test
    @DisplayName("resolveCanonicalIssuer: メタデータに issuer が無い場合 INVALID_RESPONSE")
    void resolveCanonicalIssuer_throwsInvalidResponse_whenIssuerMissing() {
        String input = "https://auth.example/realms/main";
        doReturn(new LinkedHashMap<String, Object>()).when(service).getMetadata(input);

        assertThatThrownBy(() -> service.resolveCanonicalIssuer(input))
                .isInstanceOf(OidcConnectionException.class)
                .extracting(e -> ((OidcConnectionException) e).getErrorType())
                .isEqualTo(OidcConnectionError.INVALID_RESPONSE);
    }

    @Test
    @DisplayName("resolveCanonicalIssuer: issuer が文字列でない場合 INVALID_RESPONSE")
    void resolveCanonicalIssuer_throwsInvalidResponse_whenIssuerNotString() {
        String input = "https://auth.example/realms/main";
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("issuer", 12345);
        doReturn(metadata).when(service).getMetadata(input);

        assertThatThrownBy(() -> service.resolveCanonicalIssuer(input))
                .isInstanceOf(OidcConnectionException.class)
                .extracting(e -> ((OidcConnectionException) e).getErrorType())
                .isEqualTo(OidcConnectionError.INVALID_RESPONSE);
    }

    @Test
    @DisplayName("resolveCanonicalIssuer: getMetadata の接続例外はそのまま伝播する")
    void resolveCanonicalIssuer_propagatesConnectionError() {
        String input = "https://unreachable.example/realms/main";
        doThrow(new OidcConnectionException(OidcConnectionError.CONNECTION_ERROR))
                .when(service).getMetadata(input);

        assertThatThrownBy(() -> service.resolveCanonicalIssuer(input))
                .isInstanceOf(OidcConnectionException.class)
                .extracting(e -> ((OidcConnectionException) e).getErrorType())
                .isEqualTo(OidcConnectionError.CONNECTION_ERROR);
    }

}
