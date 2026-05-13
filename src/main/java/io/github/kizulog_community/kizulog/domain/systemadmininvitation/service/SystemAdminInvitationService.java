package io.github.kizulog_community.kizulog.domain.systemadmininvitation.service;

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

import io.github.kizulog_community.kizulog.domain.systemadmininvitation.exception.InvitationError;
import io.github.kizulog_community.kizulog.domain.systemadmininvitation.exception.InvitationException;
import io.github.kizulog_community.kizulog.domain.systemadmininvitation.model.InvitationStatusValue;
import io.github.kizulog_community.kizulog.domain.systemadmininvitation.model.InvitationWithStatus;
import io.github.kizulog_community.kizulog.domain.systemadmininvitation.model.IssuedInvitation;
import io.github.kizulog_community.kizulog.domain.systemadmininvitation.model.SystemAdminInvitation;
import io.github.kizulog_community.kizulog.domain.systemadmininvitation.model.SystemAdminInvitationStatus;
import io.github.kizulog_community.kizulog.domain.systemadmininvitation.port.SystemAdminInvitationRepository;
import io.github.kizulog_community.kizulog.domain.systemadmininvitation.port.SystemAdminInvitationStatusRepository;
import lombok.RequiredArgsConstructor;

/**
 * システム管理者招待サービス
 *
 * <p>SYSTEM_ADMIN による招待発行・取消、招待先による受諾フロー、
 * 招待管理画面のための一覧/詳細取得を提供する。</p>
 *
 * <p>招待トークンは SecureRandom で 32 バイトの乱数を生成し、
 * Base64URL (RFC 4648 Section 5, パディングなし) でエンコードした
 * 43 文字の文字列。DB には SHA-256 ハッシュのみを保存し、平文は
 * 発行直後の戻り値でのみ取得可能。</p>
 *
 * @author Jun Kobayashi
 */
@Service
@RequiredArgsConstructor
public class SystemAdminInvitationService {

    /** トークン乱数のバイト数（256bit） */
    private static final int TOKEN_RANDOM_BYTES = 32;

    /** 有効期間の最小値（時間） */
    private static final int DURATION_HOURS_MIN = 1;

    /** 有効期間の最大値（時間） */
    private static final int DURATION_HOURS_MAX = 720;

    /** 暗号学的乱数生成器 */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /** 招待リポジトリ */
    private final SystemAdminInvitationRepository invitationRepository;

    /** 招待ステータスリポジトリ */
    private final SystemAdminInvitationStatusRepository statusRepository;

    /**
     * 新規招待を発行する。
     *
     * <p>本体テーブルに新バージョン、
     * ステータステーブルに PENDING を同一トランザクションで保存する。</p>
     *
     * <p>戻り値の IssuedInvitation には平文トークンが含まれる。
     * 平文は呼び出し側で URL 生成に利用した後、決して保持してはならない。
     * DB には SHA-256 ハッシュのみが保存されており、後から平文を取り出すことはできない。</p>
     *
     * @param displayName 招待先表示名（必須、空文字不可）
     * @param durationHours 有効期間（時間、1〜720）
     * @param createdBy 発行者の識別子
     * @return 発行された招待情報（平文トークン含む）
     * @throws IllegalArgumentException パラメータが不正な場合
     */
    @Transactional
    public IssuedInvitation issueInvitation(
            String displayName, int durationHours, String createdBy) {
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

        invitationRepository.save(new SystemAdminInvitation(
                invitationId, now, tokenHash, expiresAt, displayName, now, createdBy));

        statusRepository.save(new SystemAdminInvitationStatus(
                invitationId, now, InvitationStatusValue.PENDING, null, now, createdBy));

        return new IssuedInvitation(invitationId, plainToken, expiresAt);
    }

    /**
     * 平文トークンから受諾可能な招待を取得する。
     *
     * <p>以下のいずれかに該当する場合、該当する InvitationError で
     * InvitationException をスローする。</p>
     * <ul>
     *   <li>InvitationError#INVALID_TOKEN - 該当する招待が存在しない</li>
     *   <li>InvitationError#INVALID_TOKEN - 最新バージョンと異なるトークンが使われた</li>
     *   <li>InvitationError#EXPIRED - 有効期限切れ</li>
     *   <li>InvitationError#ALREADY_USED - 既に受諾済み</li>
     *   <li>InvitationError#CANCELLED - 取消済み</li>
     * </ul>
     *
     * @param plainToken 平文トークン
     * @return 受諾可能な招待（本体のみ、ステータスは PENDING であることが保証される）
     * @throws InvitationException 受諾不可能な場合
     */
    @Transactional(readOnly = true)
    public SystemAdminInvitation findValidInvitationByToken(String plainToken) {
        if (plainToken == null || plainToken.isBlank()) {
            throw new InvitationException(InvitationError.INVALID_TOKEN);
        }

        String tokenHash = sha256Hex(plainToken);
        SystemAdminInvitation invitation = invitationRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new InvitationException(InvitationError.INVALID_TOKEN));

        // 最新バージョンの token_hash と一致するかを確認
        // （再発行で旧バージョンの token が無効化されているケース）
        SystemAdminInvitation latest = invitationRepository
                .findLatestByInvitationId(invitation.getInvitationId())
                .orElseThrow(() -> new InvitationException(InvitationError.INVALID_TOKEN));
        if (!latest.getTokenHash().equals(tokenHash)) {
            throw new InvitationException(InvitationError.INVALID_TOKEN);
        }

