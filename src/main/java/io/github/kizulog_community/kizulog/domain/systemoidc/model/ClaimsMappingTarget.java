package io.github.kizulog_community.kizulog.domain.systemoidc.model;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * クレームマッピング対象属性
 *
 * @author Jun Kobayashi
 */
@Getter
@AllArgsConstructor
public enum ClaimsMappingTarget {

    /** 姓 */
    FAMILY_NAME("familyName", "family_name"),

    /** 名 */
    GIVEN_NAME("givenName", "given_name"),

    /** ミドルネーム */
    MIDDLE_NAME("middleName", "middle_name"),

    /** 所属 */
    ORGANIZATION("organization", "organization"),

    /** メールアドレス */
    EMAIL("email", "email");

    /** 内部識別子 */
    private final String key;

    /** OIDC標準のデフォルトクレーム名 */
    private final String defaultClaimName;

    /**
     * 内部識別子から対応するenum値を取得する。
     *
     * @param key 内部識別子
     * @return 対応するenum値、または該当なしの場合 null
     */
    public static ClaimsMappingTarget fromKey(String key) {
        if (key == null) {
            return null;
        }
        for (ClaimsMappingTarget target : values()) {
            if (target.key.equals(key)) {
                return target;
            }
        }
        return null;
    }

    /**
     * 全属性のデフォルトマッピングを返す。
     *
     * @return 全属性のデフォルトマッピング
     */
    public static Map<String, String> defaultMapping() {
        Map<String, String> result = new LinkedHashMap<>();
        for (ClaimsMappingTarget target : values()) {
            result.put(target.key, target.defaultClaimName);
        }
        return result;
    }

    /**
     * 全属性の一覧をenum定義順で返す。
     *
     * @return 全属性の一覧（不変）
     */
    public static List<ClaimsMappingTarget> orderedList() {
        return Arrays.asList(values());
    }

}
