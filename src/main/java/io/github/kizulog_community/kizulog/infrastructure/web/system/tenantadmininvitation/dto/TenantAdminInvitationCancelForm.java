package io.github.kizulog_community.kizulog.infrastructure.web.system.tenantadmininvitation.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * テナント管理者招待取消フォーム
 *
 * @author Jun Kobayashi
 */
@Getter
@Setter
public class TenantAdminInvitationCancelForm {

    /** 取消理由 */
    @Size(max = 500, message = "{system.tenants.invitation.form.error.reason.size}")
    private String reason;

}