        // 有効期限切れ判定（アプリ層で動的判定）
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        if (!latest.getExpiresAt().isAfter(now)) {
            throw new InvitationException(InvitationError.EXPIRED);
        }

        // ステータス判定
        SystemAdminInvitationStatus status = statusRepository
                .findLatestByInvitationId(latest.getInvitationId())
                .orElseThrow(() -> new InvitationException(InvitationError.INVALID_TOKEN));
        switch (status.getStatus()) {
            case USED:
                throw new InvitationException(InvitationError.ALREADY_USED);
            case CANCELLED:
                throw new InvitationException(InvitationError.CANCELLED);
            case PENDING:
                // ok
                break;
            default:
                throw new InvitationException(InvitationError.INVALID_TOKEN);
        }

        return latest;
    }

    /**
     * 招待を受諾完了 (USED) 状態に更新する。
     *
     * @param invitationId 招待ID
     * @param createdBy 受諾者の account_id
     * @throws InvitationException 招待が見つからない場合
     */
    @Transactional
    public void markAsUsed(String invitationId, String createdBy) {
        SystemAdminInvitationStatus current = statusRepository
                .findLatestByInvitationId(invitationId)
                .orElseThrow(() ->
                        new InvitationException(InvitationError.INVITATION_NOT_FOUND));

        if (current.getStatus() != InvitationStatusValue.PENDING) {
            // 想定外: 受諾フローの直前に検証しているため通常起こらない
            // 同時アクセスで他者が先に受諾した場合のガード
            throw new InvitationException(InvitationError.ALREADY_USED);
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        statusRepository.save(new SystemAdminInvitationStatus(
                invitationId, now, InvitationStatusValue.USED, null, now, createdBy));
    }

    /**
     * PENDING 状態の招待を取消 (CANCELLED 状態に更新する)
     *
     * @param invitationId 招待ID
     * @param reason 取消理由（任意）
     * @param cancelledBy 取消操作した SYSTEM_ADMIN の account_id
     * @throws InvitationException 招待が見つからない / 既に USED / 既に CANCELLED
     */
    @Transactional
    public void cancelInvitation(
            String invitationId, String reason, String cancelledBy) {
        SystemAdminInvitationStatus current = statusRepository
                .findLatestByInvitationId(invitationId)
                .orElseThrow(() ->
                        new InvitationException(InvitationError.INVITATION_NOT_FOUND));

        switch (current.getStatus()) {
            case USED:
                throw new InvitationException(InvitationError.ALREADY_USED_FOR_CANCEL);
            case CANCELLED:
                throw new InvitationException(InvitationError.ALREADY_CANCELLED);
            case PENDING:
                // ok
                break;
            default:
                throw new InvitationException(InvitationError.INVITATION_NOT_FOUND);
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        statusRepository.save(new SystemAdminInvitationStatus(
                invitationId, now, InvitationStatusValue.CANCELLED, reason, now, cancelledBy));
    }

    /**
     * 管理画面用に全招待をステータス付きで取得する。
     *
     * <p>並び順: PENDING を先頭、その他を後ろに。
     * 同一ステータス内では新しい順 (createdAt 降順)。</p>
     */
    @Transactional(readOnly = true)
    public List<InvitationWithStatus> listAllInvitations() {
        List<SystemAdminInvitation> invitations = invitationRepository.findAllLatest();
        Map<String, SystemAdminInvitationStatus> statusMap = invitations.stream()
                .map(i -> statusRepository.findLatestByInvitationId(i.getInvitationId())
                        .orElse(null))
                .filter(s -> s != null)
                .collect(Collectors.toMap(
                        SystemAdminInvitationStatus::getInvitationId, s -> s));

        return invitations.stream()
                .map(i -> new InvitationWithStatus(i, statusMap.get(i.getInvitationId())))
                .sorted(Comparator
                        .<InvitationWithStatus, Integer>comparing(iws ->
                                iws.getStatus() != null
                                        && iws.getStatus().getStatus()
                                                == InvitationStatusValue.PENDING
                                        ? 0 : 1)
                        .thenComparing(iws -> iws.getInvitation().getCreatedAt(),
                                Comparator.reverseOrder()))
                .toList();
    }

    /**
     * 詳細画面用に1件の招待をステータス付きで取得する。
     */
    @Transactional(readOnly = true)
    public Optional<InvitationWithStatus> findInvitationDetail(String invitationId) {
        Optional<SystemAdminInvitation> invitationOpt =
                invitationRepository.findLatestByInvitationId(invitationId);
        if (invitationOpt.isEmpty()) {
            return Optional.empty();
        }
        SystemAdminInvitationStatus status = statusRepository
                .findLatestByInvitationId(invitationId).orElse(null);
        return Optional.of(new InvitationWithStatus(invitationOpt.get(), status));
    }

    /**
     * 暗号学的に安全なトークンを生成する。
     *
     * <p>SecureRandom で 32 バイトの乱数を生成し、Base64URL (パディングなし)
     * でエンコードする。長さは 43 文字。</p>
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
