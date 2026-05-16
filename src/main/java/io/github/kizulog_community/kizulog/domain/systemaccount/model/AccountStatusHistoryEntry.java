package io.github.kizulog_community.kizulog.domain.systemaccount.model;

import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * システム管理アカウントのステータス履歴1件分の表示用ビュー
 *
 * @author Jun Kobayashi
 */
@Getter
@AllArgsConstructor
public class AccountStatusHistoryEntry {

    /** バージョン（履歴上のタイムスタンプにも相当） */
    private final OffsetDateTime version;

    /** その時点のステータス */
    private final AccountStatus status;

    /** 変更理由（招待時は null の可能性あり） */
    private final String reason;

    /** 履歴行の作成日時 */
    private final OffsetDateTime createdAt;

    /** 履歴行の作成者 */
    private final String createdBy;

}
