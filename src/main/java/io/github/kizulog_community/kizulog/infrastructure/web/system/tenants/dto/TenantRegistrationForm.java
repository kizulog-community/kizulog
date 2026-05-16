package io.github.kizulog_community.kizulog.infrastructure.web.system.tenants.dto;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * テナント新規登録フォーム
 *
 * @author Jun Kobayashi
 */
@Getter
@Setter
public class TenantRegistrationForm {

    /** テナント名（必須、1〜100文字） */
    @NotBlank(message = "{system.tenants.form.error.name.required}")
    @Size(max = 100, message = "{system.tenants.form.error.name.tooLong}")
    private String name;

    /** 登録するhost一覧（最低1個以上、Service層でさらに検証）*/
    private List<String> hosts = new ArrayList<>();

    /** 変更理由（必須、1〜1000文字） */
    @NotBlank(message = "{system.tenants.form.error.reason.required}")
    @Size(max = 1000, message = "{system.tenants.form.error.reason.tooLong}")
    private String reason;

}
