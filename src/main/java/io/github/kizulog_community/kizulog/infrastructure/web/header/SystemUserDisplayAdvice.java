package io.github.kizulog_community.kizulog.infrastructure.web.header;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import io.github.kizulog_community.kizulog.infrastructure.security.principal.SystemUserPrincipal;
import lombok.RequiredArgsConstructor;

/**
 * システム管理画面ヘッダ用ユーザ表示Advice
 *
 * @author Jun Kobayashi
 */
@ControllerAdvice(basePackages = "io.github.kizulog_community.kizulog.infrastructure.web.system")
@RequiredArgsConstructor
public class SystemUserDisplayAdvice {

    private static final Logger log = LoggerFactory.getLogger(SystemUserDisplayAdvice.class);

    private final SystemUserDisplayResolver resolver;

    /**
     * 全Controllerで利用可能な userDisplay モデル属性を投入する。
     *
     * @param principal 現在のPrincipal（未認証の場合 null）
     * @return ユーザ表示View、未認証時はnull、Resolver例外時もnull（fail-open）
     */
    @ModelAttribute("userDisplay")
    public SystemUserDisplayView userDisplay(
            @AuthenticationPrincipal SystemUserPrincipal principal) {
        if (principal == null) {
            return null;
        }
        try {
            return resolver.resolve(principal);
        } catch (RuntimeException e) {
            log.warn("Failed to resolve user display: identityId={}, error={}",
                    principal.getIdentityId(), e.getMessage());
            return null;
        }
    }

}
