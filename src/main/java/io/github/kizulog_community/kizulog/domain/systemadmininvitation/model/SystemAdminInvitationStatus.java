package io.github.kizulog_community.kizulog.domain.systemadmininvitation.model;

import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * システム管理者招待ステータスドメインモデル
 *
 * <p>SystemAdminInvitationの状態（PENDING/USED/CANCELLED）を履歴管理する。
 * バージョン管理: 同一invitation_idの中でversionが最大のレコードが有効値となる。</p>
 *
 * @author Jun Kobayashi
 */
@Getter
@AllArgsConstructor
public class SystemAdminInvitationStatus {

    /** 招待ID */
    private final String invitationId;

    /** バージョン */
    private final OffsetDateTime version;

    /** ステータス */
    private final InvitationStatusValue status;

    /** 理由 */
    private final String reason;

    /** 作成日時（UTC） */
    private final OffsetDateTime createdAt;

    /** 作成者 */
    private final String createdBy;

}
