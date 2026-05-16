package io.github.kizulog_community.kizulog.infrastructure.web.system.tenants.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * テナントhost追加フォーム
 *
 * @author Jun Kobayashi
 */
@Getter
@Setter
public class TenantHostAddForm {

    /** ホスト名（必須、1〜253文字） */
    @NotBlank(message = "{system.tenants.form.error.host.required}")
    @Size(max = 253, message = "{system.tenants.form.error.host.tooLong}")
    private String host;

    /** 変更理由（必須、1〜1000文字） */
    @NotBlank(message = "{system.tenants.form.error.reason.required}")
    @Size(max = 1000, message = "{system.tenants.form.error.reason.tooLong}")
    private String reason;

}
