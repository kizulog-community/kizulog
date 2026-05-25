package io.github.kizulog_community.kizulog.infrastructure.security.client;

import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * システム・テナント両方のClientRegistrationを解決する合成リポジトリ
 *
 * <p>KizuLogはマルチFilterChain構成（システム/テナント）で、それぞれ独立した
 * DynamicSystemClientRegistrationRepository と
 * DynamicTenantClientRegistrationRepository を持つ。
 * 両者とも ClientRegistrationRepository を実装するため、
 * Spring BootのOAuth2 Client自動設定（OAuth2AuthorizedClientService等）が
 * 単一のClientRegistrationRepository Bean を要求する箇所で
 * Bean重複により解決できず起動に失敗する。</p>
 *
 * <p>本クラスは両リポジトリを束ね、@Primary として単一の
 * ClientRegistrationRepository Beanを自動設定へ供給する。
 * これにより自動設定される OAuth2AuthorizedClientService がsystem/tenant両方の
 * registrationIdを解決できるようになる。</p>
 *
 * <p>各SecurityFilterChain（SystemSecurityConfig / TenantSecurityConfig）は
 * 従来どおり個別の具象リポジトリを明示注入するため、ログインの認可開始・
 * コールバックの振り分けロジックは本クラス導入の影響を受けない。</p>
 *
 * <p>解決順序: テナント → システム。
 * DynamicTenantClientRegistrationRepository#findByRegistrationId(String) は
 * registrationIdが tenant-{UUID}-{providerId} 形式でない場合、
 * DBアクセスより前にnullを返す（プレフィックス・長さ・パターン検証のみ）
 * そのためシステム用registrationIdが渡ってもテナント側は軽量に空振りし、
 * システム側へフォールバックする。
 * registrationId形式の判定知識をテナント側リポジトリに集約でき、本クラスに形式判定を重複させずに済む。</p>
 *
 * <p>ClientRegistrationRepository#findByRegistrationId(String) の契約に従い、
 * いずれのリポジトリでも見つからない場合はnullを返す。</p>
 *
 * @author Jun Kobayashi
 */
@Component
@Primary
@RequiredArgsConstructor
public class CompositeClientRegistrationRepository implements ClientRegistrationRepository {

    /** テナント側リポジトリ */
    private final DynamicTenantClientRegistrationRepository tenantRepository;

    /** システム側リポジトリ */
    private final DynamicSystemClientRegistrationRepository systemRepository;

    /**
     * registrationIdに対応するClientRegistrationを解決する。
     *
     * @param registrationId 登録ID
     * @return 対応するClientRegistration、見つからなければnull
     */
    @Override
    public ClientRegistration findByRegistrationId(String registrationId) {
        ClientRegistration tenant = tenantRepository.findByRegistrationId(registrationId);
        if (tenant != null) {
            return tenant;
        }
        return systemRepository.findByRegistrationId(registrationId);
    }

}
