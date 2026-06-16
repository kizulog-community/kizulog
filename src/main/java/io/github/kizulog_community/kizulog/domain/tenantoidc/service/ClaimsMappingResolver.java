package io.github.kizulog_community.kizulog.domain.tenantoidc.service;

import java.util.EnumMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import io.github.kizulog_community.kizulog.domain.tenantoidc.model.ClaimsMappingTarget;
import io.github.kizulog_community.kizulog.domain.tenantoidc.model.TenantOidcProvider;

/**
 * クレームマッピングService（テナント用）
 *
 * @author Jun Kobayashi
 */
@Service
public class ClaimsMappingResolver {

    /**
     * プロバイダの claimsMapping に従って、属性すべての値を解決する。
     *
     * @param provider プロバイダ（マッピング設定を含む）
     * @param claims OIDCクレームMap（identity単位のキャッシュ等）
     * @return 各属性の解決値（EnumMap、すべての属性をキーに持つ）
     */
    public Map<ClaimsMappingTarget, String> resolve(
            TenantOidcProvider provider,
            Map<String, Object> claims) {
        EnumMap<ClaimsMappingTarget, String> result = new EnumMap<>(ClaimsMappingTarget.class);
        for (ClaimsMappingTarget target : ClaimsMappingTarget.values()) {
            result.put(target, resolveOne(provider, claims, target));
        }
        return result;
    }

    /**
     * 単一属性の値を解決する。
     *
     * @param provider プロバイダ（nullの場合はnullを返す）
     * @param claims クレームMap（nullの場合はnullを返す）
     * @param target 解決対象属性
     * @return 解決された文字列値、または取得できなかった場合 null
     */
    public String resolveOne(
            TenantOidcProvider provider,
            Map<String, Object> claims,
            ClaimsMappingTarget target) {
        if (provider == null || claims == null || target == null) {
            return null;
        }
        String claimKey = provider.getClaimKey(target);
        if (claimKey == null) {
            return null;
        }
        Object value = claims.get(claimKey);
        if (value == null) {
            return null;
        }
        String stringValue = value.toString();
        if (stringValue.isBlank()) {
            return null;
        }
        return stringValue;
    }

}
