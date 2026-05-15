package io.github.kizulog_community.kizulog.infrastructure.web.system.myprofile.dto;

import java.time.OffsetDateTime;

import io.github.kizulog_community.kizulog.domain.shared.SupportedLanguage;
import io.github.kizulog_community.kizulog.domain.shared.SupportedTimezone;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * マイページ言語・タイムゾーン設定 詳細表示用DTO
 *
 * @author Jun Kobayashi
 */
@Getter
@AllArgsConstructor
public class MyLocalizationDetailView {

    /** アカウントが保存している言語（未設定時はnull） */
    private final SupportedLanguage language;

    /** アカウントが保存しているタイムゾーン（未設定時はnull） */
    private final SupportedTimezone timezone;

    /** 設定の最終更新日時（UTC、未設定時はnull） */
    private final OffsetDateTime version;

    /** 最終更新者（未設定時はnull） */
    private final String createdBy;

    /**
     * 設定可否
     *
     * @return 設定済みならtrue
     */
    public boolean isConfigured() {
        return language != null && timezone != null;
    }

}