package io.github.kizulog_community.kizulog.infrastructure.web.system.accounts.dto;

import io.github.kizulog_community.kizulog.domain.systemaccount.model.AccountStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * アカウントステータス変更フォーム
 *
 * @author Jun Kobayashi
 */
@Getter
@Setter
public class AccountStatusChangeForm {

    /** 変更後のステータス（必須） */
    @NotNull(message = "{system.accounts.form.error.status.required}")
    private AccountStatus targetStatus;

    /** 変更理由（必須、1〜1000文字） */
    @NotBlank(message = "{system.accounts.form.error.reason.required}")
    @Size(max = 1000, message = "{system.accounts.form.error.reason.tooLong}")
    private String reason;

}
