package io.github.kizulog_community.kizulog.domain.tenantadmininvitation.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.kizulog_community.kizulog.domain.tenantadmininvitation.exception.TenantInvitationError;
import io.github.kizulog_community.kizulog.domain.tenantadmininvitation.exception.TenantInvitationException;
import io.github.kizulog_community.kizulog.domain.tenantadmininvitation.model.IssuedTenantInvitation;
import io.github.kizulog_community.kizulog.domain.tenantadmininvitation.model.TenantAdminInvitation;
import io.github.kizulog_community.kizulog.domain.tenantadmininvitation.model.TenantAdminInvitationStatus;
import io.github.kizulog_community.kizulog.domain.tenantadmininvitation.model.TenantInvitationStatusValue;
import io.github.kizulog_community.kizulog.domain.tenantadmininvitation.model.TenantInvitationWithStatus;
import io.github.kizulog_community.kizulog.domain.tenantadmininvitation.port.TenantAdminInvitationRepository;
import io.github.kizulog_community.kizulog.domain.tenantadmininvitation.port.TenantAdminInvitationStatusRepository;
import lombok.RequiredArgsConstructor;

/**
 * テナント管理者招待サービス
 *
 * @author Jun Kobayashi
 */
@Service
@RequiredArgsConstructor
public class TenantAdminInvitationService {

    /** トークン乱数のバイト数（256bit） */
    private static final int TOKEN_RANDOM_BYTES = 32;

    /** 有効期間の最小値（時間） */
    private static final int DURATION_HOURS_MIN = 1;

    /** 有効期間の最大値（時間） */
    private static final int DURATION_HOURS_MAX = 720;

    /** 暗号学的乱数生成器 */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /** 招待リポジトリ */
    private final TenantAdminInvitationRepository invitationRepository;

    /** 招待ステータスリポジトリ */
    private final TenantAdminInvitationStatusRepository statusRepository;

