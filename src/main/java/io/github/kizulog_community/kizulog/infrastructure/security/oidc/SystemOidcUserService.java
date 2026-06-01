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

import io.github.kizulog_community.kizulog.domain.systemaccount.exception.IdentityLinkError;
import io.github.kizulog_community.kizulog.domain.systemaccount.exception.IdentityLinkException;
import io.github.kizulog_community.kizulog.domain.systemaccount.model.SystemAccountIdentity;
import io.github.kizulog_community.kizulog.domain.systemaccount.port.SystemAccountIdentityRepository;
import io.github.kizulog_community.kizulog.domain.systemaccount.service.SystemAccountIdentityLinkService;
import io.github.kizulog_community.kizulog.domain.systemaccountprofile.service.SystemAccountProfileService;
import io.github.kizulog_community.kizulog.domain.systemadmininvitation.exception.InvitationError;
import io.github.kizulog_community.kizulog.domain.systemadmininvitation.service.InvitationAcceptanceService;
import io.github.kizulog_community.kizulog.domain.systemadmininvitation.service.SystemAdminInvitationService;
import io.github.kizulog_community.kizulog.domain.systemauth.exception.SystemAuthenticationException;
import io.github.kizulog_community.kizulog.domain.systemauth.service.SystemAuthenticationService;
import io.github.kizulog_community.kizulog.infrastructure.security.principal.SystemUserPrincipal;
import io.github.kizulog_community.kizulog.infrastructure.web.system.invite.InvitationAcceptanceSession;
import io.github.kizulog_community.kizulog.infrastructure.web.system.myprofile.IdentityLinkSession;

/**
 * システム管理者OIDCユーザーサービス
 *
 * @author Jun Kobayashi
 */
@Component
public class SystemOidcUserService implements OAuth2UserService<OidcUserRequest, OidcUser> {

    /** ロガー */
    private static final Logger log = LoggerFactory.getLogger(SystemOidcUserService.class);

    /** ドメイン認証サービス */
    private final SystemAuthenticationService systemAuthenticationService;

    /** identityリポジトリ(招待時の重複チェック用) */
    private final SystemAccountIdentityRepository systemAccountIdentityRepository;

    /** 招待受諾オーケストレーションサービス */
    private final InvitationAcceptanceService invitationAcceptanceService;

    /** 招待管理サービス(自動CANCELLED時に使用) */
    private final SystemAdminInvitationService invitationService;

    /** identityリンクサービス */
    private final SystemAccountIdentityLinkService identityLinkService;

    /** 招待セッション(sessionスコープProxy Bean) */
    private final InvitationAcceptanceSession invitationSession;

    /** identityリンクセッション(sessionスコープProxy Bean) */
    private final IdentityLinkSession identityLinkSession;

    /** OIDCクレームフィルタ（ホワイトリスト） */
    private final OidcClaimsFilter claimsFilter;

    /** プロファイルキャッシュService */
    private final SystemAccountProfileService profileService;

    /** Spring標準のOidcUserService（デリゲート） */
    private final OAuth2UserService<OidcUserRequest, OidcUser> delegate;

    /**
     * 本番用コンストラクタ
     */
    @Autowired
    public SystemOidcUserService(
            SystemAuthenticationService systemAuthenticationService,
            SystemAccountIdentityRepository systemAccountIdentityRepository,
            InvitationAcceptanceService invitationAcceptanceService,
            SystemAdminInvitationService invitationService,
            SystemAccountIdentityLinkService identityLinkService,
            InvitationAcceptanceSession invitationSession,
            IdentityLinkSession identityLinkSession,
            OidcClaimsFilter claimsFilter,
            SystemAccountProfileService profileService) {
        this(
                systemAuthenticationService,
                systemAccountIdentityRepository,
                invitationAcceptanceService,
                invitationService,
                identityLinkService,
                invitationSession,
                identityLinkSession,
                claimsFilter,
                profileService,
                new OidcUserService());
    }

    /**
     * テスト用コンストラクタ（パッケージプライベート）
     */
    SystemOidcUserService(
            SystemAuthenticationService systemAuthenticationService,
            SystemAccountIdentityRepository systemAccountIdentityRepository,
            InvitationAcceptanceService invitationAcceptanceService,
            SystemAdminInvitationService invitationService,
            SystemAccountIdentityLinkService identityLinkService,
            InvitationAcceptanceSession invitationSession,
            IdentityLinkSession identityLinkSession,
            OidcClaimsFilter claimsFilter,
            SystemAccountProfileService profileService,
            OAuth2UserService<OidcUserRequest, OidcUser> delegate) {
        this.systemAuthenticationService = systemAuthenticationService;
        this.systemAccountIdentityRepository = systemAccountIdentityRepository;
        this.invitationAcceptanceService = invitationAcceptanceService;
        this.invitationService = invitationService;
        this.identityLinkService = identityLinkService;
        this.invitationSession = invitationSession;
        this.identityLinkSession = identityLinkSession;
        this.claimsFilter = claimsFilter;
        this.profileService = profileService;
        this.delegate = delegate;
    }

    /**
     * OIDC認証コールバック処理。
     */
    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        // 1) Spring標準の処理に委譲（ID Token検証・UserInfo取得）
        OidcUser oidcUser = delegate.loadUser(userRequest);

