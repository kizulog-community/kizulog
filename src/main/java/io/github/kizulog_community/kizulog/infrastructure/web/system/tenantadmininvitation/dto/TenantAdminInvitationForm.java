package io.github.kizulog_community.kizulog.infrastructure.web.system.tenantadmininvitation.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * テナント管理者招待発行フォーム
 *
 * @author Jun Kobayashi
 */
@Getter
@Setter
public class TenantAdminInvitationForm {

    /** 招待先表示名 */
    @NotBlank(message = "{system.tenants.invitation.form.error.displayName.required}")
    @Size(min = 1, max = 100,
            message = "{system.tenants.invitation.form.error.displayName.size}")
    private String displayName;

    /** 有効期間（時間、1〜720） */
    @NotNull(message = "{system.tenants.invitation.form.error.durationHours.required}")
    @Min(value = 1, message = "{system.tenants.invitation.form.error.durationHours.range}")
    @Max(value = 720, message = "{system.tenants.invitation.form.error.durationHours.range}")
    private Integer durationHours;

}
