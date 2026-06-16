package io.github.kizulog_community.kizulog.infrastructure.web.tenant.header;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import io.github.kizulog_community.kizulog.infrastructure.security.principal.TenantUserPrincipal;
import lombok.RequiredArgsConstructor;

/**
 * テナント画面ヘッダ用ユーザ表示Advice
 *
 * @author Jun Kobayashi
 */
@ControllerAdvice(basePackages = "io.github.kizulog_community.kizulog.infrastructure.web.tenant")
@RequiredArgsConstructor
public class TenantUserDisplayAdvice {

    private static final Logger log = LoggerFactory.getLogger(TenantUserDisplayAdvice.class);

    private final TenantUserDisplayResolver resolver;

    /**
     * テナント側Controllerで利用可能な userDisplay モデル属性を投入する。
     *
     * @param principal 現在のPrincipal（未認証の場合 null）
     * @return ユーザ表示View、未認証時はnull、Resolver例外時もnull（fail-open）
     */
    @ModelAttribute("userDisplay")
    public TenantUserDisplayView userDisplay(
            @AuthenticationPrincipal TenantUserPrincipal principal) {
        if (principal == null) {
            return null;
        }
        try {
            return resolver.resolve(principal);
        } catch (RuntimeException e) {
            log.warn("Failed to resolve tenant user display: identityId={}, error={}",
                    principal.getIdentityId(), e.getMessage());
            return null;
        }
    }

}
