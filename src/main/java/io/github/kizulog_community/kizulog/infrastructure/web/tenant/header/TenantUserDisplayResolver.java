package io.github.kizulog_community.kizulog.infrastructure.web.tenant.header;

import java.util.Collections;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import io.github.kizulog_community.kizulog.domain.tenantaccountprofile.model.TenantAccountProfile;
import io.github.kizulog_community.kizulog.domain.tenantaccountprofile.service.TenantAccountProfileService;
import io.github.kizulog_community.kizulog.domain.tenantoidc.model.ClaimsMappingTarget;
import io.github.kizulog_community.kizulog.infrastructure.security.principal.TenantUserPrincipal;
import lombok.RequiredArgsConstructor;

/**
 * テナント画面ヘッダ用ユーザ表示Resolver
 *
 * <p>プロファイルにはログイン時にマッピング解決済みの値（ターゲットキー→値）が
 * 保存されているため、ここではプロバイダ参照やマッピング適用を行わず、保存値を
 * 直接読み出して表示Viewを構築する。</p>
 *
 * @author Jun Kobayashi
 */
@Component
@RequiredArgsConstructor
public class TenantUserDisplayResolver {

    private static final Logger log = LoggerFactory.getLogger(TenantUserDisplayResolver.class);

    /** プロファイルキャッシュService */
    private final TenantAccountProfileService profileService;

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

        Map<String, Object> claims = loadClaimsForIdentity(principal.getIdentityId());

        return new TenantUserDisplayView(
                value(claims, ClaimsMappingTarget.FAMILY_NAME),
                value(claims, ClaimsMappingTarget.GIVEN_NAME),
                value(claims, ClaimsMappingTarget.MIDDLE_NAME),
                value(claims, ClaimsMappingTarget.ORGANIZATION),
                value(claims, ClaimsMappingTarget.EMAIL));
    }

    /**
     * identity の解決済みプロファイル（ターゲットキー→値）を取得する。
     *
     * @param identityId identity ID
     * @return 保存値Map、または取得失敗時の空Map
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
     * 解決済みプロファイルから指定ターゲットの値を取り出す。
     *
     * @param claims 解決済みプロファイル（ターゲットキー→値）
     * @param target 取得対象
     * @return 値（非空白）、または該当なしの場合 null
     */
    private static String value(Map<String, Object> claims, ClaimsMappingTarget target) {
        if (claims == null) {
            return null;
        }
        Object v = claims.get(target.getKey());
        if (v == null) {
            return null;
        }
        String s = v.toString();
        return s.isBlank() ? null : s;
    }

    private TenantUserDisplayView emptyView() {
        return new TenantUserDisplayView(null, null, null, null, null);
    }

}
