package io.github.kizulog_community.kizulog.infrastructure.web.system.accounts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import io.github.kizulog_community.kizulog.domain.systemaccount.exception.AccountStatusChangeError;
import io.github.kizulog_community.kizulog.domain.systemaccount.exception.AccountStatusChangeException;
import io.github.kizulog_community.kizulog.domain.systemaccount.model.AccountDetailView;
import io.github.kizulog_community.kizulog.domain.systemaccount.model.AccountListItemView;
import io.github.kizulog_community.kizulog.domain.systemaccount.model.AccountStatus;
import io.github.kizulog_community.kizulog.domain.systemaccount.port.SystemAccountIdentityRepository;
import io.github.kizulog_community.kizulog.domain.systemaccount.service.SystemAccountManagementService;
import io.github.kizulog_community.kizulog.domain.systemoidc.port.SystemOidcProviderRepository;
import io.github.kizulog_community.kizulog.domain.systemoidc.service.IdentityClaimsViewService;
import io.github.kizulog_community.kizulog.infrastructure.security.principal.SystemUserPrincipal;
import io.github.kizulog_community.kizulog.infrastructure.web.system.accounts.dto.AccountStatusChangeForm;

/**
 * SystemAccountManagementControllerの単体テスト
 *
 * @author Jun Kobayashi
 */
class SystemAccountManagementControllerTest {

    private static final String OPERATOR_ID = "acc-op";
    private static final String IDENTITY_ID = "id-op";
    private static final String ISS = "https://auth.example/realms/master";
    private static final String AUD = "kizulog-master";
    private static final String SUB = "op-sub";
    private static final OffsetDateTime T0 = OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

    private SystemAccountManagementService service;
    private MessageSource messageSource;
    private SystemAccountIdentityRepository identityRepository;
    private SystemOidcProviderRepository providerRepository;
    private IdentityClaimsViewService identityClaimsViewService;
    private SystemAccountManagementController sut;

    @BeforeEach
    void setUp() {
        service = mock(SystemAccountManagementService.class);
        messageSource = mock(MessageSource.class);
        identityRepository = mock(SystemAccountIdentityRepository.class);
        providerRepository = mock(SystemOidcProviderRepository.class);
        identityClaimsViewService = mock(IdentityClaimsViewService.class);

        when(identityRepository.findLatestByAccountId(any())).thenReturn(List.of());
        when(providerRepository.findAllLatest()).thenReturn(List.of());
        when(identityClaimsViewService.resolveClaimsView(any())).thenReturn(Map.of());
        when(identityClaimsViewService.resolveClaimsViewForAccount(any())).thenReturn(Map.of());
        when(identityClaimsViewService.resolveClaimsDisplay(any(), any())).thenReturn(List.of());
        when(identityClaimsViewService.resolveClaimsDisplayForAccount(any(), any())).thenReturn(List.of());

        sut = new SystemAccountManagementController(
                service, messageSource,
                identityRepository, providerRepository, identityClaimsViewService);
    }

    private SystemUserPrincipal operator() {
        OidcIdToken idToken = OidcIdToken.withTokenValue("v")
                .issuer(ISS).subject(SUB).audience(List.of(AUD)).build();
        return SystemUserPrincipal.ofSystemAdmin(
                OPERATOR_ID, IDENTITY_ID, ISS, AUD, SUB, idToken);
    }

    private static AccountListItemView listItem(String accountId, boolean self) {
        return new AccountListItemView(
                accountId, AccountStatus.ACTIVE, T0, 1, true, self);
    }

    private static AccountDetailView detail(String accountId, boolean self) {
        return new AccountDetailView(
                accountId, T0, "creator",
                AccountStatus.ACTIVE, null, T0, "u:init",
                true, 1, 1,
                List.of(), self);
    }

    @Test
    @DisplayName("list: 一覧をmodelに乗せ、activeMenuをaccountsに設定")
    void list_setsAttributes() {
        AccountListItemView a = listItem("acc-1", false);
        AccountListItemView b = listItem(OPERATOR_ID, true);
        when(service.listAllAccounts(OPERATOR_ID)).thenReturn(List.of(a, b));

        Model model = new ConcurrentModel();
        String view = sut.list(operator(), Locale.JAPANESE, model);

        assertThat(view).isEqualTo("system/accounts/list");
        assertThat(model.getAttribute("activeMenu")).isEqualTo("accounts");
        assertThat(model.getAttribute("items")).isEqualTo(List.of(a, b));
        assertThat(model.getAttribute("claimsDisplayPerAccount")).isNotNull();
        assertThat(model.getAttribute("claimsPerAccount")).isNotNull();
    }

    @Test
    @DisplayName("list: principalがnullでもサービスはnull operatorIdで呼ばれる")
    void list_principalNull_passesNullOperator() {
        when(service.listAllAccounts(null)).thenReturn(List.of());

        Model model = new ConcurrentModel();
        sut.list(null, Locale.JAPANESE, model);

        verify(service).listAllAccounts(null);
    }

