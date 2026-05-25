package io.github.kizulog_community.kizulog.infrastructure.security.oidc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

import io.github.kizulog_community.kizulog.domain.tenantauth.exception.TenantAuthenticationErrorType;
import io.github.kizulog_community.kizulog.domain.tenantauth.exception.TenantAuthenticationException;
import io.github.kizulog_community.kizulog.domain.tenantauth.model.TenantAuthenticationResult;
import io.github.kizulog_community.kizulog.domain.tenantauth.service.TenantAuthenticationService;
import io.github.kizulog_community.kizulog.infrastructure.security.principal.TenantUserPrincipal;

/**
 * テナント利用者OIDCユーザーサービス
 *
 * @author Jun Kobayashi
 */
@Component
public class TenantOidcUserService implements OAuth2UserService<OidcUserRequest, OidcUser> {

    /** ロガー */
    private static final Logger log = LoggerFactory.getLogger(TenantOidcUserService.class);

    /** registrationId の固定プレフィックス */
    private static final String PREFIX = "tenant-";

    /** UUID の文字列長（8-4-4-4-12 = 36文字） */
    private static final int UUID_LENGTH = 36;

    /** ドメイン認証サービス */
    private final TenantAuthenticationService tenantAuthenticationService;

    /** Spring標準のOidcUserService（デリゲート） */
    private final OAuth2UserService<OidcUserRequest, OidcUser> delegate;

    /**
     * 本番用コンストラクタ
     *
     * @param tenantAuthenticationService ドメイン認証サービス
     */
    @Autowired
    public TenantOidcUserService(TenantAuthenticationService tenantAuthenticationService) {
        this(tenantAuthenticationService, new OidcUserService());
    }

    /**
     * テスト用コンストラクタ（パッケージプライベート）
     *
     * @param tenantAuthenticationService ドメイン認証サービス
     * @param delegate Spring標準OidcUserService
     */
    TenantOidcUserService(
            TenantAuthenticationService tenantAuthenticationService,
            OAuth2UserService<OidcUserRequest, OidcUser> delegate) {
        this.tenantAuthenticationService = tenantAuthenticationService;
        this.delegate = delegate;
    }

    /**
     * OIDC認証コールバック処理。
     *
     * @param userRequest OIDCユーザーリクエスト（ID Token・ClientRegistration含む）
     * @return KizuLogのテナント利用者を表すTenantUserPrincipal
     * @throws OAuth2AuthenticationException ID Token取得失敗、または認証失敗時
     */
    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        // 1) Spring標準の処理に委譲（ID Token検証・UserInfo取得）
        OidcUser oidcUser = delegate.loadUser(userRequest);

        // 2) registrationId から tenantId 抽出
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String tenantId = extractTenantId(registrationId);
        if (tenantId == null) {
            log.warn("registrationIdからtenantIdを抽出できません: registrationId={}",
                    registrationId);
            throw new OAuth2AuthenticationException(
                    new OAuth2Error(TenantAuthenticationErrorType.ACCOUNT_NOT_FOUND.name()),
                    "Invalid tenant registrationId");
        }

        // 3) iss / aud / sub の抽出
        String iss = oidcUser.getIdToken().getIssuer().toString();
        String aud = userRequest.getClientRegistration().getClientId();
        String sub = oidcUser.getIdToken().getSubject();

        // 4) ドメイン認証
        TenantAuthenticationResult result;
        try {
            result = tenantAuthenticationService.authenticate(tenantId, iss, aud, sub);
        } catch (TenantAuthenticationException e) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error(e.getErrorType().name()), e.getMessage(), e);
        }

        // 5) TenantUserPrincipalを生成してSpring Securityに返却
        return TenantUserPrincipal.ofTenantUser(
                tenantId,
                result.getIdentity().getAccountId(),
                result.getIdentity().getIdentityId(),
                iss,
                aud,
                sub,
                oidcUser.getIdToken(),
                result.getActiveRoles());
    }

    /**
     * registrationId（tenant-{tenantId}-{providerId}）から tenantId を抽出する。
     *
     * @param registrationId 登録ID
     * @return tenantId、抽出できない場合はnull
     */
    private String extractTenantId(String registrationId) {
        if (registrationId == null || !registrationId.startsWith(PREFIX)) {
            return null;
        }
        int uuidStart = PREFIX.length();
        int uuidEnd = uuidStart + UUID_LENGTH;
        // 最低でも "tenant-" + UUID(36) + "-" + providerId(1) の長さが必要
        if (registrationId.length() < uuidEnd + 2) {
            return null;
        }
        if (registrationId.charAt(uuidEnd) != '-') {
            return null;
        }
        return registrationId.substring(uuidStart, uuidEnd);
    }

}
