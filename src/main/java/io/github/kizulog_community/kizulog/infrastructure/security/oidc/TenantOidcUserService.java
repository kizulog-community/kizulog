package io.github.kizulog_community.kizulog.infrastructure.security.oidc;

import java.util.Optional;
import java.util.Set;

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

import io.github.kizulog_community.kizulog.domain.tenantaccount.model.TenantAccountIdentity;
import io.github.kizulog_community.kizulog.domain.tenantaccount.model.TenantRole;
import io.github.kizulog_community.kizulog.domain.tenantaccount.port.TenantAccountIdentityRepository;
import io.github.kizulog_community.kizulog.domain.tenantaccountprofile.service.TenantAccountProfileService;
import io.github.kizulog_community.kizulog.domain.tenantadmininvitation.exception.TenantInvitationError;
import io.github.kizulog_community.kizulog.domain.tenantadmininvitation.service.TenantAdminAcceptanceService;
import io.github.kizulog_community.kizulog.domain.tenantadmininvitation.service.TenantAdminInvitationService;
import io.github.kizulog_community.kizulog.domain.tenantauth.exception.TenantAuthenticationErrorType;
import io.github.kizulog_community.kizulog.domain.tenantauth.exception.TenantAuthenticationException;
import io.github.kizulog_community.kizulog.domain.tenantauth.model.TenantAuthenticationResult;
import io.github.kizulog_community.kizulog.domain.tenantauth.service.TenantAuthenticationService;
import io.github.kizulog_community.kizulog.domain.tenantoidc.model.TenantOidcRegistrationId;
import io.github.kizulog_community.kizulog.infrastructure.security.principal.TenantUserPrincipal;
import io.github.kizulog_community.kizulog.infrastructure.web.tenant.invite.TenantInvitationAcceptanceSession;

/**
 * テナント利用者OIDCユーザーサービス
 *
 * <p>Spring Securityの OAuth2UserService 実装。
 * OIDCプロバイダーから検証済みID Token・UserInfoを取得した後、
 * KizuLogのテナントアカウント情報と突合して認証を完了させる。</p>
 *
 * <p>処理の流れ:
 * <ol>
 * <li>Spring標準の OidcUserService にデリゲートしてID Token検証・UserInfo取得</li>
 * <li>registrationId（tenant-{tenantId}-{providerId}）から tenantId を抽出</li>
 * <li>ID Tokenから iss/sub + ClientRegistrationから aud を抽出</li>
 * <li>セッションの状態に応じて2分岐:
 * <ul>
 * <li>招待pending → 招待受諾フロー（新規 account+identity+role 作成）</li>
 * <li>それ以外 → 通常ログインフロー（既存identityで認証）</li>
 * </ul>
 * </li>
 * <li>TenantUserPrincipalを生成してSpring Securityに返却</li>
 * </ol>
 * 認証失敗時は OAuth2AuthenticationException に変換して投げる。</p>
 *
 * @author Jun Kobayashi
 */
@Component
public class TenantOidcUserService implements OAuth2UserService<OidcUserRequest, OidcUser> {

	/** ロガー */
    private static final Logger log = LoggerFactory.getLogger(TenantOidcUserService.class);

    /** 自動取消の理由文言 */
    private static final String AUTO_CANCEL_REASON = "auto-cancelled: identity already exists";

    /** 自動取消の操作者識別子 */
    private static final String AUTO_CANCEL_OPERATOR = "tenant:auto-cancel";

    /** ドメイン認証サービス */
    private final TenantAuthenticationService tenantAuthenticationService;

    /** identityリポジトリ（招待時の重複チェック用） */
    private final TenantAccountIdentityRepository tenantAccountIdentityRepository;

    /** 招待受諾オーケストレーションサービス */
    private final TenantAdminAcceptanceService tenantAdminAcceptanceService;

    /** 招待管理サービス（自動CANCELLED時に使用） */
    private final TenantAdminInvitationService invitationService;

    /** 招待受諾セッション（sessionスコープProxy Bean） */
    private final TenantInvitationAcceptanceSession invitationSession;

    /** プロファイル保存値Resolver（マッピング解決→ターゲットキーMap化） */
    private final TenantProfileClaimsResolver profileClaimsResolver;

    /** プロファイルキャッシュService */
    private final TenantAccountProfileService profileService;

    /** Spring標準のOidcUserService（デリゲート） */
    private final OAuth2UserService<OidcUserRequest, OidcUser> delegate;

