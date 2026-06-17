package io.github.kizulog_community.kizulog.infrastructure.security.oidc;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import io.github.kizulog_community.kizulog.domain.tenantoidc.model.ClaimsMappingTarget;
import io.github.kizulog_community.kizulog.domain.tenantoidc.model.TenantOidcProvider;
import io.github.kizulog_community.kizulog.domain.tenantoidc.port.TenantOidcProviderRepository;
import io.github.kizulog_community.kizulog.domain.tenantoidc.service.ClaimsMappingResolver;
import lombok.RequiredArgsConstructor;

/**
 * テナント利用者のプロファイル保存値解決Resolver
 *
 * <p>ログイン時に、テナントOIDCプロバイダの {@code claimsMapping} に従って生クレームを
 * KizuLog のプロフィール属性（氏・名・ミドル・所属・email の最大5項目）へ解決し、
 * 「ターゲットキー（ClaimsMappingTarget#getKey()）→ 値」のMapを返す。</p>
 *
 * <p>このMapがそのまま tenant_account_profiles.claims（JSONB）に保存される。
 * プロバイダは (iss, aud) で引き、テナントIDで絞り込む。プロバイダ未特定・クレーム欠落時は
 * 空Mapを返す（fail-open）。</p>
 *
 * @author Jun Kobayashi
 */
@Component
@RequiredArgsConstructor
public class TenantProfileClaimsResolver {

    /** ロガー */
    private static final Logger log =
            LoggerFactory.getLogger(TenantProfileClaimsResolver.class);

    /** OIDCプロバイダリポジトリ */
    private final TenantOidcProviderRepository providerRepository;

    /** クレームマッピング解決Service（テナント用） */
    private final ClaimsMappingResolver claimsMappingResolver;

    /**
     * 生クレームを保存用のターゲットキー付きMapへ解決する。
     *
     * @param tenantId テナントID
     * @param iss 認証元の issuer
     * @param aud 認証元の audience（client_id）
     * @param rawClaims OIDCプロバイダから取得した生クレーム
     * @return ターゲットキー→値のMap（解決できた非空白項目のみ）。
     *         プロバイダ未特定・入力不正時は空Map
     */
    public Map<String, Object> resolveForStorage(
            String tenantId, String iss, String aud, Map<String, Object> rawClaims) {
        if (tenantId == null || iss == null || aud == null || rawClaims == null) {
            return Collections.emptyMap();
        }
        TenantOidcProvider provider = findProvider(tenantId, iss, aud);
        if (provider == null) {
            log.debug("テナントプロファイル解決: プロバイダ未特定のため空: tenantId={}, iss={}, aud={}",
                    tenantId, iss, aud);
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
     * (iss, aud) でプロバイダを引き、テナントIDで絞り込む。
     *
     * @param tenantId テナントID
     * @param iss 認証元の issuer
     * @param aud 認証元の audience
     * @return 一致したプロバイダ、または該当なしの場合 null
     */
    private TenantOidcProvider findProvider(String tenantId, String iss, String aud) {
        try {
            return providerRepository.findAllLatestByIssAndAud(iss, aud).stream()
                    .filter(p -> tenantId.equals(p.getTenantId()))
                    .findFirst()
                    .orElse(null);
        } catch (RuntimeException e) {
            log.warn("テナントプロバイダ特定に失敗: tenantId={}, iss={}, aud={}, error={}",
                    tenantId, iss, aud, e.getMessage());
            return null;
        }
    }

}
