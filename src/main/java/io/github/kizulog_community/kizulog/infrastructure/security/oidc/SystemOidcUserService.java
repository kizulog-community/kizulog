package io.github.kizulog_community.kizulog.infrastructure.security.oidc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

import io.github.kizulog_community.kizulog.domain.systemaccount.model.SystemAccountIdentity;
import io.github.kizulog_community.kizulog.domain.systemauth.exception.SystemAuthenticationException;
import io.github.kizulog_community.kizulog.domain.systemauth.service.SystemAuthenticationService;
import io.github.kizulog_community.kizulog.infrastructure.security.principal.SystemUserPrincipal;

/**
 * システム管理者OIDCユーザーサービス
 *
 * <p>Spring Securityの OAuth2UserService 実装。
 * OIDCプロバイダーから検証済みID Token・UserInfoを取得した後、
 * KizuLogのシステム管理アカウント情報と突合して認証を完了させる。</p>
 *
 * <p>処理の流れ:
 * <ol>
 *   <li>Spring標準の OidcUserService にデリゲートしてID Token検証・UserInfo取得</li>
 *   <li>ID Tokenから iss/sub + ClientRegistrationから aud を抽出</li>
 *   <li>SystemAuthenticationService#authenticateでaccount/identity/role検証
 *       (戻り値はidentity)</li>
 *   <li>SystemUserPrincipalを生成してSpring Securityに返却</li>
 * </ol>
 * 認証失敗時は OAuth2AuthenticationException に変換して投げる。
 * これによりSpring Securityのエラーハンドリング機構（failureUrl等）に委ねられる。</p>
 *
 * <p>audの取得についての注釈:
 * Spring SecurityのOidcIdTokenValidatorはID Tokenのaudクレームに
 * ClientRegistrationのclientIdが含まれることを標準で検証する。
 * よって clientRegistration.getClientId() を aud として使う。</p>
 *
 * <p>UserInfoについての注釈:
 * KizuLogのシステム管理者認証は iss/aud/sub のみを使用し、
 * UserInfo（name/email等）は意図的に使用しない設計。
 * SystemUserPrincipalもUserInfoを保持しないため、ここでは渡さない。</p>
 *
 * @author Jun Kobayashi
 */
@Component
public class SystemOidcUserService implements OAuth2UserService<OidcUserRequest, OidcUser> {

    /** ドメイン認証サービス */
    private final SystemAuthenticationService systemAuthenticationService;

    /** Spring標準のOidcUserService（デリゲート） */
    private final OAuth2UserService<OidcUserRequest, OidcUser> delegate;

    /**
     * 本番用コンストラクタ
     *
     * @param systemAuthenticationService ドメイン認証サービス
     */
    @Autowired
    public SystemOidcUserService(SystemAuthenticationService systemAuthenticationService) {
        this(systemAuthenticationService, new OidcUserService());
    }

    /**
     * テスト用コンストラクタ（パッケージプライベート）
     *
     * <p>delegateをモックに差し替え可能にするための入口。</p>
     *
     * @param systemAuthenticationService ドメイン認証サービス
     * @param delegate Spring標準のOidcUserServiceまたはそのモック
     */
    SystemOidcUserService(
            SystemAuthenticationService systemAuthenticationService,
            OAuth2UserService<OidcUserRequest, OidcUser> delegate) {
        this.systemAuthenticationService = systemAuthenticationService;
        this.delegate = delegate;
    }

    /**
     * OIDC認証コールバック処理。
     *
     * @param userRequest OIDCユーザーリクエスト（ID Token・ClientRegistration含む）
     * @return KizuLogのシステム管理者を表すSystemUserPrincipal
     * @throws OAuth2AuthenticationException ID Token取得失敗、または認証失敗時
     */
    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        // 1) Spring標準の処理に委譲（ID Token検証・UserInfo取得）
        //    OAuth2AuthenticationExceptionはそのまま伝播させる
        OidcUser oidcUser = delegate.loadUser(userRequest);

        // 2) iss / aud / sub の抽出
        String iss = oidcUser.getIdToken().getIssuer().toString();
        String aud = userRequest.getClientRegistration().getClientId();
        String sub = oidcUser.getIdToken().getSubject();

        // 3) ドメイン認証サービスでidentity検索 → アカウント・ロール検証
        SystemAccountIdentity identity;
        try {
            identity = systemAuthenticationService.authenticate(iss, aud, sub);
        } catch (SystemAuthenticationException e) {
            // ドメイン例外をSpring Security例外に変換
            throw new OAuth2AuthenticationException(
                    new OAuth2Error(e.getErrorType().name()),
                    e.getMessage(),
                    e);
        }

        // 4) SystemUserPrincipalを生成してSpring Securityに返却
        return SystemUserPrincipal.ofSystemAdmin(
                identity.getAccountId(),
                identity.getIdentityId(),
                iss,
                aud,
                sub,
                oidcUser.getIdToken());
    }

}
