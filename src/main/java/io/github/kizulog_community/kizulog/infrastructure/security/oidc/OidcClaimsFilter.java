package io.github.kizulog_community.kizulog.infrastructure.security.oidc;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import io.github.kizulog_community.kizulog.config.OidcProfileClaimsProperties;
import lombok.RequiredArgsConstructor;

/**
 * OIDCクレームのホワイトリストフィルタ
 *
 * @author Jun Kobayashi
 */
@Component
@RequiredArgsConstructor
public class OidcClaimsFilter {

    private final OidcProfileClaimsProperties properties;

    /**
     * ホワイトリストに基づいてクレームをフィルタする。
     *
     * @param rawClaims 元のクレームMap
     * @return ホワイトリスト適用後のクレームMap
     */
    public Map<String, Object> filter(Map<String, Object> rawClaims) {
        if (rawClaims == null || rawClaims.isEmpty()) {
            return Collections.emptyMap();
        }
        List<String> whitelist = properties.getCachedClaims();
        if (whitelist == null || whitelist.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<String> whitelistSet = Set.copyOf(whitelist);

        Map<String, Object> result = new LinkedHashMap<>();
        for (String key : whitelist) {
            if (!whitelistSet.contains(key)) {
                continue;
            }
            Object value = rawClaims.get(key);
            if (value == null) {
                continue;
            }
            result.put(key, value);
        }
        return result;
    }

}
