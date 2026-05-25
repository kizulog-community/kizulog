package io.github.kizulog_community.kizulog.infrastructure.security.matcher;

import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;

/**
 * システムホスト判定マッチャ
 *
 * @author Jun Kobayashi
 */
@Component
public class SystemHostMatcher implements RequestMatcher {

    /** システムホスト名（設定値、小文字化して保持） */
    private final String systemHost;

    /**
     * コンストラクタ
     *
     * @param systemHost システムホスト名（kizulog.system.host）
     */
    public SystemHostMatcher(@Value("${kizulog.system.host}") String systemHost) {
        this.systemHost = systemHost == null
                ? null
                : systemHost.toLowerCase(Locale.ROOT);
    }

    /**
     * リクエストがシステムホスト宛かを判定する。
     *
     * @param request HTTPリクエスト
     * @return システムホスト宛なら true
     */
    @Override
    public boolean matches(HttpServletRequest request) {
        return isSystemHost(resolveHost(request));
    }

    /**
     * 指定ホスト名がシステムホストと一致するかを判定する。
     *
     * @param host 判定対象ホスト名（小文字化済みを想定）
     * @return 一致すれば true
     */
    public boolean isSystemHost(String host) {
        if (host == null || systemHost == null) {
            return false;
        }
        return systemHost.equals(host);
    }

    /**
     * リクエストからホスト名を解決する。
     *
     * @param request HTTPリクエスト
     * @return ホスト名（小文字化）、取得できなければ null
     */
    public String resolveHost(HttpServletRequest request) {
        String serverName = request.getServerName();
        if (serverName == null || serverName.isEmpty()) {
            return null;
        }
        return serverName.toLowerCase(Locale.ROOT);
    }

    /**
     * 設定されているシステムホスト名を返す。
     *
     * @return システムホスト名（小文字化済み）
     */
    public String getSystemHost() {
        return systemHost;
    }

}
