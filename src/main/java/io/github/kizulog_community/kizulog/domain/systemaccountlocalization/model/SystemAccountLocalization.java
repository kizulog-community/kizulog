package io.github.kizulog_community.kizulog.domain.systemaccountlocalization.model;

import java.time.OffsetDateTime;

import io.github.kizulog_community.kizulog.domain.shared.SupportedLanguage;
import io.github.kizulog_community.kizulog.domain.shared.SupportedTimezone;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * システム管理アカウント言語・タイムゾーン設定ドメインモデル
 *
 * @author Jun Kobayashi
 */
@Getter
@AllArgsConstructor
public class SystemAccountLocalization {

    /** アカウントID */
    private final String accountId;

    /** バージョン */
    private final OffsetDateTime version;

    /** 言語 */
    private final SupportedLanguage language;

    /** タイムゾーン */
    private final SupportedTimezone timezone;

    /** 作成日時（UTC） */
    private final OffsetDateTime createdAt;

    /** 作成者 */
    private final String createdBy;

}
