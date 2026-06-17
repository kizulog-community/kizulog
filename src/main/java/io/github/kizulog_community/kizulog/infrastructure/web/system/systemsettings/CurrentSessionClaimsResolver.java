package io.github.kizulog_community.kizulog.infrastructure.web.system.systemsettings;

import java.util.Collections;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import io.github.kizulog_community.kizulog.infrastructure.security.principal.SystemUserPrincipal;

/**
 * 現在のログインセッションのOIDCクレーム取得Resolver
 *
 * @author Jun Kobayashi
 */
@Component
public class CurrentSessionClaimsResolver {

    private static final Logger log = LoggerFactory.getLogger(CurrentSessionClaimsResolver.class);

    /**
     * 現在のセッションの ID Token クレームMapを返す。
     *
     * @param principal 現在のPrincipal（未認証可）
     * @return クレームMap（ID Tokenの生クレーム）、取得失敗時は空Map
     */
    public Map<String, Object> resolveClaims(SystemUserPrincipal principal) {
        if (principal == null) {
            return Collections.emptyMap();
        }
        try {
            Map<String, Object> claims = principal.getClaims();
            return (claims != null) ? claims : Collections.emptyMap();
        } catch (RuntimeException e) {
            log.warn("Failed to load current session claims: identityId={}, error={}",
                    principal.getIdentityId(), e.getMessage());
            return Collections.emptyMap();
        }
    }

}