    /**
     * 本番用コンストラクタ
     *
     * @param tenantAuthenticationService ドメイン認証サービス
     * @param tenantAccountIdentityRepository identityリポジトリ
     * @param tenantAdminAcceptanceService 招待受諾オーケストレーションサービス
     * @param invitationService 招待管理サービス
     * @param invitationSession 招待受諾セッション
     * @param profileClaimsResolver プロファイル保存値Resolver
     * @param profileService プロファイルキャッシュService
     */
    @Autowired
    public TenantOidcUserService(
            TenantAuthenticationService tenantAuthenticationService,
            TenantAccountIdentityRepository tenantAccountIdentityRepository,
            TenantAdminAcceptanceService tenantAdminAcceptanceService,
            TenantAdminInvitationService invitationService,
            TenantInvitationAcceptanceSession invitationSession,
            TenantProfileClaimsResolver profileClaimsResolver,
            TenantAccountProfileService profileService) {
        this(
                tenantAuthenticationService,
                tenantAccountIdentityRepository,
                tenantAdminAcceptanceService,
                invitationService,
                invitationSession,
                profileClaimsResolver,
                profileService,
                new OidcUserService());
    }

    /**
     * テスト用コンストラクタ（パッケージプライベート）
     *
     * @param tenantAuthenticationService ドメイン認証サービス
     * @param tenantAccountIdentityRepository identityリポジトリ
     * @param tenantAdminAcceptanceService 招待受諾オーケストレーションサービス
     * @param invitationService 招待管理サービス
     * @param invitationSession 招待受諾セッション
     * @param profileClaimsResolver プロファイル保存値Resolver
     * @param profileService プロファイルキャッシュService
     * @param delegate Spring標準OidcUserService
     */
    TenantOidcUserService(
            TenantAuthenticationService tenantAuthenticationService,
            TenantAccountIdentityRepository tenantAccountIdentityRepository,
            TenantAdminAcceptanceService tenantAdminAcceptanceService,
            TenantAdminInvitationService invitationService,
            TenantInvitationAcceptanceSession invitationSession,
            TenantProfileClaimsResolver profileClaimsResolver,
            TenantAccountProfileService profileService,
            OAuth2UserService<OidcUserRequest, OidcUser> delegate) {
        this.tenantAuthenticationService = tenantAuthenticationService;
        this.tenantAccountIdentityRepository = tenantAccountIdentityRepository;
        this.tenantAdminAcceptanceService = tenantAdminAcceptanceService;
        this.invitationService = invitationService;
        this.invitationSession = invitationSession;
        this.profileClaimsResolver = profileClaimsResolver;
        this.profileService = profileService;
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
        Optional<TenantOidcRegistrationId> parsedOpt =
                TenantOidcRegistrationId.parse(registrationId);
        if (parsedOpt.isEmpty()) {
            log.warn("registrationIdからtenantIdを抽出できません: registrationId={}",
                    registrationId);
            throw new OAuth2AuthenticationException(
                    new OAuth2Error(TenantAuthenticationErrorType.ACCOUNT_NOT_FOUND.name()),
                    "Invalid tenant registrationId");
        }
        String tenantId = parsedOpt.get().getTenantId();

        // 3) iss / aud / sub の抽出
        String iss = oidcUser.getIdToken().getIssuer().toString();
        String aud = userRequest.getClientRegistration().getClientId();
        String sub = oidcUser.getIdToken().getSubject();

        // 4) セッション状態による2分岐
        TenantAccountIdentity identity;
        Set<TenantRole> activeRoles;
        if (invitationSession.isPending()) {
            identity = handleInvitationFlow(tenantId, iss, aud, sub);
            // 受諾フローで作成される管理者ロールは TENANT_ADMIN 固定
            activeRoles = Set.of(TenantRole.TENANT_ADMIN);
        } else {
            TenantAuthenticationResult result =
                    handleStandardLoginFlow(tenantId, iss, aud, sub);
            identity = result.getIdentity();
            activeRoles = result.getActiveRoles();
        }

        // 5) U.0: プロファイル更新（fail-open）
        updateProfileCache(identity, oidcUser, tenantId, iss, aud, sub);

        // 6) TenantUserPrincipalを生成してSpring Securityに返却
        return TenantUserPrincipal.ofTenantUser(
                tenantId,
                identity.getAccountId(),
                identity.getIdentityId(),
                iss,
                aud,
                sub,
                oidcUser.getIdToken(),
                activeRoles);
    }

    /**
     * プロファイルキャッシュを更新する（fail-open）。
     *
     * <p>テナントOIDCプロバイダの claimsMapping に従って生クレームを5項目（氏・名・ミドル・所属・email）へ解決し、
     * ターゲットキー→値のMapとして tenant_account_profiles に差分があった場合のみ新バージョンとして保存する。
     * 生のOIDCクレームは保存しない。
     * キャッシュ更新の失敗はログのみで握りつぶし、認証自体は継続させる。</p>
     *
     * @param identity 認証成功したidentity
     * @param oidcUser OIDCユーザ情報
     * @param tenantId テナントID（プロバイダ特定に使用）
     * @param iss 認証元の issuer（プロバイダ特定に使用）
     * @param aud 認証元の audience（プロバイダ特定に使用）
     * @param sub createdBy として記録する値
     */
    private void updateProfileCache(
            TenantAccountIdentity identity, OidcUser oidcUser,
            String tenantId, String iss, String aud, String sub) {
        try {
            var resolved = profileClaimsResolver.resolveForStorage(
                    tenantId, iss, aud, oidcUser.getClaims());
            if (!resolved.isEmpty()) {
                profileService.upsertIfChanged(identity.getIdentityId(), resolved, sub);
            }
        } catch (RuntimeException e) {
            log.warn("Failed to update tenant profile cache: identityId={}, error={}",
                    identity.getIdentityId(), e.getMessage());
        }
    }