    /**
     * 新規招待を発行する。
     *
     * @param tenantId 招待先テナントID（必須、空文字不可）
     * @param displayName 招待先表示名（必須、空文字不可）
     * @param durationHours 有効期間（時間、1〜720）
     * @param createdBy 発行者の識別子
     * @return 発行された招待情報（平文トークン含む）
     * @throws IllegalArgumentException パラメータが不正な場合
     */
    @Transactional
    public IssuedTenantInvitation issueInvitation(
            String tenantId, String displayName, int durationHours, String createdBy) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId must not be blank");
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("displayName must not be blank");
        }
        if (durationHours < DURATION_HOURS_MIN || durationHours > DURATION_HOURS_MAX) {
            throw new IllegalArgumentException(
                    "durationHours must be between " + DURATION_HOURS_MIN
                            + " and " + DURATION_HOURS_MAX);
        }
        if (createdBy == null || createdBy.isBlank()) {
            throw new IllegalArgumentException("createdBy must not be blank");
        }

        String invitationId = UUID.randomUUID().toString();
        String plainToken = generateSecureToken();
        String tokenHash = sha256Hex(plainToken);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime expiresAt = now.plusHours(durationHours);

        invitationRepository.save(new TenantAdminInvitation(
                invitationId, now, tenantId, tokenHash, expiresAt, displayName, now, createdBy));

        statusRepository.save(new TenantAdminInvitationStatus(
                invitationId, now, TenantInvitationStatusValue.PENDING, null, now, createdBy));

        return new IssuedTenantInvitation(invitationId, plainToken, expiresAt);
    }

    /**
     * 平文トークンから受諾可能な招待を取得する。
     *
     * @param plainToken 平文トークン
     * @return 受諾可能な招待（本体のみ、ステータスは PENDING であることが保証される）
     * @throws TenantInvitationException 受諾不可能な場合
     */
    @Transactional(readOnly = true)
    public TenantAdminInvitation findValidInvitationByToken(String plainToken) {
        if (plainToken == null || plainToken.isBlank()) {
            throw new TenantInvitationException(TenantInvitationError.INVALID_TOKEN);
        }

        String tokenHash = sha256Hex(plainToken);
        TenantAdminInvitation invitation = invitationRepository.findByTokenHash(tokenHash)
                .orElseThrow(() ->
                        new TenantInvitationException(TenantInvitationError.INVALID_TOKEN));

        // 最新バージョンの token_hash と一致するかを確認
        // （再発行で旧バージョンの token が無効化されているケース）
        TenantAdminInvitation latest = invitationRepository
                .findLatestByInvitationId(invitation.getInvitationId())
                .orElseThrow(() ->
                        new TenantInvitationException(TenantInvitationError.INVALID_TOKEN));
        if (!latest.getTokenHash().equals(tokenHash)) {
            throw new TenantInvitationException(TenantInvitationError.INVALID_TOKEN);
        }

        // 有効期限切れ判定（アプリ層で動的判定）
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        if (!latest.getExpiresAt().isAfter(now)) {
            throw new TenantInvitationException(TenantInvitationError.EXPIRED);
        }

        // ステータス判定
        TenantAdminInvitationStatus status = statusRepository
                .findLatestByInvitationId(latest.getInvitationId())
                .orElseThrow(() ->
                        new TenantInvitationException(TenantInvitationError.INVALID_TOKEN));
        switch (status.getStatus()) {
            case USED:
                throw new TenantInvitationException(TenantInvitationError.ALREADY_USED);
            case CANCELLED:
                throw new TenantInvitationException(TenantInvitationError.CANCELLED);
            case PENDING:
                // ok
                break;
            default:
                throw new TenantInvitationException(TenantInvitationError.INVALID_TOKEN);
        }

        return latest;
    }

    /**
     * 招待を受諾完了 (USED) 状態に更新する。
     *
     * @param tenantId 招待が属するべきテナントID
     * @param invitationId 招待ID
     * @param createdBy 受諾者の account_id
     * @throws TenantInvitationException 招待が見つからない / テナント不一致 / 既に受諾済み
     */
    @Transactional
    public void markAsUsed(String tenantId, String invitationId, String createdBy) {
        verifyInvitationBelongsToTenant(tenantId, invitationId);

        TenantAdminInvitationStatus current = statusRepository
                .findLatestByInvitationId(invitationId)
                .orElseThrow(() ->
                        new TenantInvitationException(
                                TenantInvitationError.INVITATION_NOT_FOUND));

        if (current.getStatus() != TenantInvitationStatusValue.PENDING) {
            // 想定外: 受諾フローの直前に検証しているため通常起こらない
            // 同時アクセスで他者が先に受諾した場合のガード
            throw new TenantInvitationException(TenantInvitationError.ALREADY_USED);
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        statusRepository.save(new TenantAdminInvitationStatus(
                invitationId, now, TenantInvitationStatusValue.USED, null, now, createdBy));
    }

    /**
     * PENDING 状態の招待を取消 (CANCELLED 状態に更新する)
     *
     * @param tenantId 招待が属するべきテナントID
     * @param invitationId 招待ID
     * @param reason 取消理由（任意）
     * @param cancelledBy 取消操作した SYSTEM_ADMIN の account_id
     * @throws TenantInvitationException 招待が見つからない / テナント不一致 / 既に USED / 既に CANCELLED
     */
    @Transactional
    public void cancelInvitation(
            String tenantId, String invitationId, String reason, String cancelledBy) {
        verifyInvitationBelongsToTenant(tenantId, invitationId);

        TenantAdminInvitationStatus current = statusRepository
                .findLatestByInvitationId(invitationId)
                .orElseThrow(() ->
                        new TenantInvitationException(
                                TenantInvitationError.INVITATION_NOT_FOUND));

        switch (current.getStatus()) {
            case USED:
                throw new TenantInvitationException(
                        TenantInvitationError.ALREADY_USED_FOR_CANCEL);
            case CANCELLED:
                throw new TenantInvitationException(TenantInvitationError.ALREADY_CANCELLED);
            case PENDING:
                // ok
                break;
            default:
                throw new TenantInvitationException(
                        TenantInvitationError.INVITATION_NOT_FOUND);
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        statusRepository.save(new TenantAdminInvitationStatus(
                invitationId, now, TenantInvitationStatusValue.CANCELLED,
                reason, now, cancelledBy));
    }

    /**
     * 管理画面用に指定テナントの全招待をステータス付きで取得する。
     *
     * @param tenantId テナントID
     * @return 招待とステータスのペアのリスト
     */
    @Transactional(readOnly = true)
    public List<TenantInvitationWithStatus> listAllInvitations(String tenantId) {
        List<TenantAdminInvitation> invitations =
                invitationRepository.findAllLatestByTenantId(tenantId);
        Map<String, TenantAdminInvitationStatus> statusMap = invitations.stream()
                .map(i -> statusRepository.findLatestByInvitationId(i.getInvitationId())
                        .orElse(null))
                .filter(s -> s != null)
                .collect(Collectors.toMap(
                        TenantAdminInvitationStatus::getInvitationId, s -> s));

        return invitations.stream()
                .map(i -> new TenantInvitationWithStatus(i, statusMap.get(i.getInvitationId())))
                .sorted(Comparator
                        .<TenantInvitationWithStatus, Integer>comparing(iws ->
                                iws.getStatus() != null
                                        && iws.getStatus().getStatus()
                                                == TenantInvitationStatusValue.PENDING
                                        ? 0 : 1)
                        .thenComparing(iws -> iws.getInvitation().getCreatedAt(),
                                Comparator.reverseOrder()))
                .toList();
    }

    /**
     * 詳細画面用に1件の招待をステータス付きで取得する。
     *
     * @param tenantId テナントID
     * @param invitationId 招待ID
     * @return 招待とステータスのペア。該当しない場合は空
     */
    @Transactional(readOnly = true)
    public Optional<TenantInvitationWithStatus> findInvitationDetail(
            String tenantId, String invitationId) {
        Optional<TenantAdminInvitation> invitationOpt =
                invitationRepository.findLatestByInvitationId(invitationId);
        if (invitationOpt.isEmpty()) {
            return Optional.empty();
        }
        TenantAdminInvitation invitation = invitationOpt.get();
        // テナント越境チェック: 別テナントの招待は「存在しない」扱い
        if (!invitation.getTenantId().equals(tenantId)) {
            return Optional.empty();
        }
        TenantAdminInvitationStatus status = statusRepository
                .findLatestByInvitationId(invitationId).orElse(null);
        return Optional.of(new TenantInvitationWithStatus(invitation, status));
    }

    /**
     * 指定 invitationId の招待が指定 tenantId に属することを検証する。
     *
     * @param tenantId テナントID
     * @param invitationId 招待ID
     * @throws TenantInvitationException 招待が見つからない / テナント不一致
     */
    private void verifyInvitationBelongsToTenant(String tenantId, String invitationId) {
        TenantAdminInvitation invitation = invitationRepository
                .findLatestByInvitationId(invitationId)
                .orElseThrow(() ->
                        new TenantInvitationException(
                                TenantInvitationError.INVITATION_NOT_FOUND));
        if (!invitation.getTenantId().equals(tenantId)) {
            throw new TenantInvitationException(TenantInvitationError.INVITATION_NOT_FOUND);
        }
    }

    /**
     * 暗号学的に安全なトークンを生成する。
     */
    private String generateSecureToken() {
        byte[] randomBytes = new byte[TOKEN_RANDOM_BYTES];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    /**
     * 文字列の SHA-256 ハッシュを 16 進文字列で取得する。
     */
    private String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 は Java 標準で必ず利用可能
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

}
