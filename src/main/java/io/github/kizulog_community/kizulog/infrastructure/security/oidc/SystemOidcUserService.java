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

import io.github.kizulog_community.kizulog.domain.systemaccount.model.SystemAccountIdentity;
import io.github.kizulog_community.kizulog.domain.systemaccount.port.SystemAccountIdentityRepository;
import io.github.kizulog_community.kizulog.domain.systemadmininvitation.exception.InvitationError;
import io.github.kizulog_community.kizulog.domain.systemadmininvitation.service.InvitationAcceptanceService;
import io.github.kizulog_community.kizulog.domain.systemadmininvitation.service.SystemAdminInvitationService;
import io.github.kizulog_community.kizulog.domain.systemauth.exception.SystemAuthenticationException;
import io.github.kizulog_community.kizulog.domain.systemauth.service.SystemAuthenticationService;
import io.github.kizulog_community.kizulog.infrastructure.security.principal.SystemUserPrincipal;
import io.github.kizulog_community.kizulog.infrastructure.web.system.invite.InvitationAcceptanceSession;

/**
 * システム管理者OIDCユーザーサービス
 *
 * <p>Spring Securityの OAuth2UserService 実装。
 * OIDCプロバイダーから検証済みID Token・UserInfoを取得した後、
 * KizuLogのシステム管理アカウント情報と突合して認証を完了させる。</p>
 *
 * <p>処理の流れ:
 * <ol>
 * <li>Spring標準の OidcUserService にデリゲートしてID Token検証・UserInfo取得</li>
 * <li>ID Tokenから iss/sub + ClientRegistrationから aud を抽出</li>
 * <li>セッションに招待待ち状態があるか判定:
 * <ul>
 * <li>あり → 招待受諾フロー(新規account+identity+role作成)</li>
 * <li>なし → 通常ログインフロー(既存identityで認証)</li>
 * </ul>
 * </li>
 * <li>SystemUserPrincipalを生成してSpring Securityに返却</li>
 * </ol>
 * 認証失敗時は OAuth2AuthenticationException に変換して投げる。</p>
 *
 * <p>招待受諾フロー詳細:
 * セッション内のInvitationAcceptanceSessionがpending状態の場合、
 * 1.iss/aud/subで既存identityがあれば「既に登録済」エラー、
 * 2.なければInvitationAcceptanceServiceで新規アカウント+identity+SYSTEM_ADMINロール作成。
 * いずれの場合もセッション状態はクリアする。</p>
 *
 * <p>audの取得についての注釈:
 * Spring SecurityのOidcIdTokenValidatorはID Tokenのaudクレームに
 * ClientRegistrationのclientIdが含まれることを標準で検証する。
 * よって clientRegistration.getClientId() を aud として使う。</p>
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

    /** 招待セッション(sessionスコープProxy Bean) */
    private final InvitationAcceptanceSession invitationSession;

    /** Spring標準のOidcUserService（デリゲート） */
    private final OAuth2UserService<OidcUserRequest, OidcUser> delegate;

    /**
     * 本番用コンストラクタ
     *
     * @param systemAuthenticationService ドメイン認証サービス
     * @param systemAccountIdentityRepository identityリポジトリ
     * @param invitationAcceptanceService 招待受諾オーケストレーションサービス
     * @param invitationService 招待管理サービス
     * @param invitationSession 招待セッション(sessionスコープProxy Bean)
     */
    @Autowired
    public SystemOidcUserService(
            SystemAuthenticationService systemAuthenticationService,
            SystemAccountIdentityRepository systemAccountIdentityRepository,
            InvitationAcceptanceService invitationAcceptanceService,
            SystemAdminInvitationService invitationService,
            InvitationAcceptanceSession invitationSession) {
        this(
                systemAuthenticationService,
                systemAccountIdentityRepository,
                invitationAcceptanceService,
                invitationService,
                invitationSession,
                new OidcUserService());
    }

    /**
     * テスト用コンストラクタ（パッケージプライベート）
     *
     * <p>delegateをモックに差し替え可能にするための入口。</p>
     */
    SystemOidcUserService(
            SystemAuthenticationService systemAuthenticationService,
            SystemAccountIdentityRepository systemAccountIdentityRepository,
            InvitationAcceptanceService invitationAcceptanceService,
            SystemAdminInvitationService invitationService,
            InvitationAcceptanceSession invitationSession,
            OAuth2UserService<OidcUserRequest, OidcUser> delegate) {
        this.systemAuthenticationService = systemAuthenticationService;
        this.systemAccountIdentityRepository = systemAccountIdentityRepository;
        this.invitationAcceptanceService = invitationAcceptanceService;
        this.invitationService = invitationService;
        this.invitationSession = invitationSession;
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

        // 3) 招待ペンディングか通常ログインかを分岐
        SystemAccountIdentity identity;
        if (invitationSession.isPending()) {
            identity = handleInvitationFlow(iss, aud, sub);
        } else {
            identity = handleStandardLoginFlow(iss, aud, sub);
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

    /**
     * 招待受諾フローの処理。
     *
     * <p>iss/aud/subで既存identityを検索し、
     * <ul>
     * <li>見つかった場合: 既にKizuLogに登録済の利用者。招待を自動CANCELLED化して
     *     IDENTITY_EXISTS エラーで認証失敗。受諾者には「既に登録済」を案内する。</li>
     * <li>見つからない場合: 新規利用者。InvitationAcceptanceServiceで
     *     account/identity/role 一式を作成し、招待をUSED状態に更新。</li>
     * </ul>
     * いずれの場合もセッションのpending情報はクリアする。</p>
     *
     * @return 認証成功時のidentity(新規作成 or 既存)
     * @throws OAuth2AuthenticationException 招待受諾不能な場合
     */
    private SystemAccountIdentity handleInvitationFlow(String iss, String aud, String sub) {
        String invitationId = invitationSession.getInvitationId();
        log.info("招待受諾フロー開始: invitationId={}, iss={}, aud={}, sub={}",
                invitationId, iss, aud, sub);

        // 既存identityチェック
        var existingOpt = systemAccountIdentityRepository
                .findLatestByIssAndAudAndSub(iss, aud, sub);

        if (existingOpt.isPresent()) {
            // 既にアカウント保持者 → 招待を自動CANCELLED化
            log.warn("招待受諾フロー: 既存identityあり、招待を自動取消: "
                    + "invitationId={}, existingIdentityId={}",
                    invitationId, existingOpt.get().getIdentityId());
            try {
                invitationService.cancelInvitation(
                        invitationId,
                        "auto-cancelled: identity already exists",
                        "system:auto-cancel");
            } catch (RuntimeException e) {
                // 同時アクセスで既にCANCELLED/USEDになっていた等
                log.warn("自動取消に失敗（先行操作の可能性）: {}", e.getMessage());
            }
            invitationSession.clear();
            throw new OAuth2AuthenticationException(
                    new OAuth2Error(InvitationError.IDENTITY_EXISTS.name()),
                    "Identity already exists for this account");
        }

        // 新規受諾 → アカウント作成
        SystemAccountIdentity created;
        try {
            created = invitationAcceptanceService.acceptInvitation(
                    invitationId, iss, aud, sub);
        } catch (RuntimeException e) {
            // 例: 同時アクセスで先にmarkAsUsedされた場合のALREADY_USED
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
     * 通常ログインフローの処理。
     *
     * @return 認証成功時のidentity
     * @throws OAuth2AuthenticationException 認証失敗時
     */
    private SystemAccountIdentity handleStandardLoginFlow(String iss, String aud, String sub) {
        try {
            return systemAuthenticationService.authenticate(iss, aud, sub);
        } catch (SystemAuthenticationException e) {
            // ドメイン例外をSpring Security例外に変換
            throw new OAuth2AuthenticationException(
                    new OAuth2Error(e.getErrorType().name()), e.getMessage(), e);
        }
    }

}
