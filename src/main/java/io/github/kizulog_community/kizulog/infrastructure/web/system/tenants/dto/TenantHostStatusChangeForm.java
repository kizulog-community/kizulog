package io.github.kizulog_community.kizulog.infrastructure.web.system.tenants.dto;

import io.github.kizulog_community.kizulog.domain.tenant.model.TenantHostStatusValue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * テナントhostステータス変更フォーム
 *
 * @author Jun Kobayashi
 */
@Getter
@Setter
public class TenantHostStatusChangeForm {

    /** 変更後のステータス（必須） */
    @NotNull(message = "{system.tenants.form.error.hostStatus.required}")
    private TenantHostStatusValue targetStatus;

    /** 変更理由（必須、1〜1000文字） */
    @NotBlank(message = "{system.tenants.form.error.reason.required}")
    @Size(max = 1000, message = "{system.tenants.form.error.reason.tooLong}")
    private String reason;

}
