package io.github.kizulog_community.kizulog.domain.systemaccountprofile.model;

import java.time.OffsetDateTime;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * システム管理アカウントプロファイルドメインモデル
 *
 * @author Jun Kobayashi
 */
@Getter
@AllArgsConstructor
public class SystemAccountProfile {

    /** Identity ID */
    private final String identityId;

    /** バージョン（レコード作成時刻/UTC） */
    private final OffsetDateTime version;

    /** OIDCクレームのスナップショット */
    private final Map<String, Object> claims;

    /** 作成日時（UTC） */
    private final OffsetDateTime createdAt;

    /** 作成者 */
    private final String createdBy;

}
