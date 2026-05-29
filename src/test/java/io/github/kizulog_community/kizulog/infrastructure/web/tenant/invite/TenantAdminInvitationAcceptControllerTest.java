package io.github.kizulog_community.kizulog.infrastructure.web.tenant.invite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import io.github.kizulog_community.kizulog.domain.tenant.model.Tenant;
import io.github.kizulog_community.kizulog.domain.tenantadmininvitation.exception.TenantInvitationError;
import io.github.kizulog_community.kizulog.domain.tenantadmininvitation.exception.TenantInvitationException;
import io.github.kizulog_community.kizulog.domain.tenantadmininvitation.model.TenantAdminInvitation;
import io.github.kizulog_community.kizulog.domain.tenantadmininvitation.service.TenantAdminInvitationService;
import io.github.kizulog_community.kizulog.domain.tenantoidc.model.TenantOidcProviderChoiceView;
import io.github.kizulog_community.kizulog.domain.tenantoidc.service.TenantOidcProviderService;
import io.github.kizulog_community.kizulog.infrastructure.web.tenant.TenantContext;

/**
 * TenantAdminInvitationAcceptControllerの単体テスト
 *
 * @author Jun Kobayashi
 */
class TenantAdminInvitationAcceptControllerTest {

    private static final String TENANT_ID = "550e8400-e29b-41d4-a716-446655440000";
    private static final String OTHER_TENANT_ID = "660e8400-e29b-41d4-a716-446655440099";
    private static final String INVITATION_ID = "inv-1";
    private static final String TOKEN = "plain-token-value";
    private static final String DISPLAY_NAME = "招待された管理者";
    private static final OffsetDateTime NOW =
            OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

    private TenantAdminInvitationService invitationService;
    private TenantOidcProviderService tenantOidcProviderService;
    private TenantInvitationAcceptanceSession invitationSession;
    private TenantAdminInvitationAcceptController controller;
    private Model model;

