package io.github.kizulog_community.kizulog.domain.tenant.model;

import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 業務テナント識別ホストドメインモデル
 *
 * <p>tenant_hosts テーブルに対応する。1テナントが複数のhostでアクセス可能、
 * かつ同一hostが複数テナントに紐づく M:N 関係を表現する。</p>
 *
 * <p>識別キーは (tenant_id, host) の組。テナント識別時は slug と組み合わせて
 * 一意なテナントを特定する。</p>
 *
 * <p>バージョン管理: 同一 (tenant_id, host) の中でversionが最大のレコードが
 * 有効値となる（version-based history）。</p>
 *
 * <p>hostは登録後不変。host単位での有効/無効は tenant_host_status で管理。
 * 「hostを変更する」操作は、旧hostを無効化して新hostを追加する形で表現する。</p>
 *
 * <p>host検証ルール:</p>
 * <ul>
 * <li>RFC1123準拠ホスト名: 各ラベルが [a-z0-9]([a-z0-9-]*[a-z0-9])? 、ドット区切り</li>
 * <li>全長253文字以内</li>
 * <li>小文字のみ</li>
 * <li>ポート番号禁止</li>
 * <li>スキーマ禁止</li>
 * </ul>
 *
 * @author Jun Kobayashi
 */
@Getter
@AllArgsConstructor
public class TenantHost {

    /** テナントID */
    private final String tenantId;

    /** ホスト名（RFC1123準拠、小文字のみ、ポート/スキーマ禁止） */
    private final String host;

    /** バージョン */
    private final OffsetDateTime version;

    /** 作成日時（UTC） */
    private final OffsetDateTime createdAt;

    /** 作成者 */
    private final String createdBy;

}
