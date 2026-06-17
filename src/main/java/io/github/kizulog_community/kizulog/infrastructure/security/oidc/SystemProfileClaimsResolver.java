package io.github.kizulog_community.kizulog.infrastructure.security.oidc;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import io.github.kizulog_community.kizulog.domain.systemoidc.model.ClaimsMappingTarget;
import io.github.kizulog_community.kizulog.domain.systemoidc.model.SystemOidcProvider;
import io.github.kizulog_community.kizulog.domain.systemoidc.port.SystemOidcProviderRepository;
import io.github.kizulog_community.kizulog.domain.systemoidc.service.ClaimsMappingResolver;
import lombok.RequiredArgsConstructor;

/**
 * システム管理アカウントのプロファイル保存値解決Resolver
 *
 * <p>ログイン時に、OIDCプロバイダの {@code claimsMapping} に従って生クレームを
 * KizuLog のプロフィール属性（氏・名・ミドル・所属・email の最大5項目）へ解決し、
 * 「ターゲットキー（ClaimsMappingTarget#getKey()）→ 値」のMapを返す。</p>
 *
 * <p>このMapがそのまま system_account_profiles.claims（JSONB）に保存される。
 * これにより生のOIDCクレームは永続化されず、表示側はプロバイダ参照なしに
 * 保存値を直読みできる。プロバイダ未特定・クレーム欠落時は空Mapを返す（fail-open）。</p>
 *
 * @author Jun Kobayashi
 */
@Component
@RequiredArgsConstructor
public class SystemProfileClaimsResolver {

    /** ロガー */
    private static final Logger log =
            LoggerFactory.getLogger(SystemProfileClaimsResolver.class);

    /** OIDCプロバイダリポジトリ */
    private final SystemOidcProviderRepository providerRepository;

    /** クレームマッピング解決Service */
    private final ClaimsMappingResolver claimsMappingResolver;

    /**
     * 生クレームを保存用のターゲットキー付きMapへ解決する。
     *
     * @param iss 認証元の issuer
     * @param rawClaims OIDCプロバイダから取得した生クレーム
     * @return ターゲットキー→値のMap（解決できた非空白項目のみ）。
     *         プロバイダ未特定・入力不正時は空Map
     */
    public Map<String, Object> resolveForStorage(String iss, Map<String, Object> rawClaims) {
        if (iss == null || rawClaims == null) {
            return Collections.emptyMap();
        }
        SystemOidcProvider provider = findProviderByIss(iss);
        if (provider == null) {
            log.debug("プロファイル解決: プロバイダ未特定のため空: iss={}", iss);
            return Collections.emptyMap();
        }
        Map<ClaimsMappingTarget, String> resolved =
                claimsMappingResolver.resolve(provider, rawClaims);
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<ClaimsMappingTarget, String> entry : resolved.entrySet()) {
            String value = entry.getValue();
            if (value != null && !value.isBlank()) {
                result.put(entry.getKey().getKey(), value);
            }
        }
        return result;
    }

    /**
     * iss に一致するOIDCプロバイダを特定する（末尾スラッシュ揺れを吸収）。
     *
     * @param iss 認証元の issuer
     * @return 一致したプロバイダ、または該当なしの場合 null
     */
    private SystemOidcProvider findProviderByIss(String iss) {
        try {
            return providerRepository.findAllLatest().stream()
                    .filter(p -> matchesIss(p, iss))
                    .findFirst()
                    .orElse(null);
        } catch (RuntimeException e) {
            log.warn("プロバイダ特定に失敗: iss={}, error={}", iss, e.getMessage());
            return null;
        }
    }

    /**
     * プロバイダのURIがissと一致するか判定する。
     *
     * @param provider プロバイダ
     * @param iss 認証元の iss
     * @return 一致する場合 true
     */
    private boolean matchesIss(SystemOidcProvider provider, String iss) {
        if (provider == null || provider.getUri() == null || iss == null) {
            return false;
        }
        return trimSlash(provider.getUri()).equals(trimSlash(iss));
    }

    /**
     * 末尾スラッシュを除去する。
     *
     * @param s 入力文字列
     * @return 末尾スラッシュ除去後の文字列
     */
    private static String trimSlash(String s) {
        if (s == null) {
            return null;
        }
        if (s.endsWith("/")) {
            return s.substring(0, s.length() - 1);
        }
        return s;
    }

}
