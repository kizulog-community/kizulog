package io.github.kizulog_community.kizulog.infrastructure.web.system.invite;

import java.io.Serializable;

import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

import lombok.Getter;
import lombok.Setter;

/**
 * 招待受諾セッション情報
 *
 * @author Jun Kobayashi
 */
@Component
@Scope(value = WebApplicationContext.SCOPE_SESSION, proxyMode = ScopedProxyMode.TARGET_CLASS)
@Getter
@Setter
public class InvitationAcceptanceSession implements Serializable {

    /** シリアライズUID */
    private static final long serialVersionUID = 1L;

    /** 検証済み招待ID */
    private String invitationId;

    /** 平文トークン */
    private String plainToken;

    /** 招待先表示名 */
    private String displayName;

    /**
     * 招待待ち状態かを判定する。
     *
     * @return 招待待ち状態ならtrue
     */
    public boolean isPending() {
        return invitationId != null && !invitationId.isBlank();
    }

    /**
     * セッション情報をクリアする。
     */
    public void clear() {
        this.invitationId = null;
        this.plainToken = null;
        this.displayName = null;
    }

}
