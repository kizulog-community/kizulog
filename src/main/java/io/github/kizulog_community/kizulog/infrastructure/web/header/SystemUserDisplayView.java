package io.github.kizulog_community.kizulog.infrastructure.web.header;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * システム管理画面ヘッダ用ユーザ表示View
 *
 * @author Jun Kobayashi
 */
@Getter
@AllArgsConstructor
public class SystemUserDisplayView {

    /** 姓 */
    private final String familyName;

    /** 名 */
    private final String givenName;

    /** ミドルネーム */
    private final String middleName;

    /** 所属 */
    private final String organization;

    /** email */
    private final String email;

    /**
     * 表示名（姓・名・ミドルネームを組み立てた結果）が空かを判定する。
     *
     * @return 表示名が空ならtrue
     */
    public boolean isNameEmpty() {
        return isBlank(familyName) && isBlank(givenName) && isBlank(middleName);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

}
