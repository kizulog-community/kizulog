package io.github.kizulog_community.kizulog.domain.tenant.model;

import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 業務テナントドメインモデル
 *
 * <p>KizuLogが提供する業務テナント（顧客企業・組織等）を表現する。
 * 各テナントは複数のhostでアクセス可能で、URLパスのslugで識別される。</p>
 *
 * <p>バージョン管理: 同一tenant_idの中でversionが最大のレコードが
 * 有効値となる（version-based history）。</p>
 *
 * @author Jun Kobayashi
 */
@Getter
@AllArgsConstructor
public class Tenant {

    /** テナントID */
    private final String tenantId;

    /** バージョン */
    private final OffsetDateTime version;

    /** テナント名 */
    private final String name;

    /** URL用テナント識別子 */
    private final String slug;

    /** 作成日時（UTC） */
    private final OffsetDateTime createdAt;

    /** 作成者 */
    private final String createdBy;

}
