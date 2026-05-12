package io.github.kizulog_community.kizulog.domain.systemoidc.model;

import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * システムOIDCプロバイダーステータスドメインモデル
 *
 * <p>SystemOidcProviderの有効/無効を履歴管理する。
 * バージョン管理: 同一provider_idの中でversionが最大のレコードが有効値となる。</p>
 *
 * @author Jun Kobayashi
 */
@Getter
@AllArgsConstructor
public class SystemOidcProviderStatus {

    /** プロバイダーID */
    private final String providerId;

    /** バージョン */
    private final OffsetDateTime version;

    /** ステータス */
    private final OidcProviderStatusValue status;

    /** 理由 */
    private final String reason;

    /** 作成日時（UTC） */
    private final OffsetDateTime createdAt;

    /** 作成者 */
    private final String createdBy;

}
