package io.github.kizulog_community.kizulog.infrastructure.web.system.systemsettings;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import io.github.kizulog_community.kizulog.domain.systemaccountprofile.model.SystemAccountProfile;
import io.github.kizulog_community.kizulog.domain.systemaccountprofile.service.SystemAccountProfileService;
import io.github.kizulog_community.kizulog.infrastructure.security.principal.SystemUserPrincipal;
import lombok.RequiredArgsConstructor;

/**
 * 現在のログインセッションのOIDCクレーム取得Resolver
 *
 * @author Jun Kobayashi
 */
@Component
@RequiredArgsConstructor
public class CurrentSessionClaimsResolver {

    private static final Logger log = LoggerFactory.getLogger(CurrentSessionClaimsResolver.class);

    private final SystemAccountProfileService profileService;

    /**
     * 現在のセッションで取得済みのクレームMapを返す。
     *
     * @param principal 現在のPrincipal（未認証可）
     * @return クレームMap（identity経由でキャッシュ済みの値）、取得失敗時は空Map
     */
    public Map<String, Object> resolveClaims(SystemUserPrincipal principal) {
        if (principal == null || principal.getIdentityId() == null) {
            return Collections.emptyMap();
        }
        try {
            Optional<SystemAccountProfile> profileOpt =
                    profileService.getProfile(principal.getIdentityId());
            return profileOpt
                    .map(SystemAccountProfile::getClaims)
                    .orElse(Collections.emptyMap());
        } catch (RuntimeException e) {
            log.warn("Failed to load current session claims: identityId={}, error={}",
                    principal.getIdentityId(), e.getMessage());
            return Collections.emptyMap();
        }
    }

}
