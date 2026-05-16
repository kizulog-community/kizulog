package io.github.kizulog_community.kizulog.domain.tenant.service;

import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

/**
 * テナントホスト名の検証および正規化サービス
 *
 * <p>RFC1123準拠ホスト名のフォーマットチェックと、KizuLog独自の追加制約
 * （小文字限定・ポート禁止・スキーマ禁止）を行う。</p>
 *
 * <p>正規化処理: 前後空白除去 + 小文字化。
 * 入力時の大文字混在やスペース誤入力に対し、保存・比較時のキーを揃える。</p>
 *
 * <p>RFC1123準拠ラベルの仕様:</p>
 * <ul>
 * <li>各ラベル: [a-z0-9]([a-z0-9-]*[a-z0-9])?（小文字化後）</li>
 * <li>各ラベル最大63文字</li>
 * <li>ラベル間はドット区切り</li>
 * <li>全長253文字以内</li>
 * <li>ハイフンで始まる/終わるラベルは不可</li>
 * <li>数字のみのラベルも許容（RFC1123拡張）</li>
 * </ul>
 *
 * @author Jun Kobayashi
 */
@Service
public class HostNormalizer {

    /** 単一ラベルのパターン: 先頭・末尾は英数、中間に英数とハイフンを許容、最大63文字 */
    private static final String LABEL_PATTERN = "[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?";

    /** 完全なhostパターン: 1個以上のラベルをドット区切り */
    private static final Pattern HOST_PATTERN =
            Pattern.compile("^" + LABEL_PATTERN + "(?:\\." + LABEL_PATTERN + ")*$");

    /** hostの最大全長 */
    private static final int MAX_HOST_LENGTH = 253;

    /**
     * 入力hostを正規化（trim + 小文字化）した上で、フォーマット検証を行う。
     *
     * <p>入力が null、空、または検証ルールに違反する場合は IllegalArgumentException をスローする。
     * 上位サービスはこの例外を捕捉し、ドメイン例外（TenantRegistrationException 等）に
     * 変換すること。</p>
     *
     * <p>検証項目:</p>
     * <ul>
     * <li>null・空文字でないこと</li>
     * <li>正規化後の全長が1〜253文字</li>
     * <li>スキーマ（http://、https://等）を含まないこと</li>
     * <li>ポート指定（:）を含まないこと</li>
     * <li>RFC1123ホスト名パターンに一致すること</li>
     * </ul>
     *
     * @param raw 入力hostまたはnull
     * @return 正規化済みhost
     * @throws IllegalArgumentException 検証エラー時
     */
    public String normalizeAndValidate(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("host is null");
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("host is empty");
        }
        // スキーマ検出（小文字化前に::を含む形でも検出するため、小文字化前にチェック）
        String lower = trimmed.toLowerCase(java.util.Locale.ROOT);
        if (lower.contains("://")) {
            throw new IllegalArgumentException("host must not contain scheme: " + raw);
        }
        // ポート番号検出（コロンを含む）
        if (lower.contains(":")) {
            throw new IllegalArgumentException("host must not contain port: " + raw);
        }
        // 全長チェック
        if (lower.length() > MAX_HOST_LENGTH) {
            throw new IllegalArgumentException(
                    "host exceeds maximum length " + MAX_HOST_LENGTH + ": " + raw);
        }
        // RFC1123パターンチェック
        if (!HOST_PATTERN.matcher(lower).matches()) {
            throw new IllegalArgumentException("host violates RFC1123 format: " + raw);
        }
        return lower;
    }

}
