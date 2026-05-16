package io.github.kizulog_community.kizulog.domain.tenant.model;

import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 業務テナント識別ホストステータスドメインモデル
 *
 * <p>tenant_host_status テーブルに対応する。host単位でのACTIVE/INACTIVE
 * 状態を version-based history で表現する。</p>
 *
 * <p>同一 (tenant_id, host) の中でversionが最大のレコードが現在のステータス。
 * 過去レコードは履歴として保全される。</p>
 *
 * <p>テナント本体のステータスとは独立して管理される。
 * テナントが ACTIVE でも、特定のhostだけ INACTIVE にできる。</p>
 *
 * @author Jun Kobayashi
 */
@Getter
@AllArgsConstructor
public class TenantHostStatus {

    /** テナントID */
    private final String tenantId;

    /** ホスト名 */
    private final String host;

    /** バージョン */
    private final OffsetDateTime version;

    /** ステータス値 */
    private final TenantHostStatusValue status;

    /** 変更理由（1-1000文字、必須） */
    private final String reason;

    /** 作成日時（UTC） */
    private final OffsetDateTime createdAt;

    /** 作成者 */
    private final String createdBy;

}
