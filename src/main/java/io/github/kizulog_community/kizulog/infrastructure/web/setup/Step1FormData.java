package io.github.kizulog_community.kizulog.infrastructure.web.setup;

import java.util.ArrayList;
import java.util.List;

import io.github.kizulog_community.kizulog.domain.shared.SupportedLanguage;
import io.github.kizulog_community.kizulog.domain.shared.SupportedTimezone;
import lombok.Getter;
import lombok.Setter;

/**
 * Step1フォーム
 *
 * <p>Step1のフォーム送信データを受け取るDTO。</p>
 *
 * @author Jun Kobayashi
 */
@Getter
@Setter
public class Step1FormData {

    /** デフォルト言語 */
    private SupportedLanguage defaultLanguage;

    /** 利用可能言語リスト */
    private List<SupportedLanguage> availableLanguages = new ArrayList<>();

    /** デフォルトタイムゾーン */
    private SupportedTimezone defaultTimezone;

    /** 利用可能タイムゾーンリスト */
    private List<SupportedTimezone> availableTimezones = new ArrayList<>();

}