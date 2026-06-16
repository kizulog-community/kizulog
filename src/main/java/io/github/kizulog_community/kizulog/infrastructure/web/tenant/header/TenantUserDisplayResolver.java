package io.github.kizulog_community.kizulog.infrastructure.web.tenant.header;

import java.util.Collections;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import io.github.kizulog_community.kizulog.domain.tenantaccountprofile.model.TenantAccountProfile;
import io.github.kizulog_community.kizulog.domain.tenantaccountprofile.service.TenantAccountProfileService;
import io.github.kizulog_community.kizulog.domain.tenantoidc.model.ClaimsMappingTarget;
import io.github.kizulog_community.kizulog.domain.tenantoidc.model.TenantOidcProvider;
import io.github.kizulog_community.kizulog.domain.tenantoidc.port.TenantOidcProviderRepository;
import io.github.kizulog_community.kizulog.domain.tenantoidc.service.ClaimsMappingResolver;
import io.github.kizulog_community.kizulog.infrastructure.security.principal.TenantUserPrincipal;
import lombok.RequiredArgsConstructor;

/**
 * テナント画面ヘッダ用ユーザ表示Resolver
 *
 * @author Jun Kobayashi
 */
@Component
@RequiredArgsConstructor
public class TenantUserDisplayResolver {

    private static final Logger log = LoggerFactory.getLogger(TenantUserDisplayResolver.class);

    /** プロファイルキャッシュService */
    private final TenantAccountProfileService profileService;

    /** OIDCプロバイダリポジトリ */
    private final TenantOidcProviderRepository providerRepository;

    /** クレームマッピング解決Service（テナント用） */
    private final ClaimsMappingResolver claimsMappingResolver;

    /**
     * Principalから表示用Viewを構築する。
     *
     * @param principal 現在の認証Principal
     * @return ユーザ表示View
     */
    public TenantUserDisplayView resolve(TenantUserPrincipal principal) {
        if (principal == null) {
            return emptyView();
        }

        // identity の最新プロファイルを取得
        Map<String, Object> claims = loadClaimsForIdentity(principal.getIdentityId());

        // identity が属する provider を引く
        TenantOidcProvider provider = findProviderForPrincipal(principal);
        if (provider == null) {
            log.debug("No provider found for tenant identity: identityId={}, iss={}, aud={}",
                    principal.getIdentityId(), principal.getIss(), principal.getAud());
            return emptyView();
        }

        // マッピング適用
        Map<ClaimsMappingTarget, String> resolved =
                claimsMappingResolver.resolve(provider, claims);

        return new TenantUserDisplayView(
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
                    .map(TenantAccountProfile::getClaims)
                    .orElse(Collections.emptyMap());
        } catch (RuntimeException e) {
            log.warn("Failed to load tenant profile claims for identity: identityId={}, error={}",
                    identityId, e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * Principal が属するOIDCプロバイダを特定する。
     *
     * @param principal 現在のPrincipal
     * @return プロバイダ、または該当なしの場合 null
     */
    private TenantOidcProvider findProviderForPrincipal(TenantUserPrincipal principal) {
        String iss = principal.getIss();
        String aud = principal.getAud();
        String tenantId = principal.getTenantId();
        if (iss == null || aud == null || tenantId == null) {
            return null;
        }
        try {
            return providerRepository.findAllLatestByIssAndAud(iss, aud).stream()
                    .filter(p -> tenantId.equals(p.getTenantId()))
                    .findFirst()
                    .orElse(null);
        } catch (RuntimeException e) {
            log.warn("Failed to find provider for tenant identity: identityId={}, "
                            + "iss={}, aud={}, error={}",
                    principal.getIdentityId(), iss, aud, e.getMessage());
            return null;
        }
    }

    private TenantUserDisplayView emptyView() {
        return new TenantUserDisplayView(null, null, null, null, null);
    }

}
