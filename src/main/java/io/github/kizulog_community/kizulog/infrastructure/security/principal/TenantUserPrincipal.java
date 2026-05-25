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

import io.github.kizulog_community.kizulog.domain.tenantaccount.model.TenantRole;
import lombok.Getter;

/**
 * テナント利用者Principal
 *
 * @author Jun Kobayashi
 */
@Getter
public final class TenantUserPrincipal implements OidcUser {

    /** TENANT_ADMINロールのオーソリティ名 */
    public static final String ROLE_TENANT_ADMIN = "ROLE_TENANT_ADMIN";

    /** EMPLOYEEロールのオーソリティ名 */
    public static final String ROLE_EMPLOYEE = "ROLE_EMPLOYEE";

    /** 所属テナントID（tenants.tenant_id） */
    private final String tenantId;

    /** テナントアカウントID（tenant_accounts.account_id） */
    private final String accountId;

    /** 認証に使われたidentityのID（tenant_account_identities.identity_id） */
    private final String identityId;

    /** OIDC Issuer（issクレーム・identity.iss相当） */
    private final String iss;

    /** OIDC Audience（identity.aud相当・clientId） */
    private final String aud;

    /** OIDC Subject（subクレーム・identity.sub相当） */
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
     * @param tenantId 所属テナントID
     * @param accountId テナントアカウントID
     * @param identityId 認証に使われたidentityのID
     * @param iss OIDC Issuer
     * @param aud OIDC Audience
     * @param sub OIDC Subject
     * @param idToken ID Token
     * @param userInfo UserInfo（null可）
     * @param authorities 付与ロール
     */
    public TenantUserPrincipal(
            String tenantId,
            String accountId,
            String identityId,
            String iss,
            String aud,
            String sub,
            OidcIdToken idToken,
            OidcUserInfo userInfo,
            Collection<? extends GrantedAuthority> authorities) {
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId must not be null");
        this.accountId = Objects.requireNonNull(accountId, "accountId must not be null");
        this.identityId = Objects.requireNonNull(identityId, "identityId must not be null");
        this.iss = Objects.requireNonNull(iss, "iss must not be null");
        this.aud = Objects.requireNonNull(aud, "aud must not be null");
        this.sub = Objects.requireNonNull(sub, "sub must not be null");
        this.idToken = Objects.requireNonNull(idToken, "idToken must not be null");
        this.userInfo = userInfo;
        Objects.requireNonNull(authorities, "authorities must not be null");
        this.authorities = Collections.unmodifiableSet(new LinkedHashSet<>(authorities));
    }

    /**
     * 有効ロール集合からテナント利用者Principalを構築する。
     *
     * @param tenantId 所属テナントID
     * @param accountId アカウントID
     * @param identityId 認証に使われたidentityのID
     * @param iss Issuer
     * @param aud Audience
     * @param sub Subject
     * @param idToken ID Token
     * @param activeRoles 有効ロール集合（空でないこと）
     * @return 構築済みPrincipal
     */
    public static TenantUserPrincipal ofTenantUser(
            String tenantId,
            String accountId,
            String identityId,
            String iss,
            String aud,
            String sub,
            OidcIdToken idToken,
            Set<TenantRole> activeRoles) {
        Objects.requireNonNull(activeRoles, "activeRoles must not be null");
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();
        for (TenantRole role : activeRoles) {
            authorities.add(new SimpleGrantedAuthority(toAuthorityName(role)));
        }
        return new TenantUserPrincipal(
                tenantId, accountId, identityId, iss, aud, sub, idToken, null, authorities);
    }

    /**
     * TenantRole を Spring Security の権限名に変換する。
     *
     * @param role テナントロール
     * @return 権限名
     */
    private static String toAuthorityName(TenantRole role) {
        return switch (role) {
            case TENANT_ADMIN -> ROLE_TENANT_ADMIN;
            case EMPLOYEE -> ROLE_EMPLOYEE;
        };
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
     * @return 属性Map
     */
    @Override
    public Map<String, Object> getAttributes() {
        return idToken.getClaims();
    }

    /**
     * Principal名を返す。
     *
     * @return アカウントID
     */
    @Override
    public String getName() {
        return accountId;
    }

}
