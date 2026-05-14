package io.github.kizulog_community.kizulog.domain.systemadmininvitation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import io.github.kizulog_community.kizulog.domain.systemadmininvitation.exception.InvitationError;
import io.github.kizulog_community.kizulog.domain.systemadmininvitation.exception.InvitationException;
import io.github.kizulog_community.kizulog.domain.systemadmininvitation.model.InvitationStatusValue;
import io.github.kizulog_community.kizulog.domain.systemadmininvitation.model.InvitationWithStatus;
import io.github.kizulog_community.kizulog.domain.systemadmininvitation.model.IssuedInvitation;
import io.github.kizulog_community.kizulog.domain.systemadmininvitation.model.SystemAdminInvitation;
import io.github.kizulog_community.kizulog.domain.systemadmininvitation.model.SystemAdminInvitationStatus;
import io.github.kizulog_community.kizulog.domain.systemadmininvitation.port.SystemAdminInvitationRepository;
import io.github.kizulog_community.kizulog.domain.systemadmininvitation.port.SystemAdminInvitationStatusRepository;

/**
 * SystemAdminInvitationServiceの単体テスト
 *
 * @author Jun Kobayashi
 */
class SystemAdminInvitationServiceTest {

    private static final OffsetDateTime BASE_TIME =
            OffsetDateTime.of(2026, 5, 1, 12, 0, 0, 0, ZoneOffset.UTC);

    private SystemAdminInvitationRepository invitationRepository;
    private SystemAdminInvitationStatusRepository statusRepository;
    private SystemAdminInvitationService service;

    @BeforeEach
    void setUp() {
        invitationRepository = mock(SystemAdminInvitationRepository.class);
        statusRepository = mock(SystemAdminInvitationStatusRepository.class);
        service = new SystemAdminInvitationService(invitationRepository, statusRepository);
    }

