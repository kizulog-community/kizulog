package io.github.kizulog_community.kizulog.infrastructure.web.system.tenants.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * テナント編集フォーム
 *
 * @author Jun Kobayashi
 */
@Getter
@Setter
public class TenantEditForm {

    /** テナント名（必須、1〜100文字） */
    @NotBlank(message = "{system.tenants.form.error.name.required}")
    @Size(max = 100, message = "{system.tenants.form.error.name.tooLong}")
    private String name;

    /** 変更理由（必須、1〜1000文字） */
    @NotBlank(message = "{system.tenants.form.error.reason.required}")
    @Size(max = 1000, message = "{system.tenants.form.error.reason.tooLong}")
    private String reason;

}
