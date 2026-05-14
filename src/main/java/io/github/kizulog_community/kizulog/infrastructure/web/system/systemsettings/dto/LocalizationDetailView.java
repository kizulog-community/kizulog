package io.github.kizulog_community.kizulog.infrastructure.web.system.systemsettings.dto;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

import io.github.kizulog_community.kizulog.domain.shared.SupportedLanguage;
import io.github.kizulog_community.kizulog.domain.shared.SupportedTimezone;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 言語・タイムゾーン設定詳細表示用DTO
 *
 * @author Jun Kobayashi
 */
@Getter
@AllArgsConstructor
public class LocalizationDetailView {

    /** デフォルト言語（未保存時はnull） */
    private final SupportedLanguage defaultLanguage;

    /** 利用可能言語リスト（未保存時は空リスト） */
    private final List<SupportedLanguage> availableLanguages;

    /** デフォルトタイムゾーン（未保存時はnull） */
    private final SupportedTimezone defaultTimezone;

    /** 利用可能タイムゾーンリスト（未保存時は空リスト） */
    private final List<SupportedTimezone> availableTimezones;

    /** LANGUAGEのバージョン（=作成日時相当、未保存時はnull） */
    private final OffsetDateTime languageVersion;

    /** LANGUAGEの作成者（未保存時はnull） */
    private final String languageCreatedBy;

    /** TIMEZONEのバージョン（=作成日時相当、未保存時はnull） */
    private final OffsetDateTime timezoneVersion;

    /** TIMEZONEの作成者（未保存時はnull） */
    private final String timezoneCreatedBy;

    /**
     * 言語設定が保存済みかを判定する。
     *
     * @return 言語設定が保存済みの場合true
     */
    public boolean isLanguageConfigured() {
        return defaultLanguage != null
                && availableLanguages != null
                && !availableLanguages.isEmpty();
    }

    /**
     * タイムゾーン設定が保存済みかを判定する。
     *
     * @return タイムゾーン設定が保存済みの場合true
     */
    public boolean isTimezoneConfigured() {
        return defaultTimezone != null
                && availableTimezones != null
                && !availableTimezones.isEmpty();
    }

    /**
     * 未保存状態のビューを生成するファクトリメソッド。
     *
     * @return 未保存状態のLocalizationDetailView
     */
    public static LocalizationDetailView empty() {
        return new LocalizationDetailView(null, Collections.emptyList()
        		, null, Collections.emptyList(), null, null, null, null);
    }

}