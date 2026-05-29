package io.github.kizulog_community.kizulog.infrastructure.web.tenant.invite;

import java.io.Serializable;

import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

import lombok.Getter;
import lombok.Setter;

/**
 * テナント管理者招待受諾セッション情報
 *
 * <p>テナントホスト上の受諾リンク（/admin-invite/{token}}）でトークン検証に成功した後、
 * OIDC ログイン（/oauth2/authorization/tenant-{tenantId}-{providerId}）を経て
 * コールバックに戻るまでの中間状態を保持する。</p>
 *
 * @author Jun Kobayashi
 */
@Component
@Scope(value = WebApplicationContext.SCOPE_SESSION, proxyMode = ScopedProxyMode.TARGET_CLASS)
@Getter
@Setter
public class TenantInvitationAcceptanceSession implements Serializable {

    /** シリアライズUID */
    private static final long serialVersionUID = 1L;

    /** 検証済み招待ID */
    private String invitationId;

    /** 受諾アクセス元ホストから解決したテナントID */
    private String tenantId;

    /** 平文トークン */
    private String plainToken;

    /** 招待先表示名 */
    private String displayName;

    /**
     * 招待待ち状態かを判定する。
     *
     * @return invitationId と tenantId 両方が設定されていれば true
     */
    public boolean isPending() {
        return invitationId != null && !invitationId.isBlank()
                && tenantId != null && !tenantId.isBlank();
    }

    /**
     * セッション情報をクリアする。
     */
    public void clear() {
        this.invitationId = null;
        this.tenantId = null;
        this.plainToken = null;
        this.displayName = null;
    }

}
