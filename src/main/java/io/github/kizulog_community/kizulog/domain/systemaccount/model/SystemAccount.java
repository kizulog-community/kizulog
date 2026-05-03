package io.github.kizulog_community.kizulog.domain.systemaccount.model;

import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * システム管理アカウントドメインモデル
 *
 * <p>バージョン管理：同一account_idの中でversionが最大のレコードが有効値となる。</p>
 *
 * @author Jun Kobayashi
 */
@Getter
@AllArgsConstructor
public class SystemAccount {

    /** アカウントID（UUID） */
    private final String accountId;

    /** バージョン */
    private final OffsetDateTime version;

    /** 作成日時（UTC） */
    private final OffsetDateTime createdAt;

    /** 作成者 */
    private final String createdBy;

}
