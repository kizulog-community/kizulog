package io.github.kizulog_community.kizulog.infrastructure.web.system.myprofile;

import java.io.Serializable;

import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

import lombok.Getter;
import lombok.Setter;

/**
 * Identityリンクフロー用セッション情報
 *
 * <p>ログイン中の管理者が「別OIDC連携を追加」する際の中間状態を保持する。
 * OIDC認可リダイレクトを発行する前に、現アカウントID・対象providerIdを記録し、
 * コールバック時にこれを参照して連携対象を特定する。</p>
 *
 * <p>セキュリティ要件: コールバック処理時に、セッションに保存された accountId と
 * 現在認証済みの principal の accountId が一致することを確認する。</p>
 *
 * @author Jun Kobayashi
 */
@Component
@Scope(value = WebApplicationContext.SCOPE_SESSION, proxyMode = ScopedProxyMode.TARGET_CLASS)
@Getter
@Setter
public class IdentityLinkSession implements Serializable {

    /** シリアライズUID */
    private static final long serialVersionUID = 1L;

    /** 連携対象のaccountId（操作開始時の認証アカウント） */
    private String targetAccountId;

    /** 連携対象のproviderId */
    private String providerId;

    /**
     * Identityリンク待ち状態かを判定する。
     *
     * @return targetAccountId と providerId 両方が設定されていればtrue
     */
    public boolean isPending() {
        return targetAccountId != null && !targetAccountId.isBlank()
                && providerId != null && !providerId.isBlank();
    }

    /**
     * セッション情報をクリアする。
     */
    public void clear() {
        this.targetAccountId = null;
        this.providerId = null;
    }

}
