package io.github.kizulog_community.kizulog.infrastructure.web.tenant;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * テナント識別フィルタ
 *
 * <p>URL パターン /t/{slug}/... からテナントを識別し、TenantContext に設定するフィルタ。</p>
 *
 * <p>処理フロー:</p>
 * <ol>
 * <li>URLが {@code /t/} で始まらない場合は素通し（このフィルタ対象外）</li>
 * <li>URLから slug を抽出（[a-z0-9-]{21}に一致しない場合は404）</li>
 * <li>slug でテナントを検索（存在しなければ404）</li>
 * <li>テナントステータスを確認（ACTIVE でなければ404）</li>
 * <li>リクエストhost が tenant_hosts にACTIVE状態で存在するか確認（不一致なら404）</li>
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

    /** テナントURLパターン: /t/{slug}/... または /t/{slug} */
    private static final Pattern TENANT_URL_PATTERN =
            Pattern.compile("^/t/([a-z0-9-]{21})(?:/.*)?$");

    /** URLパスのテナント識別プレフィックス */
    private static final String TENANT_PATH_PREFIX = "/t/";

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

        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        // contextPath を除去（Spring Boot デフォルトでは contextPath は空）
        if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }

        // /t/ で始まらないリクエストは素通し
        if (!path.startsWith(TENANT_PATH_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        // slug 抽出
        Matcher matcher = TENANT_URL_PATTERN.matcher(path);
        if (!matcher.matches()) {
            log.warn("テナントURL不正: path={}", path);
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String slug = matcher.group(1);

        // テナント検索
        Optional<Tenant> tenantOpt = tenantRepository.findLatestBySlug(slug);
        if (tenantOpt.isEmpty()) {
            log.warn("テナント未存在: slug={}", slug);
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
            log.warn("テナントがACTIVEではない: tenantId={}, slug={}, status={}",
                    tenant.getTenantId(), slug, tenantStatus);
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // リクエスト host を取得・正規化
        String requestHost = extractHost(request);
        if (requestHost == null) {
            log.warn("リクエストhostが取得できない: slug={}", slug);
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // (tenantId, host) ペアの存在 + ACTIVE状態を確認
        if (!isHostActiveForTenant(tenant.getTenantId(), requestHost)) {
            log.warn("hostがテナントに紐づかない or 無効: tenantId={}, host={}, slug={}",
                    tenant.getTenantId(), requestHost, slug);
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // ここまで来たら識別成功
        try {
            TenantContext.set(tenant);
            log.debug("テナント識別成功: tenantId={}, slug={}, host={}",
                    tenant.getTenantId(), slug, requestHost);
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * リクエスト host を取得する。
     *
     * @param request HTTPリクエスト
     * @return ホスト名（小文字化済み）、取得失敗時は null
     */
    private String extractHost(HttpServletRequest request) {
        String serverName = request.getServerName();
        if (serverName == null || serverName.isEmpty()) {
            return null;
        }
        return serverName.toLowerCase(java.util.Locale.ROOT);
    }

    /**
     * 指定 (tenantId, host) がACTIVE状態で存在するかを判定する。
     *
     * @param tenantId テナントID
     * @param host ホスト名
     * @return ACTIVE状態で存在すればtrue
     */
    private boolean isHostActiveForTenant(String tenantId, String host) {
        // tenant_hosts に登録されているか
        List<TenantHost> hosts = tenantHostRepository.findAllLatestByHost(host);
        boolean linkedToTenant = hosts.stream()
                .anyMatch(h -> h.getTenantId().equals(tenantId));
        if (!linkedToTenant) {
            return false;
        }
        // tenant_host_status がACTIVE か
        TenantHostStatusValue status = tenantHostStatusRepository
                .findLatestByTenantIdAndHost(tenantId, host)
                .map(TenantHostStatus::getStatus)
                .orElse(TenantHostStatusValue.INACTIVE);
        return status == TenantHostStatusValue.ACTIVE;
    }

}
