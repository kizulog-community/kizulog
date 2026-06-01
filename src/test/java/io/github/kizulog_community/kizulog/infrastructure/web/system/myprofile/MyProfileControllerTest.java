package io.github.kizulog_community.kizulog.infrastructure.web.system.myprofile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import io.github.kizulog_community.kizulog.domain.systemoidc.service.IdentityClaimsViewService;
import io.github.kizulog_community.kizulog.infrastructure.security.principal.SystemUserPrincipal;

/**
 * MyProfileControllerの単体テスト
 *
 * @author Jun Kobayashi
 */
class MyProfileControllerTest {

    private static final String ACCOUNT_ID = "acc-1";
    private static final String IDENTITY_ID = "id-1";
    private static final String ISS = "https://auth.example/realms/master";
    private static final String AUD = "kizulog-master";
    private static final String SUB = "user-uuid-123";

    private IdentityClaimsViewService identityClaimsViewService;
    private MyProfileController controller;
    private Model model;

    @BeforeEach
    void setUp() {
        identityClaimsViewService = mock(IdentityClaimsViewService.class);
        when(identityClaimsViewService.resolveClaimsDisplay(any(), any())).thenReturn(List.of());

        controller = new MyProfileController(identityClaimsViewService);
        model = new ConcurrentModel();
    }

    private SystemUserPrincipal principal() {
        OidcIdToken idToken = OidcIdToken.withTokenValue("v")
                .issuer(ISS).subject(SUB).audience(List.of(AUD)).build();
        return SystemUserPrincipal.ofSystemAdmin(
                ACCOUNT_ID, IDENTITY_ID, ISS, AUD, SUB, idToken);
    }

    @Test
    @DisplayName("index: マイプロフィールメニュー画面を表示し、各種attributeが設定される")
    void index_returnsViewAndSetsAttributes() {
        String view = controller.index(principal(), Locale.JAPANESE, model);

        assertThat(view).isEqualTo("system/my-profile/index");
        assertThat(model.getAttribute("activeMenu")).isEqualTo("my-profile");
        assertThat(model.getAttribute("myClaimsDisplay")).isNotNull();
        assertThat(model.getAttribute("myIss")).isEqualTo(ISS);
        assertThat(model.getAttribute("myAud")).isEqualTo(AUD);
        assertThat(model.getAttribute("mySub")).isEqualTo(SUB);
    }

    @Test
    @DisplayName("index: クレーム情報をmodelに乗せる")
    void index_setsClaims() {
        Map<String, String> entry1 = Map.of("key", "familyName", "label", "姓", "value", "山田");
        Map<String, String> entry2 = Map.of("key", "givenName", "label", "名", "value", "太郎");
        when(identityClaimsViewService.resolveClaimsDisplay(IDENTITY_ID, Locale.JAPANESE))
                .thenReturn(List.of(entry1, entry2));

        controller.index(principal(), Locale.JAPANESE, model);

        @SuppressWarnings("unchecked")
        List<Map<String, String>> myClaimsDisplay =
                (List<Map<String, String>>) model.getAttribute("myClaimsDisplay");
        assertThat(myClaimsDisplay).hasSize(2);
        assertThat(myClaimsDisplay.get(0).get("value")).isEqualTo("山田");
        assertThat(myClaimsDisplay.get(1).get("value")).isEqualTo("太郎");
    }

    @Test
    @DisplayName("index: principalがnullでもエラーにならず空Listとnullがセットされる")
    void index_principalNull() {
        String view = controller.index(null, Locale.JAPANESE, model);

        assertThat(view).isEqualTo("system/my-profile/index");
        assertThat(model.getAttribute("activeMenu")).isEqualTo("my-profile");
        assertThat(model.getAttribute("myClaimsDisplay")).isNotNull();
        assertThat(model.getAttribute("myIss")).isNull();
        assertThat(model.getAttribute("myAud")).isNull();
        assertThat(model.getAttribute("mySub")).isNull();
    }

}
