package io.github.kizulog_community.kizulog.domain.systemadmininvitation.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 招待と最新ステータスのペアを表現するオブジェクト
 *
 * @author Jun Kobayashi
 */
@Getter
@AllArgsConstructor
public class InvitationWithStatus {

    /** 招待本体 */
    private final SystemAdminInvitation invitation;

    /** 最新ステータス（存在しない場合は null） */
    private final SystemAdminInvitationStatus status;

}
