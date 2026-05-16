package io.github.kizulog_community.kizulog.domain.tenant.service;

import java.security.SecureRandom;

import org.springframework.stereotype.Service;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;

/**
 * テナントslug自動生成サービス
 *
 * <p>NanoIDライブラリを使用し、文字集合 [a-z0-9-]、長さ21文字のslugを生成する。
 * URL大文字小文字混在を避けるためカスタム文字集合を採用。</p>
 *
 * @author Jun Kobayashi
 */
@Service
public class SlugGenerator {

    /** slug生成に使う文字集合: 小文字英字 + 数字 + ハイフン（37文字） */
    private static final char[] ALPHABET = {
        'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j',
        'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't',
        'u', 'v', 'w', 'x', 'y', 'z',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '-'
    };

    /** slugの長さ */
    private static final int SLUG_LENGTH = 21;

    /** 暗号学的に安全な乱数生成器（NanoIdUtilsデフォルトと同じ） */
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * 新規slug文字列を生成する。
     *
     * <p>戻り値はパターン {@code [a-z0-9-]{21}} に一致する文字列。</p>
     *
     * @return 21文字のslug
     */
    public String generate() {
        return NanoIdUtils.randomNanoId(secureRandom, ALPHABET, SLUG_LENGTH);
    }

}
