package io.github.kizulog_community.kizulog.infrastructure.web.system.myprofile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import io.github.kizulog_community.kizulog.domain.systemaccount.exception.IdentityLinkError;
import io.github.kizulog_community.kizulog.domain.systemaccount.exception.IdentityLinkException;
import io.github.kizulog_community.kizulog.domain.systemaccount.model.LinkedIdentityView;
import io.github.kizulog_community.kizulog.domain.systemaccount.model.SystemAccountIdentity;
import io.github.kizulog_community.kizulog.domain.systemaccount.port.SystemAccountIdentityRepository;
import io.github.kizulog_community.kizulog.domain.systemaccount.service.SystemAccountIdentityLinkService;
import io.github.kizulog_community.kizulog.domain.systemoidc.service.IdentityClaimsViewService;
import io.github.kizulog_community.kizulog.infrastructure.security.principal.SystemUserPrincipal;

/**
 * OidcLinksControllerの単体テスト
 *
 * @author Jun Kobayashi
 */
class OidcLinksControllerTest {

    private static final String ACCOUNT_ID = "acc-1";
    private static final String CURRENT_IDENTITY_ID = "id-current";
    private static final String ISS = "https://auth.example/realms/master";
    private static final String AUD = "kizulog-master";
    private static final String SUB = "user-uuid-123";
    private static final OffsetDateTime VERSION =
            OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

    private SystemAccountIdentityLinkService identityLinkService;
    private IdentityLinkSession identityLinkSession;
    private SystemAccountIdentityRepository identityRepository;
    private IdentityClaimsViewService identityClaimsViewService;
    private OidcLinksController sut;

    @BeforeEach
    void setUp() {
        identityLinkService = mock(SystemAccountIdentityLinkService.class);
        identityLinkSession = mock(IdentityLinkSession.class);
        identityRepository = mock(SystemAccountIdentityRepository.class);
        identityClaimsViewService = mock(IdentityClaimsViewService.class);

        when(identityRepository.findLatestByIdentityId(any())).thenReturn(Optional.empty());
        when(identityClaimsViewService.resolveClaimsView(any())).thenReturn(Map.of());

        sut = new OidcLinksController(
                identityLinkService, identityLinkSession,
                identityRepository, identityClaimsViewService);
    }

    private SystemUserPrincipal principal() {
        OidcIdToken idToken = OidcIdToken.withTokenValue("v")
                .issuer(ISS).subject(SUB).audience(List.of(AUD)).build();
        return SystemUserPrincipal.ofSystemAdmin(
                ACCOUNT_ID, CURRENT_IDENTITY_ID, ISS, AUD, SUB, idToken);
    }

    private LinkedIdentityView linkedIdentityView(
            String identityId, boolean active, boolean currentSession) {
        return new LinkedIdentityView(
                identityId, ISS, SUB, "master", "Keycloak Master",
                true, active, currentSession, VERSION, VERSION);
    }

    private SystemAccountIdentity identityOf(String identityId) {
        return new SystemAccountIdentity(
                identityId, VERSION, ACCOUNT_ID, ISS, AUD, SUB, VERSION, "test");
    }

    @Test
    @DisplayName("list: 連携一覧をmodelに乗せ、activeMenuとactiveCountを設定")
    void list_setsAttributes() {
        LinkedIdentityView active = linkedIdentityView("id-1", true, true);
        LinkedIdentityView inactive = linkedIdentityView("id-2", false, false);
        when(identityLinkService.listLinkedIdentities(ACCOUNT_ID, CURRENT_IDENTITY_ID))
                .thenReturn(List.of(active, inactive));

        when(identityRepository.findLatestByIdentityId("id-1"))
                .thenReturn(Optional.of(identityOf("id-1")));
        when(identityRepository.findLatestByIdentityId("id-2"))
                .thenReturn(Optional.of(identityOf("id-2")));

        Model model = new ConcurrentModel();
        String view = sut.list(principal(), model);

        assertThat(view).isEqualTo("system/my-profile/oidc-links/list");
        assertThat(model.getAttribute("activeMenu")).isEqualTo("my-profile");
        assertThat(model.getAttribute("activeCount")).isEqualTo(1L);
        assertThat((List<?>) model.getAttribute("items")).hasSize(2);
        assertThat(model.getAttribute("audPerIdentity")).isNotNull();
        assertThat(model.getAttribute("claimsPerIdentity")).isNotNull();
        assertThat(model.getAttribute("claimsMappingTargets")).isNotNull();
    }

    @Test
    @DisplayName("list: T.0シリーズ - audとクレーム連携情報をmodelに乗せる")
    void list_setsAudAndClaims() {
        LinkedIdentityView active = linkedIdentityView("id-1", true, true);
        when(identityLinkService.listLinkedIdentities(ACCOUNT_ID, CURRENT_IDENTITY_ID))
                .thenReturn(List.of(active));
        when(identityRepository.findLatestByIdentityId("id-1"))
                .thenReturn(Optional.of(identityOf("id-1")));
        when(identityClaimsViewService.resolveClaimsView("id-1"))
                .thenReturn(Map.of("familyName", "山田", "givenName", "太郎"));

        Model model = new ConcurrentModel();
        sut.list(principal(), model);

        @SuppressWarnings("unchecked")
        Map<String, String> audMap = (Map<String, String>) model.getAttribute("audPerIdentity");
        assertThat(audMap).containsEntry("id-1", AUD);

        @SuppressWarnings("unchecked")
        Map<String, Map<String, String>> claimsMap =
                (Map<String, Map<String, String>>) model.getAttribute("claimsPerIdentity");
        assertThat(claimsMap.get("id-1"))
                .containsEntry("familyName", "山田")
                .containsEntry("givenName", "太郎");
    }

