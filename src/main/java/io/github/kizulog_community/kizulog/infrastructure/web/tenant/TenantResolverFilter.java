package io.github.kizulog_community.kizulog.infrastructure.web.tenant;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import io.github.kizulog_community.kizulog.domain.tenant.model.Tenant;
import io.github.kizulog_community.kizulog.domain.tenant.model.TenantHost;
import io.github.kizulog_community.kizulog.domain.tenant.model.TenantHostStatus;
import io.github.kizulog_community.kizulog.domain.tenant.model.TenantHostStatusValue;
import io.github.kizulog_community.kizulog.domain.tenant.model.TenantStatus;
import io.github.kizulog_community.kizulog.domain.tenant.model.TenantStatusValue;
import io.github.kizulog_community.kizulog.domain.tenant.port.TenantHostRepository;
import io.github.kizulog_community.kizulog.domain.tenant.port.TenantHostStatusRepository;
import io.github.kizulog_community.kizulog.domain.tenant.port.TenantRepository;
import io.github.kizulog_community.kizulog.domain.tenant.port.TenantStatusRepository;
import io.github.kizulog_community.kizulog.infrastructure.security.matcher.SystemHostMatcher;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * テナント識別フィルタ
 *
 * <p>処理フロー:</p>
 * <ol>
 * <li>リクエストがシステムホスト宛なら素通し（テナント対象外）</li>
 * <li>リクエストhostを取得（取得不可なら404）</li>
 * <li>hostに紐づくACTIVEなテナントを解決（見つからなければ404）</li>
 * <li>テナントステータスを確認（ACTIVE でなければ404）</li>
 * <li>TenantContext にテナントを設定して後続フィルタへ</li>
 * <li>finally で必ず TenantContext#clear()</li>
 * </ol>
 *
 * @author Jun Kobayashi
 */
@RequiredArgsConstructor
public class TenantResolverFilter extends OncePerRequestFilter {

    /** ロガー */
    private static final Logger log = LoggerFactory.getLogger(TenantResolverFilter.class);

    /** システムホスト判定マッチャ */
    private final SystemHostMatcher systemHostMatcher;

    /** テナントリポジトリ */
    private final TenantRepository tenantRepository;

    /** テナントステータスリポジトリ */
    private final TenantStatusRepository tenantStatusRepository;

    /** テナントホストリポジトリ */
    private final TenantHostRepository tenantHostRepository;

    /** テナントホストステータスリポジトリ */
    private final TenantHostStatusRepository tenantHostStatusRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // システムホスト宛は素通し（このフィルタの対象外）
        if (systemHostMatcher.matches(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        // リクエスト host を取得
        String requestHost = systemHostMatcher.resolveHost(request);
        if (requestHost == null) {
            log.warn("リクエストhostが取得できない");
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // host に紐づくACTIVEなテナントを解決
        Optional<Tenant> tenantOpt = resolveActiveTenantByHost(requestHost);
        if (tenantOpt.isEmpty()) {
            log.warn("hostに紐づく有効なテナントが存在しない: host={}", requestHost);
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        Tenant tenant = tenantOpt.get();

        // テナントステータス確認
        TenantStatusValue tenantStatus = tenantStatusRepository
                .findLatestByTenantId(tenant.getTenantId())
                .map(TenantStatus::getStatus)
                .orElse(TenantStatusValue.INACTIVE);
        if (tenantStatus != TenantStatusValue.ACTIVE) {
            log.warn("テナントがACTIVEではない: tenantId={}, host={}, status={}",
                    tenant.getTenantId(), requestHost, tenantStatus);
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // ここまで来たら識別成功
        try {
            TenantContext.set(tenant);
            log.debug("テナント識別成功: tenantId={}, host={}",
                    tenant.getTenantId(), requestHost);
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * host に紐づくACTIVE状態のテナントを解決する。
     *
     * @param host ホスト名（小文字化済み）
     * @return ACTIVE状態のテナント、なければ空Optional
     */
    private Optional<Tenant> resolveActiveTenantByHost(String host) {
        List<TenantHost> hosts = tenantHostRepository.findAllLatestByHost(host);
        for (TenantHost tenantHost : hosts) {
            String tenantId = tenantHost.getTenantId();
            // tenant_host_status がACTIVE か
            TenantHostStatusValue hostStatus = tenantHostStatusRepository
                    .findLatestByTenantIdAndHost(tenantId, host)
                    .map(TenantHostStatus::getStatus)
                    .orElse(TenantHostStatusValue.INACTIVE);
            if (hostStatus != TenantHostStatusValue.ACTIVE) {
                continue;
            }
            // テナント本体を取得
            Optional<Tenant> tenantOpt = tenantRepository.findLatestByTenantId(tenantId);
            if (tenantOpt.isPresent()) {
                return tenantOpt;
            }
        }
        return Optional.empty();
    }

}
