package io.github.kizulog_community.kizulog.domain.systemoidc.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import io.github.kizulog_community.kizulog.domain.systemaccount.model.SystemAccountIdentity;
import io.github.kizulog_community.kizulog.domain.systemaccount.port.SystemAccountIdentityRepository;
import io.github.kizulog_community.kizulog.domain.systemaccountprofile.model.SystemAccountProfile;
import io.github.kizulog_community.kizulog.domain.systemaccountprofile.service.SystemAccountProfileService;
import io.github.kizulog_community.kizulog.domain.systemoidc.model.ClaimsMappingTarget;
import lombok.RequiredArgsConstructor;

/**
 * Identity 単位のクレーム表示ビュー提供サービス
 *
 * @author Jun Kobayashi
 */
@Service
@RequiredArgsConstructor
public class IdentityClaimsViewService {

    /** Identityリポジトリ */
    private final SystemAccountIdentityRepository identityRepository;

    /** Profileサービス（解決済みプロファイル取得用） */
    private final SystemAccountProfileService profileService;

    /** メッセージソース（ラベル国際化用） */
    private final MessageSource messageSource;

    /**
     * 指定された identityId のクレーム表示マップを取得する。
     *
     * @param identityId identityId
     * @return クレーム表示マップ（5項目最大、空Mapあり）
     */
    public Map<String, String> resolveClaimsView(String identityId) {
        if (identityId == null) {
            return Map.of();
        }
        try {
            Optional<SystemAccountIdentity> identityOpt =
                    identityRepository.findLatestByIdentityId(identityId);
            if (identityOpt.isEmpty()) {
                return Map.of();
            }
            return resolveClaimsViewForIdentity(identityOpt.get());
        } catch (Exception e) {
            return Map.of();
        }
    }

    /**
     * 指定された accountId の代表クレーム表示マップを取得する。
     *
     * @param accountId accountId
     * @return クレーム表示マップ（5項目最大、空Mapあり）
     */
    public Map<String, String> resolveClaimsViewForAccount(String accountId) {
        if (accountId == null) {
            return Map.of();
        }
        try {
            List<SystemAccountIdentity> identities =
                    identityRepository.findLatestByAccountId(accountId);
            if (identities == null || identities.isEmpty()) {
                return Map.of();
            }
            return resolveClaimsViewForIdentity(identities.get(0));
        } catch (Exception e) {
            return Map.of();
        }
    }

    /**
     * 指定された identityId の表示用クレーム情報リストを取得する。
     *
     * @param identityId identityId
     * @param locale ラベル国際化用ロケール
     * @return 表示用クレーム情報リスト（空List あり）
     */
    public List<Map<String, String>> resolveClaimsDisplay(String identityId, Locale locale) {
        Map<String, String> claimsView = resolveClaimsView(identityId);
        return buildDisplayList(claimsView, locale, "system.oidcProviders.claimsMapping.target.");
    }

    /**
     * 指定された accountId の代表表示用クレーム情報リストを取得する。
     *
     * @param accountId accountId
     * @param locale ラベル国際化用ロケール
     * @return 表示用クレーム情報リスト（空List あり）
     */
    public List<Map<String, String>> resolveClaimsDisplayForAccount(String accountId, Locale locale) {
        Map<String, String> claimsView = resolveClaimsViewForAccount(accountId);
        return buildDisplayList(claimsView, locale, "system.oidcProviders.claimsMapping.target.");
    }

    /**
     * クレームMap → 表示用Listへ変換する。
     *
     * @param claimsView クレームMap（key: targetKey, value: 値）
     * @param locale ラベル用ロケール
     * @param labelKeyPrefix MessageSourceのキープレフィックス
     * @return 表示用List（順序: ClaimsMappingTarget enum定義順）
     */
    private List<Map<String, String>> buildDisplayList(
            Map<String, String> claimsView, Locale locale, String labelKeyPrefix) {
        List<Map<String, String>> result = new ArrayList<>();
        if (claimsView == null || claimsView.isEmpty()) {
            return result;
        }
        Locale effectiveLocale = (locale != null) ? locale : Locale.getDefault();
        for (ClaimsMappingTarget target : ClaimsMappingTarget.values()) {
            String value = claimsView.get(target.getKey());
            if (value == null || value.isBlank()) {
                continue;
            }
            String label = messageSource.getMessage(
                    labelKeyPrefix + target.getKey(),
                    null,
                    target.getKey(),
                    effectiveLocale);
            Map<String, String> entry = new LinkedHashMap<>();
            entry.put("key", target.getKey());
            entry.put("label", label);
            entry.put("value", value);
            result.add(entry);
        }
        return result;
    }

    /**
     * SystemAccountIdentity 単位のクレーム表示マップ解決。
     *
     * @param identity Identity
     * @return クレーム表示マップ（ターゲットキー→値）
     */
    private Map<String, String> resolveClaimsViewForIdentity(SystemAccountIdentity identity) {
        String identityId = identity.getIdentityId();

        Optional<SystemAccountProfile> profileOpt = profileService.getProfile(identityId);
        if (profileOpt.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> claims = profileOpt.get().getClaims();
        if (claims == null || claims.isEmpty()) {
            return Map.of();
        }

        Map<String, String> result = new LinkedHashMap<>();
        for (ClaimsMappingTarget target : ClaimsMappingTarget.values()) {
            Object value = claims.get(target.getKey());
            if (value != null && !value.toString().isBlank()) {
                result.put(target.getKey(), value.toString());
            }
        }
        return result;
    }

}