    @Test
    @DisplayName("addForm: 過去のpendingをclearしてリンク可能プロバイダーをmodelに乗せる")
    void addForm_clearsSessionAndListsProviders() {
        when(identityLinkService.listLinkableProvidersForAccount(ACCOUNT_ID))
                .thenReturn(List.of());

        Model model = new ConcurrentModel();
        String view = sut.addForm(principal(), model);

        assertThat(view).isEqualTo("system/my-profile/oidc-links/add");
        assertThat(model.getAttribute("activeMenu")).isEqualTo("my-profile");
        assertThat(model.getAttribute("providers")).isNotNull();
        verify(identityLinkSession).clear();
    }

    @Test
    @DisplayName("startLink: providerId指定→セッションにtargetAccountId/providerIdを保存し/oauth2/authorization/{id}へリダイレクト")
    void startLink_storesSessionAndRedirectsToAuthorization() {
        RedirectAttributes redirectAttrs = new RedirectAttributesModelMap();

        String view = sut.startLink("google", principal(), redirectAttrs);

        assertThat(view).isEqualTo("redirect:/oauth2/authorization/google");
        verify(identityLinkSession).setTargetAccountId(ACCOUNT_ID);
        verify(identityLinkSession).setProviderId("google");
    }

    @Test
    @DisplayName("startLink: providerIdがnull→エラー画面リダイレクト")
    void startLink_nullProviderId_redirectsToError() {
        RedirectAttributes redirectAttrs = new RedirectAttributesModelMap();

        String view = sut.startLink(null, principal(), redirectAttrs);

        assertThat(view).isEqualTo(
                "redirect:/system/my-profile/oidc-links/error?code=PROVIDER_NOT_FOUND");
        verify(identityLinkSession, never()).setProviderId(eq("any"));
    }

    @Test
    @DisplayName("startLink: providerIdが空白→エラー画面リダイレクト")
    void startLink_blankProviderId_redirectsToError() {
        RedirectAttributes redirectAttrs = new RedirectAttributesModelMap();

        String view = sut.startLink("   ", principal(), redirectAttrs);

        assertThat(view).isEqualTo(
                "redirect:/system/my-profile/oidc-links/error?code=PROVIDER_NOT_FOUND");
    }

    @Test
    @DisplayName("unlink: 正常系→Serviceに委譲、success flashで一覧へリダイレクト")
    void unlink_success() {
        RedirectAttributes redirectAttrs = new RedirectAttributesModelMap();

        String view = sut.unlink("id-target", principal(), redirectAttrs);

        verify(identityLinkService).unlinkIdentity(
                ACCOUNT_ID, "id-target", CURRENT_IDENTITY_ID, "User-initiated unlink");
        assertThat(view).isEqualTo("redirect:/system/my-profile/oidc-links");
        assertThat(redirectAttrs.getFlashAttributes().get("flashSuccessKey"))
                .isEqualTo("system.my-profile.oidc-links.flash.unlinked");
    }

    @Test
    @DisplayName("unlink: IdentityLinkException→error flash で一覧へリダイレクト")
    void unlink_failureSetsErrorFlash() {
        RedirectAttributes redirectAttrs = new RedirectAttributesModelMap();
        org.mockito.Mockito.doThrow(new IdentityLinkException(
                IdentityLinkError.CANNOT_UNLINK_LAST_ACTIVE))
                .when(identityLinkService)
                .unlinkIdentity(ACCOUNT_ID, "id-target", CURRENT_IDENTITY_ID,
                        "User-initiated unlink");

        String view = sut.unlink("id-target", principal(), redirectAttrs);

        assertThat(view).isEqualTo("redirect:/system/my-profile/oidc-links");
        assertThat(redirectAttrs.getFlashAttributes().get("flashErrorKey"))
                .isEqualTo("system.my-profile.oidc-links.error.CANNOT_UNLINK_LAST_ACTIVE");
    }

    @Test
    @DisplayName("error: 既知エラーコード→そのままmodelにセットしsession.clear()")
    void error_knownCode() {
        Model model = new ConcurrentModel();
        String view = sut.error("PROVIDER_NOT_FOUND", model);

        assertThat(view).isEqualTo("system/my-profile/oidc-links/error");
        assertThat(model.getAttribute("errorCode")).isEqualTo("PROVIDER_NOT_FOUND");
        verify(identityLinkSession).clear();
    }

    @Test
    @DisplayName("error: 未知エラーコード→UNKNOWN に正規化")
    void error_unknownCode() {
        Model model = new ConcurrentModel();
        String view = sut.error("INJECTED_VALUE", model);

        assertThat(view).isEqualTo("system/my-profile/oidc-links/error");
        assertThat(model.getAttribute("errorCode")).isEqualTo("UNKNOWN");
    }

    @Test
    @DisplayName("error: codeパラメータnull→UNKNOWN に正規化")
    void error_nullCode() {
        Model model = new ConcurrentModel();
        sut.error(null, model);

        assertThat(model.getAttribute("errorCode")).isEqualTo("UNKNOWN");
    }

}