    @BeforeEach
    void setUp() {
        invitationService = mock(TenantAdminInvitationService.class);
        tenantOidcProviderService = mock(TenantOidcProviderService.class);
        // セッションは可変POJOのため実体を使う
        invitationSession = new TenantInvitationAcceptanceSession();
        controller = new TenantAdminInvitationAcceptController(
                invitationService, tenantOidcProviderService, invitationSession);
        model = new ConcurrentModel();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private Map<String, Object> toMap(Model model) {
        return new HashMap<>(model.asMap());
    }

    private Tenant tenant(String tenantId) {
        return new Tenant(tenantId, NOW, "テナント", "slug", NOW, "creator");
    }

    private TenantAdminInvitation invitation(String tenantId) {
        return new TenantAdminInvitation(
                INVITATION_ID, NOW, tenantId, "token-hash",
                NOW.plusHours(24), DISPLAY_NAME, NOW, "creator");
    }

    @Test
    @DisplayName("receive: 検証成功時、セッションに保存し受諾確認画面へリダイレクトする")
    void receive_validToken_savesSessionAndRedirects() {
        TenantContext.set(tenant(TENANT_ID));
        when(invitationService.findValidInvitationByToken(TOKEN))
                .thenReturn(invitation(TENANT_ID));

        String view = controller.receive(TOKEN);

        assertThat(view).isEqualTo("redirect:/admin-invite/accept-confirm");
        assertThat(invitationSession.isPending()).isTrue();
        assertThat(invitationSession.getInvitationId()).isEqualTo(INVITATION_ID);
        assertThat(invitationSession.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(invitationSession.getPlainToken()).isEqualTo(TOKEN);
        assertThat(invitationSession.getDisplayName()).isEqualTo(DISPLAY_NAME);
    }

    @Test
    @DisplayName("receive: TenantContext未設定時、INVALID_TOKENエラー画面へ")
    void receive_noTenantContext_redirectsToError() {
        // TenantContextを設定しない

        String view = controller.receive(TOKEN);

        assertThat(view)
                .isEqualTo("redirect:/admin-invite/error?code="
                        + TenantInvitationError.INVALID_TOKEN.name());
        assertThat(invitationSession.isPending()).isFalse();
    }

    @Test
    @DisplayName("receive: トークン検証失敗時、該当エラーコードのエラー画面へ")
    void receive_invalidToken_redirectsToErrorWithCode() {
        TenantContext.set(tenant(TENANT_ID));
        when(invitationService.findValidInvitationByToken(TOKEN))
                .thenThrow(new TenantInvitationException(TenantInvitationError.EXPIRED));

        String view = controller.receive(TOKEN);

        assertThat(view)
                .isEqualTo("redirect:/admin-invite/error?code="
                        + TenantInvitationError.EXPIRED.name());
        assertThat(invitationSession.isPending()).isFalse();
    }

    @Test
    @DisplayName("receive: 招待のテナントと解決テナントが不一致なら TENANT_MISMATCH エラー画面へ")
    void receive_tenantMismatch_redirectsToError() {
        TenantContext.set(tenant(TENANT_ID));
        // 招待は別テナント
        when(invitationService.findValidInvitationByToken(TOKEN))
                .thenReturn(invitation(OTHER_TENANT_ID));

        String view = controller.receive(TOKEN);

        assertThat(view)
                .isEqualTo("redirect:/admin-invite/error?code="
                        + TenantInvitationError.TENANT_MISMATCH.name());
        assertThat(invitationSession.isPending()).isFalse();
    }

    @Test
    @DisplayName("receive: 開始時に前回のセッション状態をクリアする")
    void receive_clearsPreviousSession() {
        // 前回の残骸
        invitationSession.setInvitationId("old-id");
        invitationSession.setTenantId("old-tenant");
        TenantContext.set(tenant(TENANT_ID));
        when(invitationService.findValidInvitationByToken(TOKEN))
                .thenReturn(invitation(TENANT_ID));

        controller.receive(TOKEN);

        // 新しい値で上書きされている（古いidは残っていない）
        assertThat(invitationSession.getInvitationId()).isEqualTo(INVITATION_ID);
        assertThat(invitationSession.getTenantId()).isEqualTo(TENANT_ID);
    }

    @Test
    @DisplayName("acceptConfirm: pendingなし時、INVALID_TOKENエラー画面へ")
    void acceptConfirm_notPending_redirectsToError() {
        String view = controller.acceptConfirm(model);

        assertThat(view)
                .isEqualTo("redirect:/admin-invite/error?code="
                        + TenantInvitationError.INVALID_TOKEN.name());
    }

    @Test
    @DisplayName("acceptConfirm: pending時、displayNameとregistrationId付きプロバイダー一覧をmodelに設定する")
    void acceptConfirm_pending_setsModelAttributes() {
        invitationSession.setInvitationId(INVITATION_ID);
        invitationSession.setTenantId(TENANT_ID);
        invitationSession.setDisplayName(DISPLAY_NAME);
        when(tenantOidcProviderService.findEnabledChoicesByTenantId(TENANT_ID))
                .thenReturn(List.of(
                        new TenantOidcProviderChoiceView(
                                "tenant-" + TENANT_ID + "-keycloak", "Keycloak"),
                        new TenantOidcProviderChoiceView(
                                "tenant-" + TENANT_ID + "-azure", "Azure AD")));

        String view = controller.acceptConfirm(model);

        assertThat(view).isEqualTo("tenant/invite/accept-confirm");
        Map<String, Object> attrs = toMap(model);
        assertThat(attrs.get("displayName")).isEqualTo(DISPLAY_NAME);

        @SuppressWarnings("unchecked")
        List<TenantOidcProviderChoiceView> choices =
                (List<TenantOidcProviderChoiceView>) attrs.get("providers");
        assertThat(choices).hasSize(2);
        assertThat(choices.get(0).getRegistrationId())
                .isEqualTo("tenant-" + TENANT_ID + "-keycloak");
        assertThat(choices.get(0).getDisplayName()).isEqualTo("Keycloak");
        assertThat(choices.get(1).getRegistrationId())
                .isEqualTo("tenant-" + TENANT_ID + "-azure");
    }

    @Test
    @DisplayName("acceptConfirm: ENABLEDプロバイダー0件時も画面を返し、providersは空リスト")
    void acceptConfirm_noProviders_returnsViewWithEmptyList() {
        invitationSession.setInvitationId(INVITATION_ID);
        invitationSession.setTenantId(TENANT_ID);
        invitationSession.setDisplayName(DISPLAY_NAME);
        when(tenantOidcProviderService.findEnabledChoicesByTenantId(TENANT_ID))
                .thenReturn(List.of());

        String view = controller.acceptConfirm(model);

        assertThat(view).isEqualTo("tenant/invite/accept-confirm");
        @SuppressWarnings("unchecked")
        List<TenantOidcProviderChoiceView> choices =
                (List<TenantOidcProviderChoiceView>)
                        toMap(model).get("providers");
        assertThat(choices).isEmpty();
    }

    @Test
    @DisplayName("error: 既知のエラーコードはそのままmodelに設定され、セッションがクリアされる")
    void error_knownCode_setsCodeAndClearsSession() {
        invitationSession.setInvitationId(INVITATION_ID);
        invitationSession.setTenantId(TENANT_ID);

        String view = controller.error(TenantInvitationError.IDENTITY_EXISTS.name(), model);

        assertThat(view).isEqualTo("tenant/invite/error");
        assertThat(toMap(model).get("errorCode"))
                .isEqualTo(TenantInvitationError.IDENTITY_EXISTS.name());
        assertThat(invitationSession.isPending()).isFalse();
    }

    @Test
    @DisplayName("error: 不明なエラーコードはUNKNOWNに正規化される")
    void error_unknownCode_normalizesToUnknown() {
        String view = controller.error("SOMETHING_WRONG", model);

        assertThat(view).isEqualTo("tenant/invite/error");
        assertThat(toMap(model).get("errorCode")).isEqualTo("UNKNOWN");
    }

    @Test
    @DisplayName("error: nullや空のコードはUNKNOWNに正規化される")
    void error_nullOrBlankCode_normalizesToUnknown() {
        assertThat(toMap(modelAfterError(null)).get("errorCode")).isEqualTo("UNKNOWN");
        assertThat(toMap(modelAfterError("")).get("errorCode")).isEqualTo("UNKNOWN");
        assertThat(toMap(modelAfterError("  ")).get("errorCode")).isEqualTo("UNKNOWN");
    }

    @Test
    @DisplayName("error: TENANT_MISMATCHも既知コードとして受理される")
    void error_tenantMismatch_isAccepted() {
        controller.error(TenantInvitationError.TENANT_MISMATCH.name(), model);

        assertThat(toMap(model).get("errorCode"))
                .isEqualTo(TenantInvitationError.TENANT_MISMATCH.name());
    }

    @Test
    @DisplayName("acceptConfirm: pendingなし時はプロバイダーサービスを呼ばない")
    void acceptConfirm_notPending_doesNotQueryProviders() {
        controller.acceptConfirm(model);

        verify(tenantOidcProviderService, never()).findEnabledChoicesByTenantId(org.mockito.ArgumentMatchers.any());
    }

    private Model modelAfterError(String code) {
        Model m = new ConcurrentModel();
        controller.error(code, m);
        return m;
    }

}
