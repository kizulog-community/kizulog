package io.github.kizulog_community.kizulog.infrastructure.web.system.systemsettings.dto;

import java.util.ArrayList;
import java.util.List;

import io.github.kizulog_community.kizulog.domain.shared.SupportedLanguage;
import io.github.kizulog_community.kizulog.domain.shared.SupportedTimezone;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 言語・タイムゾーン設定編集用フォームDTO
 *
 * @author Jun Kobayashi
 */
@Getter
@Setter
@NoArgsConstructor
public class LocalizationEditForm {

    /** デフォルト言語 */
    @NotNull(message = "{system.localization.error.DEFAULT_LANGUAGE_REQUIRED}")
    private SupportedLanguage defaultLanguage;

    /** 利用可能言語リスト */
    @NotEmpty(message = "{system.localization.error.AVAILABLE_LANGUAGES_EMPTY}")
    private List<SupportedLanguage> availableLanguages = new ArrayList<>();

    /** デフォルトタイムゾーン */
    @NotNull(message = "{system.localization.error.DEFAULT_TIMEZONE_REQUIRED}")
    private SupportedTimezone defaultTimezone;

    /** 利用可能タイムゾーンリスト */
    @NotEmpty(message = "{system.localization.error.AVAILABLE_TIMEZONES_EMPTY}")
    private List<SupportedTimezone> availableTimezones = new ArrayList<>();

}