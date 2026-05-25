package io.github.kizulog_community.kizulog.infrastructure.security.matcher;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * SystemHostMatcherの単体テスト
 *
 * @author Jun Kobayashi
 */
class SystemHostMatcherTest {

    private static final String SYSTEM_HOST = "kizulog.dev.internal";

    private MockHttpServletRequest requestWithHost(String host) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServerName(host);
        return request;
    }

    @Test
    @DisplayName("matches: システムホスト宛のリクエストはtrue")
    void matches_systemHost_returnsTrue() {
        SystemHostMatcher matcher = new SystemHostMatcher(SYSTEM_HOST);

        assertThat(matcher.matches(requestWithHost(SYSTEM_HOST))).isTrue();
    }

    @Test
    @DisplayName("matches: テナントホスト宛のリクエストはfalse")
    void matches_tenantHost_returnsFalse() {
        SystemHostMatcher matcher = new SystemHostMatcher(SYSTEM_HOST);

        assertThat(matcher.matches(requestWithHost("acme.example.com"))).isFalse();
    }

    @Test
    @DisplayName("matches: 大文字小文字を区別せず判定する")
    void matches_caseInsensitive() {
        SystemHostMatcher matcher = new SystemHostMatcher(SYSTEM_HOST);

        assertThat(matcher.matches(requestWithHost("KizuLog.Dev.Internal"))).isTrue();
    }

    @Test
    @DisplayName("matches: 設定値が大文字でも小文字化して判定する")
    void matches_systemHostUpperCase_normalized() {
        SystemHostMatcher matcher = new SystemHostMatcher("KIZULOG.DEV.INTERNAL");

        assertThat(matcher.matches(requestWithHost("kizulog.dev.internal"))).isTrue();
    }

    @Test
    @DisplayName("isSystemHost: 一致すればtrue")
    void isSystemHost_matching_returnsTrue() {
        SystemHostMatcher matcher = new SystemHostMatcher(SYSTEM_HOST);

        assertThat(matcher.isSystemHost(SYSTEM_HOST)).isTrue();
    }

    @Test
    @DisplayName("isSystemHost: 不一致ならfalse")
    void isSystemHost_nonMatching_returnsFalse() {
        SystemHostMatcher matcher = new SystemHostMatcher(SYSTEM_HOST);

        assertThat(matcher.isSystemHost("other.example.com")).isFalse();
    }

    @Test
    @DisplayName("isSystemHost: hostがnullならfalse")
    void isSystemHost_nullHost_returnsFalse() {
        SystemHostMatcher matcher = new SystemHostMatcher(SYSTEM_HOST);

        assertThat(matcher.isSystemHost(null)).isFalse();
    }

    @Test
    @DisplayName("isSystemHost: 設定値がnullならfalse")
    void isSystemHost_nullSystemHost_returnsFalse() {
        SystemHostMatcher matcher = new SystemHostMatcher(null);

        assertThat(matcher.isSystemHost(SYSTEM_HOST)).isFalse();
    }

    @Test
    @DisplayName("resolveHost: serverNameを小文字化して返す")
    void resolveHost_lowercases() {
        SystemHostMatcher matcher = new SystemHostMatcher(SYSTEM_HOST);

        assertThat(matcher.resolveHost(requestWithHost("ACME.Example.COM")))
                .isEqualTo("acme.example.com");
    }

    @Test
    @DisplayName("resolveHost: serverNameが空文字ならnull")
    void resolveHost_emptyServerName_returnsNull() {
        SystemHostMatcher matcher = new SystemHostMatcher(SYSTEM_HOST);

        assertThat(matcher.resolveHost(requestWithHost(""))).isNull();
    }

    @Test
    @DisplayName("getSystemHost: 設定値を小文字化して返す")
    void getSystemHost_returnsNormalizedValue() {
        SystemHostMatcher matcher = new SystemHostMatcher("KIZULOG.DEV.INTERNAL");

        assertThat(matcher.getSystemHost()).isEqualTo("kizulog.dev.internal");
    }

}