    /**
     * 招待受諾フローの処理。
     *
     * @param resolvedTenantId registrationId 由来のテナントID
     * @param iss OIDC Issuer
     * @param aud OIDC Audience（client_id）
     * @param sub OIDC Subject
     * @return 認証成功時のidentity（新規作成）
     * @throws OAuth2AuthenticationException 招待受諾不能な場合
     */
    private TenantAccountIdentity handleInvitationFlow(
            String resolvedTenantId, String iss, String aud, String sub) {

        String invitationId = invitationSession.getInvitationId();
        String sessionTenantId = invitationSession.getTenantId();
        log.info("テナント招待受諾フロー開始: invitationId={}, resolvedTenantId={}, "
                        + "iss={}, aud={}, sub={}",
                invitationId, resolvedTenantId, iss, aud, sub);

        // 防御的検証: 受諾確認画面でセッションに保存した tenantId と
        // 実際にログインに使われた registrationId 由来の tenantId が一致するか
        if (sessionTenantId != null && !sessionTenantId.equals(resolvedTenantId)) {
            log.warn("受諾フロー: セッションtenantIdとregistrationId由来tenantIdが不一致: "
                            + "sessionTenantId={}, resolvedTenantId={}",
                    sessionTenantId, resolvedTenantId);
            invitationSession.clear();
            throw new OAuth2AuthenticationException(
                    new OAuth2Error(TenantInvitationError.TENANT_MISMATCH.name()),
                    "Tenant mismatch between session and login provider");
        }

        // 既存identityチェック（同一テナント内で同じ iss/aud/sub が既存でないか）
        var existingOpt = tenantAccountIdentityRepository
                .findLatestByTenantIdAndIssAndAudAndSub(resolvedTenantId, iss, aud, sub);

        if (existingOpt.isPresent()) {
            // 既にこのテナントの利用者 → 招待を自動CANCELLED化
            log.warn("テナント招待受諾フロー: 既存identityあり、招待を自動取消: "
                            + "invitationId={}, existingIdentityId={}",
                    invitationId, existingOpt.get().getIdentityId());
            try {
                invitationService.cancelInvitation(
                        resolvedTenantId,
                        invitationId,
                        AUTO_CANCEL_REASON,
                        AUTO_CANCEL_OPERATOR);
            } catch (RuntimeException e) {
                // 同時アクセスで既にCANCELLED/USEDになっていた等
                log.warn("自動取消に失敗（先行操作の可能性）: {}", e.getMessage());
            }
            invitationSession.clear();
            throw new OAuth2AuthenticationException(
                    new OAuth2Error(TenantInvitationError.IDENTITY_EXISTS.name()),
                    "Identity already exists for this tenant account");
        }

        // 新規受諾 → アカウント作成
        TenantAccountIdentity created;
        try {
            created = tenantAdminAcceptanceService.acceptInvitation(
                    invitationId, resolvedTenantId, iss, aud, sub);
        } catch (RuntimeException e) {
            // 例: 同時アクセスで先にmarkAsUsedされた場合のALREADY_USED、テナント不一致等
            log.warn("テナント招待受諾の処理中に失敗: invitationId={}, error={}",
                    invitationId, e.getMessage());
            invitationSession.clear();
            throw new OAuth2AuthenticationException(
                    new OAuth2Error(TenantInvitationError.INVITATION_NOT_FOUND.name()),
                    "Tenant invitation acceptance failed",
                    e);
        }

        invitationSession.clear();
        return created;
    }

    /**
     * 通常ログインフローの処理。
     *
     * @param tenantId テナントID
     * @param iss OIDC Issuer
     * @param aud OIDC Audience（client_id）
     * @param sub OIDC Subject
     * @return 認証結果（identity + 有効ロール）
     * @throws OAuth2AuthenticationException 認証失敗時
     */
    private TenantAuthenticationResult handleStandardLoginFlow(
            String tenantId, String iss, String aud, String sub) {
        try {
            return tenantAuthenticationService.authenticate(tenantId, iss, aud, sub);
        } catch (TenantAuthenticationException e) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error(e.getErrorType().name()), e.getMessage(), e);
        }
    }

}