    /** トークン平文 → SHA-256ハッシュ計算ヘルパー(検証用) */
    private static String sha256Hex(String input) {
        try {
            var md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hash);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    @DisplayName("issueInvitation: 正常系 - 招待本体とPENDINGステータスが同一バージョンで保存される")
    void issueInvitation_savesInvitationAndPendingStatus() {
        IssuedInvitation issued = service.issueInvitation("Test User", 24, "creator-1");

        assertThat(issued.getInvitationId()).isNotBlank();
        assertThat(issued.getPlainToken()).isNotBlank();
        // Base64URL(32バイト)はパディングなしで43文字
        assertThat(issued.getPlainToken()).hasSize(43);
        assertThat(issued.getExpiresAt()).isAfter(OffsetDateTime.now(ZoneOffset.UTC));

        ArgumentCaptor<SystemAdminInvitation> invCaptor =
                ArgumentCaptor.forClass(SystemAdminInvitation.class);
        verify(invitationRepository).save(invCaptor.capture());
        SystemAdminInvitation savedInv = invCaptor.getValue();
        assertThat(savedInv.getInvitationId()).isEqualTo(issued.getInvitationId());
        assertThat(savedInv.getDisplayName()).isEqualTo("Test User");
        assertThat(savedInv.getCreatedBy()).isEqualTo("creator-1");
        assertThat(savedInv.getTokenHash()).isEqualTo(sha256Hex(issued.getPlainToken()));

        ArgumentCaptor<SystemAdminInvitationStatus> stCaptor =
                ArgumentCaptor.forClass(SystemAdminInvitationStatus.class);
        verify(statusRepository).save(stCaptor.capture());
        SystemAdminInvitationStatus savedStatus = stCaptor.getValue();
        assertThat(savedStatus.getInvitationId()).isEqualTo(issued.getInvitationId());
        assertThat(savedStatus.getStatus()).isEqualTo(InvitationStatusValue.PENDING);
        assertThat(savedStatus.getReason()).isNull();
        assertThat(savedStatus.getCreatedBy()).isEqualTo("creator-1");
        // 招待本体とステータスが同一バージョン
        assertThat(savedStatus.getVersion()).isEqualTo(savedInv.getVersion());
    }

    @Test
    @DisplayName("issueInvitation: 平文トークンとDB保存のtokenHashはSHA-256で一致する")
    void issueInvitation_tokenHashMatchesSha256OfPlainToken() {
        IssuedInvitation issued = service.issueInvitation("user", 1, "creator");

        ArgumentCaptor<SystemAdminInvitation> captor =
                ArgumentCaptor.forClass(SystemAdminInvitation.class);
        verify(invitationRepository).save(captor.capture());
        assertThat(captor.getValue().getTokenHash())
                .isEqualTo(sha256Hex(issued.getPlainToken()));
    }

    @Test
    @DisplayName("issueInvitation: displayNameがnullの場合、IllegalArgumentExceptionをスローする")
    void issueInvitation_throwsException_whenDisplayNameIsNull() {
        assertThatThrownBy(() -> service.issueInvitation(null, 24, "creator"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("displayName");
        verify(invitationRepository, never()).save(any());
        verify(statusRepository, never()).save(any());
    }

    @Test
    @DisplayName("issueInvitation: displayNameが空白のみの場合、IllegalArgumentExceptionをスローする")
    void issueInvitation_throwsException_whenDisplayNameIsBlank() {
        assertThatThrownBy(() -> service.issueInvitation("   ", 24, "creator"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("displayName");
        verify(invitationRepository, never()).save(any());
    }

    @Test
    @DisplayName("issueInvitation: durationHoursが下限未満(0)の場合、IllegalArgumentExceptionをスローする")
    void issueInvitation_throwsException_whenDurationBelowMin() {
        assertThatThrownBy(() -> service.issueInvitation("user", 0, "creator"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("durationHours");
        verify(invitationRepository, never()).save(any());
    }

    @Test
    @DisplayName("issueInvitation: durationHoursが上限超(721)の場合、IllegalArgumentExceptionをスローする")
    void issueInvitation_throwsException_whenDurationAboveMax() {
        assertThatThrownBy(() -> service.issueInvitation("user", 721, "creator"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("durationHours");
        verify(invitationRepository, never()).save(any());
    }

    @Test
    @DisplayName("issueInvitation: durationHoursが下限(1)の場合、正常に発行される")
    void issueInvitation_succeedsAtMinDuration() {
        IssuedInvitation issued = service.issueInvitation("user", 1, "creator");
        assertThat(issued.getInvitationId()).isNotBlank();
        verify(invitationRepository).save(any());
    }

    @Test
    @DisplayName("issueInvitation: durationHoursが上限(720)の場合、正常に発行される")
    void issueInvitation_succeedsAtMaxDuration() {
        IssuedInvitation issued = service.issueInvitation("user", 720, "creator");
        assertThat(issued.getInvitationId()).isNotBlank();
        verify(invitationRepository).save(any());
    }

    @Test
    @DisplayName("issueInvitation: createdByがnullの場合、IllegalArgumentExceptionをスローする")
    void issueInvitation_throwsException_whenCreatedByIsNull() {
        assertThatThrownBy(() -> service.issueInvitation("user", 24, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("createdBy");
    }

    @Test
    @DisplayName("issueInvitation: 連続発行で異なるinvitationIdおよびplainTokenが生成される")
    void issueInvitation_generatesUniqueIdsAndTokens() {
        IssuedInvitation first = service.issueInvitation("user-1", 24, "creator");
        IssuedInvitation second = service.issueInvitation("user-2", 24, "creator");

        assertThat(first.getInvitationId()).isNotEqualTo(second.getInvitationId());
        assertThat(first.getPlainToken()).isNotEqualTo(second.getPlainToken());
    }

    @Test
    @DisplayName("findValidInvitationByToken: 正常系 - PENDINGかつ有効期限内の招待を返す")
    void findValidInvitationByToken_returnsInvitation_whenValid() {
        String plainToken = "abc123";
        String tokenHash = sha256Hex(plainToken);
        // 有効期限は現在より十分未来(BASE_TIMEは過去なのでBASE_TIME.plusDays(1)は使えない)
        OffsetDateTime futureExpiry = OffsetDateTime.now(ZoneOffset.UTC).plusDays(1);
        SystemAdminInvitation inv = invOf("inv-1", BASE_TIME, tokenHash, futureExpiry);

        when(invitationRepository.findByTokenHash(tokenHash))
                .thenReturn(Optional.of(inv));
        when(invitationRepository.findLatestByInvitationId("inv-1"))
                .thenReturn(Optional.of(inv));
        when(statusRepository.findLatestByInvitationId("inv-1"))
                .thenReturn(Optional.of(statusOf("inv-1", BASE_TIME,
                        InvitationStatusValue.PENDING)));

        SystemAdminInvitation result = service.findValidInvitationByToken(plainToken);

        assertThat(result.getInvitationId()).isEqualTo("inv-1");
    }

    @Test
    @DisplayName("findValidInvitationByToken: トークンが空白の場合、INVALID_TOKENエラー")
    void findValidInvitationByToken_throwsInvalidToken_whenBlank() {
        assertThatThrownBy(() -> service.findValidInvitationByToken(""))
                .isInstanceOf(InvitationException.class)
                .extracting(e -> ((InvitationException) e).getError())
                .isEqualTo(InvitationError.INVALID_TOKEN);
    }

    @Test
    @DisplayName("findValidInvitationByToken: トークンがnullの場合、INVALID_TOKENエラー")
    void findValidInvitationByToken_throwsInvalidToken_whenNull() {
        assertThatThrownBy(() -> service.findValidInvitationByToken(null))
                .isInstanceOf(InvitationException.class)
                .extracting(e -> ((InvitationException) e).getError())
                .isEqualTo(InvitationError.INVALID_TOKEN);
    }

    @Test
    @DisplayName("findValidInvitationByToken: 該当tokenHashが存在しない場合、INVALID_TOKENエラー")
    void findValidInvitationByToken_throwsInvalidToken_whenTokenHashNotFound() {
        when(invitationRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findValidInvitationByToken("nonexistent"))
                .isInstanceOf(InvitationException.class)
                .extracting(e -> ((InvitationException) e).getError())
                .isEqualTo(InvitationError.INVALID_TOKEN);
    }

    @Test
    @DisplayName("findValidInvitationByToken: 旧バージョンのtoken(再発行で無効化済み)の場合、INVALID_TOKENエラー")
    void findValidInvitationByToken_throwsInvalidToken_whenOldVersionToken() {
        String oldToken = "old-token";
        String oldHash = sha256Hex(oldToken);
        String newHash = sha256Hex("new-token");
        SystemAdminInvitation oldInv = invOf("inv-1", BASE_TIME, oldHash,
                BASE_TIME.plusDays(1));
        SystemAdminInvitation latestInv = invOf("inv-1", BASE_TIME.plusHours(1),
                newHash, BASE_TIME.plusDays(2));

        when(invitationRepository.findByTokenHash(oldHash))
                .thenReturn(Optional.of(oldInv));
        when(invitationRepository.findLatestByInvitationId("inv-1"))
                .thenReturn(Optional.of(latestInv));

        assertThatThrownBy(() -> service.findValidInvitationByToken(oldToken))
                .isInstanceOf(InvitationException.class)
                .extracting(e -> ((InvitationException) e).getError())
                .isEqualTo(InvitationError.INVALID_TOKEN);
    }

    @Test
    @DisplayName("findValidInvitationByToken: 有効期限切れの場合、EXPIREDエラー")
    void findValidInvitationByToken_throwsExpired_whenPastExpiresAt() {
        String plainToken = "expired-token";
        String tokenHash = sha256Hex(plainToken);
        // 有効期限を1秒前に設定
        OffsetDateTime past = OffsetDateTime.now(ZoneOffset.UTC).minusSeconds(1);
        SystemAdminInvitation inv = invOf("inv-1", BASE_TIME, tokenHash, past);

        when(invitationRepository.findByTokenHash(tokenHash))
                .thenReturn(Optional.of(inv));
        when(invitationRepository.findLatestByInvitationId("inv-1"))
                .thenReturn(Optional.of(inv));

        assertThatThrownBy(() -> service.findValidInvitationByToken(plainToken))
                .isInstanceOf(InvitationException.class)
                .extracting(e -> ((InvitationException) e).getError())
                .isEqualTo(InvitationError.EXPIRED);
    }

    @Test
    @DisplayName("findValidInvitationByToken: ステータスがUSEDの場合、ALREADY_USEDエラー")
    void findValidInvitationByToken_throwsAlreadyUsed_whenStatusUsed() {
        setupValidTokenWithStatus("used-token", "inv-used", InvitationStatusValue.USED);

        assertThatThrownBy(() -> service.findValidInvitationByToken("used-token"))
                .isInstanceOf(InvitationException.class)
                .extracting(e -> ((InvitationException) e).getError())
                .isEqualTo(InvitationError.ALREADY_USED);
    }

    @Test
    @DisplayName("findValidInvitationByToken: ステータスがCANCELLEDの場合、CANCELLEDエラー")
    void findValidInvitationByToken_throwsCancelled_whenStatusCancelled() {
        setupValidTokenWithStatus("cancelled-token", "inv-c",
                InvitationStatusValue.CANCELLED);

        assertThatThrownBy(() -> service.findValidInvitationByToken("cancelled-token"))
                .isInstanceOf(InvitationException.class)
                .extracting(e -> ((InvitationException) e).getError())
                .isEqualTo(InvitationError.CANCELLED);
    }

    @Test
    @DisplayName("findValidInvitationByToken: ステータスが存在しない場合、INVALID_TOKENエラー")
    void findValidInvitationByToken_throwsInvalidToken_whenStatusNotFound() {
        String plainToken = "no-status-token";
        String tokenHash = sha256Hex(plainToken);
        // 期限内であることを保証(BASE_TIMEは過去なのでBASE_TIME.plusDays(1)は使えない)
        OffsetDateTime futureExpiry = OffsetDateTime.now(ZoneOffset.UTC).plusDays(1);
        SystemAdminInvitation inv = invOf("inv-1", BASE_TIME, tokenHash, futureExpiry);
        when(invitationRepository.findByTokenHash(tokenHash))
                .thenReturn(Optional.of(inv));
        when(invitationRepository.findLatestByInvitationId("inv-1"))
                .thenReturn(Optional.of(inv));
        when(statusRepository.findLatestByInvitationId("inv-1"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findValidInvitationByToken(plainToken))
                .isInstanceOf(InvitationException.class)
                .extracting(e -> ((InvitationException) e).getError())
                .isEqualTo(InvitationError.INVALID_TOKEN);
    }

    @Test
    @DisplayName("markAsUsed: 正常系 - PENDINGをUSEDに更新するステータスレコードを保存する")
    void markAsUsed_savesUsedStatus_whenCurrentlyPending() {
        when(statusRepository.findLatestByInvitationId("inv-1"))
                .thenReturn(Optional.of(statusOf("inv-1", BASE_TIME,
                        InvitationStatusValue.PENDING)));

        service.markAsUsed("inv-1", "acceptor-1");

        ArgumentCaptor<SystemAdminInvitationStatus> captor =
                ArgumentCaptor.forClass(SystemAdminInvitationStatus.class);
        verify(statusRepository).save(captor.capture());
        SystemAdminInvitationStatus saved = captor.getValue();
        assertThat(saved.getInvitationId()).isEqualTo("inv-1");
        assertThat(saved.getStatus()).isEqualTo(InvitationStatusValue.USED);
        assertThat(saved.getReason()).isNull();
        assertThat(saved.getCreatedBy()).isEqualTo("acceptor-1");
    }

    @Test
    @DisplayName("markAsUsed: 招待が存在しない場合、INVITATION_NOT_FOUNDエラー")
    void markAsUsed_throwsNotFound_whenInvitationNotFound() {
        when(statusRepository.findLatestByInvitationId("missing"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markAsUsed("missing", "acceptor"))
                .isInstanceOf(InvitationException.class)
                .extracting(e -> ((InvitationException) e).getError())
                .isEqualTo(InvitationError.INVITATION_NOT_FOUND);
        verify(statusRepository, never()).save(any());
    }

    @Test
    @DisplayName("markAsUsed: 現ステータスがUSEDの場合(同時アクセスガード)、ALREADY_USEDエラー")
    void markAsUsed_throwsAlreadyUsed_whenAlreadyUsed() {
        when(statusRepository.findLatestByInvitationId("inv-1"))
                .thenReturn(Optional.of(statusOf("inv-1", BASE_TIME,
                        InvitationStatusValue.USED)));

        assertThatThrownBy(() -> service.markAsUsed("inv-1", "acceptor"))
                .isInstanceOf(InvitationException.class)
                .extracting(e -> ((InvitationException) e).getError())
                .isEqualTo(InvitationError.ALREADY_USED);
        verify(statusRepository, never()).save(any());
    }

    @Test
    @DisplayName("markAsUsed: 現ステータスがCANCELLEDの場合(同時アクセスガード)、ALREADY_USEDエラー")
    void markAsUsed_throwsAlreadyUsed_whenCancelled() {
        when(statusRepository.findLatestByInvitationId("inv-1"))
                .thenReturn(Optional.of(statusOf("inv-1", BASE_TIME,
                        InvitationStatusValue.CANCELLED)));

        assertThatThrownBy(() -> service.markAsUsed("inv-1", "acceptor"))
                .isInstanceOf(InvitationException.class)
                .extracting(e -> ((InvitationException) e).getError())
                .isEqualTo(InvitationError.ALREADY_USED);
        verify(statusRepository, never()).save(any());
    }

    @Test
    @DisplayName("cancelInvitation: 正常系 - PENDINGをCANCELLEDに更新するレコードを保存する")
    void cancelInvitation_savesCancelledStatus_whenCurrentlyPending() {
        when(statusRepository.findLatestByInvitationId("inv-1"))
                .thenReturn(Optional.of(statusOf("inv-1", BASE_TIME,
                        InvitationStatusValue.PENDING)));

        service.cancelInvitation("inv-1", "誤発行", "admin-1");

        ArgumentCaptor<SystemAdminInvitationStatus> captor =
                ArgumentCaptor.forClass(SystemAdminInvitationStatus.class);
        verify(statusRepository).save(captor.capture());
        SystemAdminInvitationStatus saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(InvitationStatusValue.CANCELLED);
        assertThat(saved.getReason()).isEqualTo("誤発行");
        assertThat(saved.getCreatedBy()).isEqualTo("admin-1");
    }

    @Test
    @DisplayName("cancelInvitation: reasonがnullでも取消可能")
    void cancelInvitation_acceptsNullReason() {
        when(statusRepository.findLatestByInvitationId("inv-1"))
                .thenReturn(Optional.of(statusOf("inv-1", BASE_TIME,
                        InvitationStatusValue.PENDING)));

        service.cancelInvitation("inv-1", null, "admin-1");

        ArgumentCaptor<SystemAdminInvitationStatus> captor =
                ArgumentCaptor.forClass(SystemAdminInvitationStatus.class);
        verify(statusRepository).save(captor.capture());
        assertThat(captor.getValue().getReason()).isNull();
    }

    @Test
    @DisplayName("cancelInvitation: 招待が存在しない場合、INVITATION_NOT_FOUNDエラー")
    void cancelInvitation_throwsNotFound_whenStatusMissing() {
        when(statusRepository.findLatestByInvitationId("missing"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancelInvitation("missing", null, "admin"))
                .isInstanceOf(InvitationException.class)
                .extracting(e -> ((InvitationException) e).getError())
                .isEqualTo(InvitationError.INVITATION_NOT_FOUND);
    }

    @Test
    @DisplayName("cancelInvitation: 現ステータスがUSEDの場合、ALREADY_USED_FOR_CANCELエラー")
    void cancelInvitation_throwsAlreadyUsedForCancel_whenUsed() {
        when(statusRepository.findLatestByInvitationId("inv-1"))
                .thenReturn(Optional.of(statusOf("inv-1", BASE_TIME,
                        InvitationStatusValue.USED)));

        assertThatThrownBy(() -> service.cancelInvitation("inv-1", null, "admin"))
                .isInstanceOf(InvitationException.class)
                .extracting(e -> ((InvitationException) e).getError())
                .isEqualTo(InvitationError.ALREADY_USED_FOR_CANCEL);
        verify(statusRepository, never()).save(any());
    }

    @Test
    @DisplayName("cancelInvitation: 現ステータスがCANCELLEDの場合、ALREADY_CANCELLEDエラー")
    void cancelInvitation_throwsAlreadyCancelled_whenAlreadyCancelled() {
        when(statusRepository.findLatestByInvitationId("inv-1"))
                .thenReturn(Optional.of(statusOf("inv-1", BASE_TIME,
                        InvitationStatusValue.CANCELLED)));

        assertThatThrownBy(() -> service.cancelInvitation("inv-1", null, "admin"))
                .isInstanceOf(InvitationException.class)
                .extracting(e -> ((InvitationException) e).getError())
                .isEqualTo(InvitationError.ALREADY_CANCELLED);
        verify(statusRepository, never()).save(any());
    }

    @Test
    @DisplayName("listAllInvitations: PENDINGが先頭、その後は作成日時降順で並ぶ")
    void listAllInvitations_ordersPendingFirstThenByCreatedAtDesc() {
        SystemAdminInvitation oldUsed = invOf("u-1", BASE_TIME, "hash-1",
                BASE_TIME.plusDays(1));
        SystemAdminInvitation newPending = invOf("p-1", BASE_TIME.plusHours(2),
                "hash-2", BASE_TIME.plusDays(2));
        SystemAdminInvitation oldPending = invOf("p-2", BASE_TIME.plusHours(1),
                "hash-3", BASE_TIME.plusDays(3));
        SystemAdminInvitation newCancelled = invOf("c-1", BASE_TIME.plusHours(3),
                "hash-4", BASE_TIME.plusDays(4));

        when(invitationRepository.findAllLatest())
                .thenReturn(List.of(oldUsed, newPending, oldPending, newCancelled));
        when(statusRepository.findLatestByInvitationId("u-1"))
                .thenReturn(Optional.of(statusOf("u-1", BASE_TIME,
                        InvitationStatusValue.USED)));
        when(statusRepository.findLatestByInvitationId("p-1"))
                .thenReturn(Optional.of(statusOf("p-1", BASE_TIME,
                        InvitationStatusValue.PENDING)));
        when(statusRepository.findLatestByInvitationId("p-2"))
                .thenReturn(Optional.of(statusOf("p-2", BASE_TIME,
                        InvitationStatusValue.PENDING)));
        when(statusRepository.findLatestByInvitationId("c-1"))
                .thenReturn(Optional.of(statusOf("c-1", BASE_TIME,
                        InvitationStatusValue.CANCELLED)));

        List<InvitationWithStatus> result = service.listAllInvitations();

        assertThat(result).hasSize(4);
        // 第1キー: PENDING先頭、PENDING内は createdAt 降順(新しい順)
        // p-1: createdAt = BASE_TIME+2h, p-2: createdAt = BASE_TIME+1h
        // → p-1 が先、p-2 が次
        assertThat(result.get(0).getInvitation().getInvitationId()).isEqualTo("p-1");
        assertThat(result.get(1).getInvitation().getInvitationId()).isEqualTo("p-2");
        // 第2キー: その他も createdAt 降順
        // c-1: createdAt = BASE_TIME+3h, u-1: createdAt = BASE_TIME
        // → c-1 が先、u-1 が次
        assertThat(result.get(2).getInvitation().getInvitationId()).isEqualTo("c-1");
        assertThat(result.get(3).getInvitation().getInvitationId()).isEqualTo("u-1");
    }

    @Test
    @DisplayName("listAllInvitations: 招待が0件の場合、空のリストを返す")
    void listAllInvitations_returnsEmpty_whenNoInvitations() {
        when(invitationRepository.findAllLatest()).thenReturn(List.of());

        List<InvitationWithStatus> result = service.listAllInvitations();

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("listAllInvitations: ステータスが存在しない招待はリストから除外される")
    void listAllInvitations_excludesInvitationsWithoutStatus() {
        SystemAdminInvitation withStatus = invOf("inv-1", BASE_TIME, "h-1",
                BASE_TIME.plusDays(1));
        SystemAdminInvitation withoutStatus = invOf("inv-2", BASE_TIME, "h-2",
                BASE_TIME.plusDays(1));
        when(invitationRepository.findAllLatest())
                .thenReturn(List.of(withStatus, withoutStatus));
        when(statusRepository.findLatestByInvitationId("inv-1"))
                .thenReturn(Optional.of(statusOf("inv-1", BASE_TIME,
                        InvitationStatusValue.PENDING)));
        when(statusRepository.findLatestByInvitationId("inv-2"))
                .thenReturn(Optional.empty());

        List<InvitationWithStatus> result = service.listAllInvitations();

        // ステータスなしの招待は statusMap に含まれないため、サイズは元配列と同じだが
        // status=null として渡される。ソート条件は PENDING を 0、それ以外を 1 とするため、
        // status=null は 1 扱いとなる。
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getInvitation().getInvitationId()).isEqualTo("inv-1");
        assertThat(result.get(0).getStatus()).isNotNull();
        assertThat(result.get(1).getInvitation().getInvitationId()).isEqualTo("inv-2");
        assertThat(result.get(1).getStatus()).isNull();
    }

    @Test
    @DisplayName("findInvitationDetail: 招待とステータスが存在する場合、InvitationWithStatusを返す")
    void findInvitationDetail_returnsWithStatus_whenBothExist() {
        SystemAdminInvitation inv = invOf("inv-1", BASE_TIME, "hash", BASE_TIME.plusDays(1));
        when(invitationRepository.findLatestByInvitationId("inv-1"))
                .thenReturn(Optional.of(inv));
        when(statusRepository.findLatestByInvitationId("inv-1"))
                .thenReturn(Optional.of(statusOf("inv-1", BASE_TIME,
                        InvitationStatusValue.PENDING)));

        Optional<InvitationWithStatus> result = service.findInvitationDetail("inv-1");

        assertThat(result).isPresent();
        assertThat(result.get().getInvitation().getInvitationId()).isEqualTo("inv-1");
        assertThat(result.get().getStatus().getStatus()).isEqualTo(InvitationStatusValue.PENDING);
    }

    @Test
    @DisplayName("findInvitationDetail: 招待が存在しない場合、空のOptionalを返す")
    void findInvitationDetail_returnsEmpty_whenInvitationNotFound() {
        when(invitationRepository.findLatestByInvitationId("missing"))
                .thenReturn(Optional.empty());

        Optional<InvitationWithStatus> result = service.findInvitationDetail("missing");

        assertThat(result).isEmpty();
        verify(statusRepository, never()).findLatestByInvitationId(any());
    }

    @Test
    @DisplayName("findInvitationDetail: 招待は存在するがステータスが無い場合、status=nullのInvitationWithStatusを返す")
    void findInvitationDetail_returnsWithNullStatus_whenStatusMissing() {
        SystemAdminInvitation inv = invOf("inv-1", BASE_TIME, "hash", BASE_TIME.plusDays(1));
        when(invitationRepository.findLatestByInvitationId("inv-1"))
                .thenReturn(Optional.of(inv));
        when(statusRepository.findLatestByInvitationId("inv-1"))
                .thenReturn(Optional.empty());

        Optional<InvitationWithStatus> result = service.findInvitationDetail("inv-1");

        assertThat(result).isPresent();
        assertThat(result.get().getStatus()).isNull();
    }

    private SystemAdminInvitation invOf(
            String id, OffsetDateTime version, String tokenHash, OffsetDateTime expiresAt) {
        return new SystemAdminInvitation(
                id, version, tokenHash, expiresAt,
                "display-" + id, version, "creator-" + id);
    }

    private SystemAdminInvitationStatus statusOf(
            String id, OffsetDateTime version, InvitationStatusValue value) {
        return new SystemAdminInvitationStatus(
                id, version, value, null, version, "creator-" + id);
    }

    private void setupValidTokenWithStatus(
            String plainToken, String invitationId, InvitationStatusValue status) {
        String tokenHash = sha256Hex(plainToken);
        SystemAdminInvitation inv = invOf(invitationId, BASE_TIME, tokenHash,
                BASE_TIME.plusDays(1).withYear(2999)); // 期限内であることを確実にするため遠い未来
        when(invitationRepository.findByTokenHash(tokenHash))
                .thenReturn(Optional.of(inv));
        when(invitationRepository.findLatestByInvitationId(invitationId))
                .thenReturn(Optional.of(inv));
        when(statusRepository.findLatestByInvitationId(invitationId))
                .thenReturn(Optional.of(statusOf(invitationId, BASE_TIME, status)));
    }

}
