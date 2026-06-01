package io.github.kizulog_community.kizulog.infrastructure.web.header;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;

import io.github.kizulog_community.kizulog.infrastructure.security.principal.SystemUserPrincipal;

/**
 * SystemUserDisplayAdviceの単体テスト
 *
 * @author Jun Kobayashi
 */
class SystemUserDisplayAdviceTest {

    private SystemUserDisplayResolver resolver;
    private SystemUserDisplayAdvice sut;

    @BeforeEach
    void setUp() {
        resolver = mock(SystemUserDisplayResolver.class);
        sut = new SystemUserDisplayAdvice(resolver);
    }

    @Test
    @DisplayName("userDisplay: principalがnullの場合はnullを返す（Resolverは呼ばれない）")
    void userDisplay_returnsNull_whenPrincipalIsNull() {
        SystemUserDisplayView result = sut.userDisplay(null);

        assertThat(result).isNull();
        verify(resolver, never()).resolve(null);
    }

    @Test
    @DisplayName("userDisplay: principalがある場合はResolver.resolve()結果を返す")
    void userDisplay_returnsResolverResult_whenPrincipalPresent() {
        SystemUserPrincipal principal = buildPrincipal();
        // T.0: 5フィールド構成 (familyName, givenName, middleName, organization, email)
        SystemUserDisplayView view = new SystemUserDisplayView(
                "山田", "太郎", null, "開発部", "taro@example.com");
        when(resolver.resolve(principal)).thenReturn(view);

        SystemUserDisplayView result = sut.userDisplay(principal);

        assertThat(result).isSameAs(view);
        verify(resolver).resolve(principal);
    }

    @Test
    @DisplayName("userDisplay: Resolver例外時はnull返却+ログ（fail-open）")
    void userDisplay_returnsNull_whenResolverThrows() {
        SystemUserPrincipal principal = buildPrincipal();
        when(resolver.resolve(principal))
                .thenThrow(new RuntimeException("DB connection refused"));

        SystemUserDisplayView result = sut.userDisplay(principal);

        // fail-open: 例外を握りつぶしてnullを返す
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("userDisplay: Resolverがnullを返した場合はそのままnull返却")
    void userDisplay_returnsNull_whenResolverReturnsNull() {
        SystemUserPrincipal principal = buildPrincipal();
        when(resolver.resolve(principal)).thenReturn(null);

        SystemUserDisplayView result = sut.userDisplay(principal);

        assertThat(result).isNull();
    }

    /**
     * テスト用のPrincipalを生成する。
     */
    private SystemUserPrincipal buildPrincipal() {
        OidcIdToken idToken = OidcIdToken
                .withTokenValue("dummy-token")
                .issuer("https://example.com")
                .subject("sub-1")
                .audience(List.of("aud-1"))
                .build();
        return SystemUserPrincipal.ofSystemAdmin(
                "acc-1", "id-1", "https://example.com", "aud-1", "sub-1", idToken);
    }

}
