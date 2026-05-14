package io.github.kizulog_community.kizulog.infrastructure.web.system.accounts.dto;

import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 発行直後の招待URL表示用DTO
 *
 * @author Jun Kobayashi
 */
@Getter
@AllArgsConstructor
public class IssuedInvitationView {

    /**
     * トークン込みの招待URL
     */
    private final String inviteUrl;

    /** 有効期限 */
    private final OffsetDateTime expiresAt;

}
