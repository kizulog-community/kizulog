package io.github.kizulog_community.kizulog.infrastructure.web.setup;

import io.github.kizulog_community.kizulog.domain.shared.SupportedLanguage;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Step0フォーム
 *
 * @author Jun Kobayashi
 */
@Getter
@Setter
@NoArgsConstructor
public class Step0FormData {

    /** 選択された言語 */
    @NotNull(message = "{setup.step0.error.language.required}")
    private SupportedLanguage language;

}
