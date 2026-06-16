package io.github.kizulog_community.kizulog.domain.tenantoidc.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * クレームマッピング検証・正規化ヘルパー（テナント用）
 *
 * @author Jun Kobayashi
 */
public final class ClaimsMappingValidator {

    private ClaimsMappingValidator() {
        // ユーティリティクラスのためインスタンス化禁止
    }

    /**
     * クレームマッピングを正規化する。
     *
     * @param input 正規化対象のMap
     * @return 正規化されたMapのコピー（LinkedHashMap、定義順）
     */
    public static Map<String, String> normalize(Map<String, String> input) {
        Map<String, String> result = new LinkedHashMap<>();
        if (input == null) {
            return result;
        }
        for (ClaimsMappingTarget target : ClaimsMappingTarget.values()) {
            String raw = input.get(target.getKey());
            if (raw == null) {
                continue;
            }
            String stripped = raw.strip();
            if (stripped.isEmpty()) {
                continue;
            }
            result.put(target.getKey(), stripped);
        }
        return result;
    }

    /**
     * マッピングが空（全属性が未設定）かを判定する。
     *
     * @param mapping 判定対象のマッピング
     * @return 全属性が未設定の場合true
     */
    public static boolean isEmpty(Map<String, String> mapping) {
        return mapping == null || mapping.isEmpty();
    }

}
