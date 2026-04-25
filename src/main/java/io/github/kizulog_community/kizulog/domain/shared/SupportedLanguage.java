package io.github.kizulog_community.kizulog.domain.shared;

import java.util.Locale;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * サポート言語
 *
 * @author Jun Kobayashi
 */
@Getter
@RequiredArgsConstructor
public enum SupportedLanguage {

    JA(Locale.JAPANESE, "日本語");

    /** ロケール（Java標準・BCP47準拠） */
    private final Locale locale;

    /** 表示名 */
    private final String displayName;

    /**
     * 言語コードを返す.
     *
     * @return BCP47準拠の言語タグ
     */
    public String getCode() {
        return locale.toLanguageTag();
    }

}