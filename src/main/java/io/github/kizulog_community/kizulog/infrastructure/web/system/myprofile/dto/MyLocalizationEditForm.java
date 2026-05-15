package io.github.kizulog_community.kizulog.infrastructure.web.system.myprofile.dto;

import io.github.kizulog_community.kizulog.domain.shared.SupportedLanguage;
import io.github.kizulog_community.kizulog.domain.shared.SupportedTimezone;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * マイページ言語・タイムゾーン設定 編集フォーム
 *
 * @author Jun Kobayashi
 */
@Getter
@Setter
public class MyLocalizationEditForm {

    /** 言語 */
    @NotNull
    private SupportedLanguage language;

    /** タイムゾーン */
    @NotNull
    private SupportedTimezone timezone;

}
