package io.github.kizulog_community.kizulog.infrastructure.security.principal;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import lombok.Getter;

/**
 * システム管理者Principal
 *
 * <p>認証済みのシステム管理者を表すPrincipal。
 * Spring SecurityのOidcUserを実装することで、
 * Controllerの引数で{@code @AuthenticationPrincipal}を用いて受け取ることができる。</p>
 *
 * <p>不変オブジェクトとして設計しており、各フィールドはコンストラクタで
 * 確定したのち変更されない。継承による情報改ざんを防ぐためfinalクラスとする。</p>
 *
 * <p>iss/aud/subのフィールド名はOIDC仕様の用語およびSystemAccountモデルと統一している。
 * OidcUserインタフェース由来のgetIssuer()・getAudience()・getSubject()は
 * 親インタフェースのデフォルト実装（ID Tokenから直接取得）が用いられる。</p>
 *
 * @author Jun Kobayashi
 */
@Getter
public final class SystemUserPrincipal implements OidcUser {

    /** SYSTEM_ADMINロールのオーソリティ名 */
    public static final String ROLE_SYSTEM_ADMIN = "ROLE_SYSTEM_ADMIN";

    /** システム管理アカウントID（system_accounts.account_id） */
    private final String accountId;

    /** OIDC Issuer（issクレーム・system_accounts.iss相当） */
    private final String iss;

    /** OIDC Audience（system_accounts.aud相当・clientId） */
    private final String aud;

    /** OIDC Subject（subクレーム・system_accounts.sub相当） */
    private final String sub;

    /** ID Token */
    private final OidcIdToken idToken;

    /** UserInfo Endpointレスポンス（本アプリではnull許容） */
    private final OidcUserInfo userInfo;

    /** 付与済みロール */
    private final Set<GrantedAuthority> authorities;

    /**
     * コンストラクタ
     *
     * @param accountId システム管理アカウントID
     * @param iss OIDC Issuer
     * @param aud OIDC Audience
     * @param sub OIDC Subject
     * @param idToken ID Token
     * @param userInfo UserInfo（null可）
     * @param authorities 付与ロール
     */
    public SystemUserPrincipal(
            String accountId,
            String iss,
            String aud,
            String sub,
            OidcIdToken idToken,
            OidcUserInfo userInfo,
            Collection<? extends GrantedAuthority> authorities) {
        this.accountId = Objects.requireNonNull(accountId, "accountId must not be null");
        this.iss = Objects.requireNonNull(iss, "iss must not be null");
        this.aud = Objects.requireNonNull(aud, "aud must not be null");
        this.sub = Objects.requireNonNull(sub, "sub must not be null");
        this.idToken = Objects.requireNonNull(idToken, "idToken must not be null");
        this.userInfo = userInfo;
        Objects.requireNonNull(authorities, "authorities must not be null");
        this.authorities = Collections.unmodifiableSet(new LinkedHashSet<>(authorities));
    }

    /**
     * SYSTEM_ADMINロールを持つPrincipalを構築する。
     *
     * @param accountId アカウントID
     * @param iss Issuer
     * @param aud Audience
     * @param sub Subject
     * @param idToken ID Token
     * @return 構築済みPrincipal
     */
    public static SystemUserPrincipal ofSystemAdmin(
            String accountId,
            String iss,
            String aud,
            String sub,
            OidcIdToken idToken) {
        Set<GrantedAuthority> roles =
                Set.of(new SimpleGrantedAuthority(ROLE_SYSTEM_ADMIN));
        return new SystemUserPrincipal(
                accountId, iss, aud, sub, idToken, null, roles);
    }

    /**
     * ID Tokenのクレームを返す。
     *
     * @return クレームMap
     */
    @Override
    public Map<String, Object> getClaims() {
        return idToken.getClaims();
    }

    /**
     * ID Tokenの属性を返す。
     *
     * <p>OAuth2User.getAttributes()の実装。本アプリではID Tokenのクレームと同一。</p>
     *
     * @return 属性Map
     */
    @Override
    public Map<String, Object> getAttributes() {
        return idToken.getClaims();
    }

    /**
     * Principal名を返す。
     *
     * <p>Spring Securityのセッション識別やログ出力に利用される。
     * 個人特定情報を含めず、アカウントIDのみとする。</p>
     *
     * @return アカウントID
     */
    @Override
    public String getName() {
        return accountId;
    }

}
