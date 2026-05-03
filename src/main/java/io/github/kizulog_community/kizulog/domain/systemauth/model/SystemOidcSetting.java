package io.github.kizulog_community.kizulog.domain.systemauth.model;

import java.util.Objects;

import lombok.Getter;
import lombok.ToString;

/**
 * システム管理OIDC設定
 *
 * <p>system_config（key="OIDC"）から取り出したOIDC設定を、
 * clientSecret復号後の状態で保持する不変オブジェクト。</p>
 *
 * <p>セットアップウィザードのフォーム用DTOである
 * io.github.kizulog_community.kizulog.infrastructure.web.setup.OidcSetting
 * とは異なり、本クラスは認証フロー（ClientRegistration構築）用途で利用される。</p>
 *
 * <p>clientSecretは復号済みの平文を保持する。誤ってログ等に出力されないよう、
 * toString()からは除外している。</p>
 *
 * @author Jun Kobayashi
 */
@Getter
@ToString(exclude = "clientSecret")
public final class SystemOidcSetting {

    /** OIDC識別子（システム管理の場合はmaster固定） */
    private final String id;

    /** Issuer URI */
    private final String uri;

    /** Client ID */
    private final String clientId;

    /** Client Secret（復号後の平文） */
    private final String clientSecret;

    /**
     * コンストラクタ
     *
     * @param id OIDC識別子
     * @param uri Issuer URI
     * @param clientId Client ID
     * @param clientSecret 復号済みClient Secret
     */
    public SystemOidcSetting(
            String id, String uri, String clientId, String clientSecret) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.uri = Objects.requireNonNull(uri, "uri must not be null");
        this.clientId = Objects.requireNonNull(clientId, "clientId must not be null");
        this.clientSecret = Objects.requireNonNull(clientSecret, "clientSecret must not be null");
    }

}
