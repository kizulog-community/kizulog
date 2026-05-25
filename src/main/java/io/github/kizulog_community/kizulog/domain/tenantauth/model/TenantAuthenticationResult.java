package io.github.kizulog_community.kizulog.domain.tenantauth.model;

import java.util.Set;

import io.github.kizulog_community.kizulog.domain.tenantaccount.model.TenantAccountIdentity;
import io.github.kizulog_community.kizulog.domain.tenantaccount.model.TenantRole;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * テナント利用者認証の成功結果
 *
 * @author Jun Kobayashi
 */
@Getter
@AllArgsConstructor
public class TenantAuthenticationResult {

    /** 認証に使用された identity（accountId・tenantId を含む） */
    private final TenantAccountIdentity identity;

    /** アカウントに付与されている有効なロールの集合（空ではないことが保証される） */
    private final Set<TenantRole> activeRoles;

}
