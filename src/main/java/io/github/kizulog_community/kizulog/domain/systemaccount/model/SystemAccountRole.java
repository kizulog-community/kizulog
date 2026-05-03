package io.github.kizulog_community.kizulog.domain.systemaccount.model;

import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * システム管理アカウントロールドメインモデル
 *
 * <p>システム管理者のロールを表現する。
 * 現状はSYSTEM_ADMINのみ。</p>
 *
 * @author Jun Kobayashi
 */
@Getter
@AllArgsConstructor
public class SystemAccountRole {

    /** ロール紐付けID（UUID） */
    private final String roleId;

    /** バージョン */
    private final OffsetDateTime version;

    /** 紐付くアカウントID */
    private final String accountId;

    /** ロール */
    private final SystemRole role;

    /** 作成日時（UTC） */
    private final OffsetDateTime createdAt;

    /** 作成者 */
    private final String createdBy;

}
