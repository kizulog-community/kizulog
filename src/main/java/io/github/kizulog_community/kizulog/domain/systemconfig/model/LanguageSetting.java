package io.github.kizulog_community.kizulog.domain.systemconfig.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.github.kizulog_community.kizulog.domain.shared.SupportedLanguage;
import lombok.Getter;

/**
 * 言語設定ドメインモデル
 *
 * <p>システム全体の言語設定を表現するオブジェクト。
 * デフォルト言語と利用可能言語のリストで構成される。</p>
 *
 * <p>不変条件：
 * <ul>
 * <li>デフォルト言語は必須（null不可）</li>
 * <li>利用可能言語リストは1つ以上必須</li>
 * <li>デフォルト言語は利用可能言語リストに含まれる必要がある</li>
 * </ul>
 * </p>
 *
 * @author Jun Kobayashi
 */
@Getter
public class LanguageSetting {

    /** デフォルト言語 */
    private final SupportedLanguage defaultLanguage;

    /** 利用可能言語リスト */
    private final List<SupportedLanguage> availableLanguages;

    /**
     * コンストラクタ
     *
     * @param defaultLanguage デフォルト言語
     * @param availableLanguages 利用可能言語リスト
     */
    public LanguageSetting(
            SupportedLanguage defaultLanguage,
            List<SupportedLanguage> availableLanguages) {
        this.defaultLanguage = defaultLanguage;
        this.availableLanguages = (availableLanguages == null)
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(availableLanguages));
    }

}
