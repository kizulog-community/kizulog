package io.github.kizulog_community.kizulog.infrastructure.security.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

/**
 * CompositeClientRegistrationRepositoryの単体テスト
 *
 * @author Jun Kobayashi
 */
class CompositeClientRegistrationRepositoryTest {

    private DynamicTenantClientRegistrationRepository tenantRepository;
    private DynamicSystemClientRegistrationRepository systemRepository;
    private CompositeClientRegistrationRepository composite;

    @BeforeEach
    void setUp() {
        tenantRepository = mock(DynamicTenantClientRegistrationRepository.class);
        systemRepository = mock(DynamicSystemClientRegistrationRepository.class);
        composite = new CompositeClientRegistrationRepository(tenantRepository, systemRepository);
    }

    private ClientRegistration registration(String registrationId) {
        return ClientRegistration.withRegistrationId(registrationId)
                .clientId("client-id")
                .clientSecret("secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("openid")
                .authorizationUri("https://auth.example/oauth2/authorize")
                .tokenUri("https://auth.example/oauth2/token")
                .build();
    }

    @Test
    @DisplayName("findByRegistrationId: テナント側が解決すればそれを返す")
    void findByRegistrationId_returnsTenant_whenTenantResolves() {
        String regId = "tenant-uuid-master";
        ClientRegistration tenantReg = registration(regId);
        when(tenantRepository.findByRegistrationId(regId)).thenReturn(tenantReg);

        ClientRegistration result = composite.findByRegistrationId(regId);

        assertThat(result).isSameAs(tenantReg);
    }

    @Test
    @DisplayName("findByRegistrationId: テナント側が解決したらシステム側は呼ばれない")
    void findByRegistrationId_doesNotCallSystem_whenTenantResolves() {
        String regId = "tenant-uuid-master";
        when(tenantRepository.findByRegistrationId(regId)).thenReturn(registration(regId));

        composite.findByRegistrationId(regId);

        verify(systemRepository, never()).findByRegistrationId(regId);
    }

    @Test
    @DisplayName("findByRegistrationId: テナントがnullならシステム側を返す")
    void findByRegistrationId_returnsSystem_whenTenantReturnsNull() {
        String regId = "master";
        ClientRegistration systemReg = registration(regId);
        when(tenantRepository.findByRegistrationId(regId)).thenReturn(null);
        when(systemRepository.findByRegistrationId(regId)).thenReturn(systemReg);

        ClientRegistration result = composite.findByRegistrationId(regId);

        assertThat(result).isSameAs(systemReg);
    }

    @Test
    @DisplayName("findByRegistrationId: テナントがnullのときシステム側に問い合わせる")
    void findByRegistrationId_callsSystem_whenTenantReturnsNull() {
        String regId = "master";
        when(tenantRepository.findByRegistrationId(regId)).thenReturn(null);
        when(systemRepository.findByRegistrationId(regId)).thenReturn(registration(regId));

        composite.findByRegistrationId(regId);

        verify(systemRepository).findByRegistrationId(regId);
    }

    @Test
    @DisplayName("findByRegistrationId: 両方nullならnullを返す")
    void findByRegistrationId_returnsNull_whenBothReturnNull() {
        String regId = "unknown";
        when(tenantRepository.findByRegistrationId(regId)).thenReturn(null);
        when(systemRepository.findByRegistrationId(regId)).thenReturn(null);

        ClientRegistration result = composite.findByRegistrationId(regId);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("findByRegistrationId: registrationIdがnullでも委譲し両方nullならnull")
    void findByRegistrationId_handlesNullId() {
        when(tenantRepository.findByRegistrationId(null)).thenReturn(null);
        when(systemRepository.findByRegistrationId(null)).thenReturn(null);

        ClientRegistration result = composite.findByRegistrationId(null);

        assertThat(result).isNull();
    }

}
