package io.github.kizulog_community.kizulog.domain.tenantoidc.model;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 業務テナントOIDCプロバイダー
 *
 * @author Jun Kobayashi
 */
@Getter
@AllArgsConstructor
public class TenantOidcProvider {

    /** テナントID */
    private final String tenantId;

    /** プロバイダー識別子（テナント内ユニーク、[a-z0-9-]+ 1-32文字、不変） */
    private final String providerId;

    /** バージョン */
    private final OffsetDateTime version;

    /** 表示名（管理画面とテナントログイン画面で表示） */
    private final String displayName;

    /** OIDC Issuer URI（不変） */
    private final String iss;

    /** Audience（不変） */
    private final String aud;

    /** クライアントID */
    private final String clientId;

    /** クライアントシークレット（AES暗号化済み） */
    private final String clientSecret;

    /** クレームマッピング設定 */
    private final Map<String, String> claimsMapping;

    /** 作成日時 */
    private final OffsetDateTime createdAt;

    /** 作成者 */
    private final String createdBy;

    /**
     * 指定された属性のOIDCクレームキーを取得する。
     *
     * @param target 取得対象の属性
     * @return マッピングされたOIDCクレームキー、または未設定の場合 null
     */
    public String getClaimKey(ClaimsMappingTarget target) {
        if (target == null || claimsMapping == null) {
            return null;
        }
        String value = claimsMapping.get(target.getKey());
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }

    /**
     * クレームマッピングのコピーを返す。
     *
     * @return クレームマッピングのコピー
     */
    public Map<String, String> getClaimsMappingCopy() {
        if (claimsMapping == null) {
            return new LinkedHashMap<>();
        }
        return new LinkedHashMap<>(claimsMapping);
    }

    /**
     * クレームマッピングの不変ビューを返す。
     *
     * @return クレームマッピングの不変ビュー
     */
    public Map<String, String> getClaimsMappingView() {
        if (claimsMapping == null) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(claimsMapping);
    }

}