        // 2) iss / aud / sub の抽出
        String iss = oidcUser.getIdToken().getIssuer().toString();
        String aud = userRequest.getClientRegistration().getClientId();
        String sub = oidcUser.getIdToken().getSubject();

        // 3) セッション状態による3分岐
        SystemAccountIdentity identity;
        if (invitationSession.isPending()) {
            identity = handleInvitationFlow(iss, aud, sub);
        } else if (identityLinkSession.isPending()) {
            identity = handleIdentityLinkFlow(iss, aud, sub);
        } else {
            identity = handleStandardLoginFlow(iss, aud, sub);
        }

        // 4) R.0: プロファイル更新（fail-open）
        updateProfileCache(identity, oidcUser, sub);

        // 5) SystemUserPrincipalを生成してSpring Securityに返却
        return SystemUserPrincipal.ofSystemAdmin(
                identity.getAccountId(),
                identity.getIdentityId(),
                iss,
                aud,
                sub,
                oidcUser.getIdToken());
    }

    /**
     * プロファイルキャッシュを更新する。
     *
     * @param identity 認証成功したidentity
     * @param oidcUser OIDCユーザ情報
     * @param sub createdBy として記録する値
     */
    private void updateProfileCache(
            SystemAccountIdentity identity, OidcUser oidcUser, String sub) {
        try {
            var filtered = claimsFilter.filter(oidcUser.getClaims());
            if (!filtered.isEmpty()) {
                profileService.upsertIfChanged(identity.getIdentityId(), filtered, sub);
            }
        } catch (RuntimeException e) {
            log.warn("Failed to update profile cache: identityId={}, error={}",
                    identity.getIdentityId(), e.getMessage());
        }
    }

    /**
     * 招待受諾フローの処理。
     */
    private SystemAccountIdentity handleInvitationFlow(String iss, String aud, String sub) {
        String invitationId = invitationSession.getInvitationId();
        log.info("招待受諾フロー開始: invitationId={}, iss={}, aud={}, sub={}",
                invitationId, iss, aud, sub);

        var existingOpt = systemAccountIdentityRepository
                .findLatestByIssAndAudAndSub(iss, aud, sub);

        if (existingOpt.isPresent()) {
            log.warn("招待受諾フロー: 既存identityあり、招待を自動取消: "
                    + "invitationId={}, existingIdentityId={}",
                    invitationId, existingOpt.get().getIdentityId());
            try {
                invitationService.cancelInvitation(
                        invitationId,
                        "auto-cancelled: identity already exists",
                        "system:auto-cancel");
            } catch (RuntimeException e) {
                log.warn("自動取消に失敗（先行操作の可能性）: {}", e.getMessage());
            }
            invitationSession.clear();
            throw new OAuth2AuthenticationException(
                    new OAuth2Error(InvitationError.IDENTITY_EXISTS.name()),
                    "Identity already exists for this account");
        }

        SystemAccountIdentity created;
        try {
            created = invitationAcceptanceService.acceptInvitation(
                    invitationId, iss, aud, sub);
        } catch (RuntimeException e) {
            log.warn("招待受諾の処理中に失敗: invitationId={}, error={}",
                    invitationId, e.getMessage());
            invitationSession.clear();
            throw new OAuth2AuthenticationException(
                    new OAuth2Error(InvitationError.INVITATION_NOT_FOUND.name()),
                    "Invitation acceptance failed",
                    e);
        }

        invitationSession.clear();
        return created;
    }

    /**
     * identityリンクフローの処理。
     */
    private SystemAccountIdentity handleIdentityLinkFlow(String iss, String aud, String sub) {
        String accountId = identityLinkSession.getTargetAccountId();
        String providerId = identityLinkSession.getProviderId();
        log.info("identityリンクフロー開始: accountId={}, providerId={}, iss={}, aud={}, sub={}",
                accountId, providerId, iss, aud, sub);

        try {
            SystemAccountIdentity created = identityLinkService.linkIdentity(
                    accountId, providerId, iss, aud, sub);
            identityLinkSession.clear();
            log.info("identityリンク完了: accountId={}, identityId={}",
                    created.getAccountId(), created.getIdentityId());
            return created;
        } catch (IdentityLinkException e) {
            log.warn("identityリンク失敗: accountId={}, providerId={}, error={}",
                    accountId, providerId, e.getError());
            identityLinkSession.clear();
            throw new OAuth2AuthenticationException(
                    new OAuth2Error(e.getError().name()),
                    "Identity link failed: " + e.getError().name(),
                    e);
        } catch (RuntimeException e) {
            log.warn("identityリンクの処理中に予期せぬエラー: accountId={}, error={}",
                    accountId, e.getMessage());
            identityLinkSession.clear();
            throw new OAuth2AuthenticationException(
                    new OAuth2Error(IdentityLinkError.PROVIDER_NOT_FOUND.name()),
                    "Identity link unexpected error", e);
        }
    }

    /**
     * 通常ログインフローの処理。
     */
    private SystemAccountIdentity handleStandardLoginFlow(String iss, String aud, String sub) {
        try {
            return systemAuthenticationService.authenticate(iss, aud, sub);
        } catch (SystemAuthenticationException e) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error(e.getErrorType().name()), e.getMessage(), e);
        }
    }

}
