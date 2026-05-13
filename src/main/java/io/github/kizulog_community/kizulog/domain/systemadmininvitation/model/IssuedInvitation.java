package io.github.kizulog_community.kizulog.domain.systemadmininvitation.model;

import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 発行された招待情報を表現するオブジェクト
 *
 * @author Jun Kobayashi
 */
@Getter
@AllArgsConstructor
public class IssuedInvitation {

    /** 招待ID（UUID v4） */
    private final String invitationId;

    /** 平文トークン（発行直後の1回のみ取得可能） */
    private final String plainToken;

    /** 有効期限 */
    private final OffsetDateTime expiresAt;

}
