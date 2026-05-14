package io.github.kizulog_community.kizulog.infrastructure.web.system.accounts.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 招待取消フォーム
 *
 * @author Jun Kobayashi
 */
@Getter
@Setter
public class InvitationCancelForm {

    /** 取消理由 */
    @Size(max = 500, message = "{system.invitations.form.error.reason.size}")
    private String reason;

}
