package io.github.kizulog_community.kizulog.infrastructure.web.header;

import java.util.Collections;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import io.github.kizulog_community.kizulog.domain.systemaccountprofile.model.SystemAccountProfile;
import io.github.kizulog_community.kizulog.domain.systemaccountprofile.service.SystemAccountProfileService;
import io.github.kizulog_community.kizulog.domain.systemoidc.model.ClaimsMappingTarget;
import io.github.kizulog_community.kizulog.domain.systemoidc.model.SystemOidcProvider;
import io.github.kizulog_community.kizulog.domain.systemoidc.port.SystemOidcProviderRepository;
import io.github.kizulog_community.kizulog.domain.systemoidc.service.ClaimsMappingResolver;
import io.github.kizulog_community.kizulog.infrastructure.security.principal.SystemUserPrincipal;
import lombok.RequiredArgsConstructor;

/**
 * システム管理画面ヘッダ用ユーザ表示Resolver
 *
 * @author Jun Kobayashi
 */
@Component
@RequiredArgsConstructor
public class SystemUserDisplayResolver {

    private static final Logger log = LoggerFactory.getLogger(SystemUserDisplayResolver.class);

    /** プロファイルキャッシュService */
    private final SystemAccountProfileService profileService;

    /** OIDCプロバイダリポジトリ */
    private final SystemOidcProviderRepository providerRepository;

    /** クレームマッピング解決Service */
    private final ClaimsMappingResolver claimsMappingResolver;

    /**
     * Principalから表示用Viewを構築する。
     *
     * @param principal 現在の認証Principal
     * @return ユーザ表示View
     */
    public SystemUserDisplayView resolve(SystemUserPrincipal principal) {
        if (principal == null) {
            return emptyView();
        }

        // identity の最新プロファイルを取得
        Map<String, Object> claims = loadClaimsForIdentity(principal.getIdentityId());

        // identity が属する provider を引く
        SystemOidcProvider provider = findProviderForIdentity(principal);
        if (provider == null) {
            log.debug("No provider found for identity: identityId={}, iss={}",
                    principal.getIdentityId(), principal.getIss());
            return emptyView();
        }

        // マッピング適用
        Map<ClaimsMappingTarget, String> resolved =
                claimsMappingResolver.resolve(provider, claims);

        return new SystemUserDisplayView(
                resolved.get(ClaimsMappingTarget.FAMILY_NAME),
                resolved.get(ClaimsMappingTarget.GIVEN_NAME),
                resolved.get(ClaimsMappingTarget.MIDDLE_NAME),
                resolved.get(ClaimsMappingTarget.ORGANIZATION),
                resolved.get(ClaimsMappingTarget.EMAIL));
    }

    /**
     * identity のキャッシュ済みクレームを取得する。
     *
     * @param identityId identity ID
     * @return クレームMap、または取得失敗時の空Map
     */
    private Map<String, Object> loadClaimsForIdentity(String identityId) {
        if (identityId == null) {
            return Collections.emptyMap();
        }
        try {
            return profileService.getProfile(identityId)
                    .map(SystemAccountProfile::getClaims)
                    .orElse(Collections.emptyMap());
        } catch (RuntimeException e) {
            log.warn("Failed to load profile claims for identity: identityId={}, error={}",
                    identityId, e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * identity が属するOIDCプロバイダを特定する。
     *
     * @param principal 現在のPrincipal
     * @return プロバイダ、または該当なしの場合 null
     */
    private SystemOidcProvider findProviderForIdentity(SystemUserPrincipal principal) {
        String iss = principal.getIss();
        if (iss == null) {
            return null;
        }
        try {
            return providerRepository.findAllLatest().stream()
                    .filter(p -> matchesIss(p, iss))
                    .findFirst()
                    .orElse(null);
        } catch (RuntimeException e) {
            log.warn("Failed to find provider for identity: identityId={}, iss={}, error={}",
                    principal.getIdentityId(), iss, e.getMessage());
            return null;
        }
    }

    /**
     * プロバイダのURIがissと一致するか判定する。
     *
     * @param provider プロバイダ
     * @param iss 認証元のiss
     * @return 一致する場合true
     */
    private boolean matchesIss(SystemOidcProvider provider, String iss) {
        if (provider == null || provider.getUri() == null || iss == null) {
            return false;
        }
        return trimSlash(provider.getUri()).equals(trimSlash(iss));
    }

    private static String trimSlash(String s) {
        if (s == null) {
            return null;
        }
        if (s.endsWith("/")) {
            return s.substring(0, s.length() - 1);
        }
        return s;
    }

    private SystemUserDisplayView emptyView() {
        return new SystemUserDisplayView(null, null, null, null, null);
    }

}