    @Test
    @DisplayName("detail: 存在するaccountIdは詳細をmodelに乗せて詳細テンプレートを返す")
    void detail_found_renders() {
        AccountDetailView v = detail("acc-1", false);
        when(service.findAccountDetail("acc-1", OPERATOR_ID)).thenReturn(Optional.of(v));

        Model model = new ConcurrentModel();
        RedirectAttributes redirectAttrs = new RedirectAttributesModelMap();
        String view = sut.detail("acc-1", operator(), Locale.JAPANESE, model, redirectAttrs);

        assertThat(view).isEqualTo("system/accounts/detail");
        assertThat(model.getAttribute("account")).isEqualTo(v);
        assertThat(model.getAttribute("statusChangeForm"))
                .isInstanceOf(AccountStatusChangeForm.class);
        assertThat(model.getAttribute("identitiesView")).isNotNull();
    }

    @Test
    @DisplayName("detail: 存在しないaccountIdは一覧へリダイレクトしフラッシュにエラーキーを設定")
    void detail_notFound_redirects() {
        when(service.findAccountDetail("missing", OPERATOR_ID)).thenReturn(Optional.empty());

        Model model = new ConcurrentModel();
        RedirectAttributes redirectAttrs = new RedirectAttributesModelMap();
        String view = sut.detail("missing", operator(), Locale.JAPANESE, model, redirectAttrs);

        assertThat(view).isEqualTo("redirect:/system/accounts/list");
        assertThat(redirectAttrs.getFlashAttributes().get("flashErrorKey"))
                .isEqualTo("system.accounts.error.notFound");
    }

    @Test
    @DisplayName("changeStatus: 正常系→サービス呼び出し+成功フラッシュ+詳細リダイレクト")
    void changeStatus_success() {
        AccountStatusChangeForm form = new AccountStatusChangeForm();
        form.setTargetStatus(AccountStatus.INACTIVE);
        form.setReason("退職");
        BindingResult br = new BeanPropertyBindingResult(form, "statusChangeForm");

        RedirectAttributes redirectAttrs = new RedirectAttributesModelMap();
        String view = sut.changeStatus(
                "acc-1", form, br, operator(), Locale.JAPANESE, redirectAttrs);

        assertThat(view).isEqualTo("redirect:/system/accounts/acc-1");
        verify(service).changeAccountStatus(
                "acc-1", AccountStatus.INACTIVE, "退職", OPERATOR_ID);
        assertThat(redirectAttrs.getFlashAttributes().get("flashSuccessKey"))
                .isEqualTo("system.accounts.statusChange.success");
    }

    @Test
    @DisplayName("changeStatus: BindingResultエラー→サービス未呼び出し、再オープン用openModal設定")
    void changeStatus_bindingErrors_redirectsWithModal() {
        AccountStatusChangeForm form = new AccountStatusChangeForm();
        BindingResult br = new BeanPropertyBindingResult(form, "statusChangeForm");
        br.reject("dummy");

        RedirectAttributes redirectAttrs = new RedirectAttributesModelMap();
        String view = sut.changeStatus(
                "acc-1", form, br, operator(), Locale.JAPANESE, redirectAttrs);

        assertThat(view).isEqualTo("redirect:/system/accounts/acc-1");
        verify(service, never()).changeAccountStatus(any(), any(), any(), any());
        assertThat(redirectAttrs.getFlashAttributes().get("openModal"))
                .isEqualTo("statusChange");
        assertThat(redirectAttrs.getFlashAttributes().get("statusChangeForm")).isSameAs(form);
    }

    @Test
    @DisplayName("changeStatus: サービスがAccountStatusChangeException→エラーメッセージをフラッシュに設定")
    void changeStatus_serviceThrows_setsErrorFlash() {
        AccountStatusChangeForm form = new AccountStatusChangeForm();
        form.setTargetStatus(AccountStatus.INACTIVE);
        form.setReason("退職");
        BindingResult br = new BeanPropertyBindingResult(form, "statusChangeForm");

        doThrowExceptionOnService(AccountStatusChangeError.CANNOT_DEACTIVATE_LAST_ACTIVE_ADMIN);
        when(messageSource.getMessage(
                eq("system.accounts.form.error.CANNOT_DEACTIVATE_LAST_ACTIVE_ADMIN"),
                any(), anyString(), eq(Locale.JAPANESE)))
                .thenReturn("最後の有効な管理者は無効化できません。");

        RedirectAttributes redirectAttrs = new RedirectAttributesModelMap();
        String view = sut.changeStatus(
                "acc-1", form, br, operator(), Locale.JAPANESE, redirectAttrs);

        assertThat(view).isEqualTo("redirect:/system/accounts/acc-1");
        assertThat(redirectAttrs.getFlashAttributes().get("flashErrorMessage"))
                .isEqualTo("最後の有効な管理者は無効化できません。");
    }

    @Test
    @DisplayName("changeStatus: principalがnullでもoperatorId='system'で呼ばれる")
    void changeStatus_principalNull_usesSystemOperator() {
        AccountStatusChangeForm form = new AccountStatusChangeForm();
        form.setTargetStatus(AccountStatus.INACTIVE);
        form.setReason("退職");
        BindingResult br = new BeanPropertyBindingResult(form, "statusChangeForm");

        RedirectAttributes redirectAttrs = new RedirectAttributesModelMap();
        sut.changeStatus(
                "acc-1", form, br, null, Locale.JAPANESE, redirectAttrs);

        verify(service).changeAccountStatus(
                "acc-1", AccountStatus.INACTIVE, "退職", "system");
    }

    private void doThrowExceptionOnService(AccountStatusChangeError error) {
        org.mockito.Mockito.doThrow(new AccountStatusChangeException(error))
                .when(service).changeAccountStatus(
                        anyString(), any(AccountStatus.class), anyString(), anyString());
    }

}
