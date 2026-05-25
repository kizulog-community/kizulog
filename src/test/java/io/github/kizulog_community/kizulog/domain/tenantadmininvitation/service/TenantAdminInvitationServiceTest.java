package io.github.kizulog_community.kizulog.domain.tenantadmininvitation.service;

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

import io.github.kizulog_community.kizulog.domain.tenantadmininvitation.exception.TenantInvitationError;
import io.github.kizulog_community.kizulog.domain.tenantadmininvitation.exception.TenantInvitationException;
import io.github.kizulog_community.kizulog.domain.tenantadmininvitation.model.IssuedTenantInvitation;
import io.github.kizulog_community.kizulog.domain.tenantadmininvitation.model.TenantAdminInvitation;
import io.github.kizulog_community.kizulog.domain.tenantadmininvitation.model.TenantAdminInvitationStatus;
import io.github.kizulog_community.kizulog.domain.tenantadmininvitation.model.TenantInvitationStatusValue;
import io.github.kizulog_community.kizulog.domain.tenantadmininvitation.model.TenantInvitationWithStatus;
import io.github.kizulog_community.kizulog.domain.tenantadmininvitation.port.TenantAdminInvitationRepository;
import io.github.kizulog_community.kizulog.domain.tenantadmininvitation.port.TenantAdminInvitationStatusRepository;

/**
 * TenantAdminInvitationService の単体テスト
 *
 * @author Jun Kobayashi
 */
class TenantAdminInvitationServiceTest {

    private static final OffsetDateTime BASE_TIME =
            OffsetDateTime.of(2026, 5, 1, 12, 0, 0, 0, ZoneOffset.UTC);

    private static final String TENANT_A = "tenant-A";
    private static final String TENANT_B = "tenant-B";

    private TenantAdminInvitationRepository invitationRepository;
    private TenantAdminInvitationStatusRepository statusRepository;
    private TenantAdminInvitationService service;

    @BeforeEach
    void setUp() {
        invitationRepository = mock(TenantAdminInvitationRepository.class);
        statusRepository = mock(TenantAdminInvitationStatusRepository.class);
        service = new TenantAdminInvitationService(invitationRepository, statusRepository);
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
    @DisplayName("issueInvitation: 正常系 - 招待本体(tenantId付き)とPENDINGステータスが同一バージョンで保存される")
    void issueInvitation_savesInvitationAndPendingStatus() {
        IssuedTenantInvitation issued =
                service.issueInvitation(TENANT_A, "Test User", 24, "creator-1");

        assertThat(issued.getInvitationId()).isNotBlank();
        assertThat(issued.getPlainToken()).isNotBlank();
        // Base64URL(32バイト)はパディングなしで43文字
        assertThat(issued.getPlainToken()).hasSize(43);
        assertThat(issued.getExpiresAt()).isAfter(OffsetDateTime.now(ZoneOffset.UTC));

        ArgumentCaptor<TenantAdminInvitation> invCaptor =
                ArgumentCaptor.forClass(TenantAdminInvitation.class);
        verify(invitationRepository).save(invCaptor.capture());
        TenantAdminInvitation savedInv = invCaptor.getValue();
        assertThat(savedInv.getInvitationId()).isEqualTo(issued.getInvitationId());
        assertThat(savedInv.getTenantId()).isEqualTo(TENANT_A);
        assertThat(savedInv.getDisplayName()).isEqualTo("Test User");
        assertThat(savedInv.getCreatedBy()).isEqualTo("creator-1");
        assertThat(savedInv.getTokenHash()).isEqualTo(sha256Hex(issued.getPlainToken()));

        ArgumentCaptor<TenantAdminInvitationStatus> stCaptor =
                ArgumentCaptor.forClass(TenantAdminInvitationStatus.class);
        verify(statusRepository).save(stCaptor.capture());
        TenantAdminInvitationStatus savedStatus = stCaptor.getValue();
        assertThat(savedStatus.getInvitationId()).isEqualTo(issued.getInvitationId());
        assertThat(savedStatus.getStatus()).isEqualTo(TenantInvitationStatusValue.PENDING);
        assertThat(savedStatus.getReason()).isNull();
        assertThat(savedStatus.getCreatedBy()).isEqualTo("creator-1");
        // 招待本体とステータスが同一バージョン
        assertThat(savedStatus.getVersion()).isEqualTo(savedInv.getVersion());
    }

    @Test
    @DisplayName("issueInvitation: 平文トークンとDB保存のtokenHashはSHA-256で一致する")
    void issueInvitation_tokenHashMatchesSha256OfPlainToken() {
        IssuedTenantInvitation issued =
                service.issueInvitation(TENANT_A, "user", 1, "creator");

        ArgumentCaptor<TenantAdminInvitation> captor =
                ArgumentCaptor.forClass(TenantAdminInvitation.class);
        verify(invitationRepository).save(captor.capture());
        assertThat(captor.getValue().getTokenHash())
                .isEqualTo(sha256Hex(issued.getPlainToken()));
    }

    @Test
    @DisplayName("issueInvitation: tenantIdがnullの場合、IllegalArgumentExceptionをスローする")
    void issueInvitation_throwsException_whenTenantIdIsNull() {
        assertThatThrownBy(() -> service.issueInvitation(null, "user", 24, "creator"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tenantId");
        verify(invitationRepository, never()).save(any());
        verify(statusRepository, never()).save(any());
    }

    @Test
    @DisplayName("issueInvitation: tenantIdが空白の場合、IllegalArgumentExceptionをスローする")
    void issueInvitation_throwsException_whenTenantIdIsBlank() {
        assertThatThrownBy(() -> service.issueInvitation("   ", "user", 24, "creator"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tenantId");
        verify(invitationRepository, never()).save(any());
    }

    @Test
    @DisplayName("issueInvitation: displayNameがnullの場合、IllegalArgumentExceptionをスローする")
    void issueInvitation_throwsException_whenDisplayNameIsNull() {
        assertThatThrownBy(() -> service.issueInvitation(TENANT_A, null, 24, "creator"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("displayName");
        verify(invitationRepository, never()).save(any());
        verify(statusRepository, never()).save(any());
    }

    @Test
    @DisplayName("issueInvitation: displayNameが空白のみの場合、IllegalArgumentExceptionをスローする")
    void issueInvitation_throwsException_whenDisplayNameIsBlank() {
        assertThatThrownBy(() -> service.issueInvitation(TENANT_A, "   ", 24, "creator"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("displayName");
        verify(invitationRepository, never()).save(any());
    }

    @Test
    @DisplayName("issueInvitation: durationHoursが下限未満(0)の場合、IllegalArgumentExceptionをスローする")
    void issueInvitation_throwsException_whenDurationBelowMin() {
        assertThatThrownBy(() -> service.issueInvitation(TENANT_A, "user", 0, "creator"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("durationHours");
        verify(invitationRepository, never()).save(any());
    }

    @Test
    @DisplayName("issueInvitation: durationHoursが上限超(721)の場合、IllegalArgumentExceptionをスローする")
    void issueInvitation_throwsException_whenDurationAboveMax() {
        assertThatThrownBy(() -> service.issueInvitation(TENANT_A, "user", 721, "creator"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("durationHours");
        verify(invitationRepository, never()).save(any());
    }

    @Test
    @DisplayName("issueInvitation: durationHoursが下限(1)の場合、正常に発行される")
    void issueInvitation_succeedsAtMinDuration() {
        IssuedTenantInvitation issued =
                service.issueInvitation(TENANT_A, "user", 1, "creator");
        assertThat(issued.getInvitationId()).isNotBlank();
        verify(invitationRepository).save(any());
    }

    @Test
    @DisplayName("issueInvitation: durationHoursが上限(720)の場合、正常に発行される")
    void issueInvitation_succeedsAtMaxDuration() {
        IssuedTenantInvitation issued =
                service.issueInvitation(TENANT_A, "user", 720, "creator");
        assertThat(issued.getInvitationId()).isNotBlank();
        verify(invitationRepository).save(any());
    }

    @Test
    @DisplayName("issueInvitation: createdByがnullの場合、IllegalArgumentExceptionをスローする")
    void issueInvitation_throwsException_whenCreatedByIsNull() {
        assertThatThrownBy(() -> service.issueInvitation(TENANT_A, "user", 24, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("createdBy");
    }

    @Test
    @DisplayName("issueInvitation: 連続発行で異なるinvitationIdおよびplainTokenが生成される")
    void issueInvitation_generatesUniqueIdsAndTokens() {
        IssuedTenantInvitation first =
                service.issueInvitation(TENANT_A, "user-1", 24, "creator");
        IssuedTenantInvitation second =
                service.issueInvitation(TENANT_A, "user-2", 24, "creator");

        assertThat(first.getInvitationId()).isNotEqualTo(second.getInvitationId());
        assertThat(first.getPlainToken()).isNotEqualTo(second.getPlainToken());
    }

    @Test
    @DisplayName("findValidInvitationByToken: 正常系 - PENDINGかつ有効期限内の招待(tenantId含む)を返す")
    void findValidInvitationByToken_returnsInvitation_whenValid() {
        String plainToken = "abc123";
        String tokenHash = sha256Hex(plainToken);
        OffsetDateTime futureExpiry = OffsetDateTime.now(ZoneOffset.UTC).plusDays(1);
        TenantAdminInvitation inv = invOf("inv-1", TENANT_A, BASE_TIME, tokenHash, futureExpiry);

        when(invitationRepository.findByTokenHash(tokenHash))
                .thenReturn(Optional.of(inv));
        when(invitationRepository.findLatestByInvitationId("inv-1"))
                .thenReturn(Optional.of(inv));
        when(statusRepository.findLatestByInvitationId("inv-1"))
                .thenReturn(Optional.of(statusOf("inv-1", BASE_TIME,
                        TenantInvitationStatusValue.PENDING)));

        TenantAdminInvitation result = service.findValidInvitationByToken(plainToken);

        assertThat(result.getInvitationId()).isEqualTo("inv-1");
        assertThat(result.getTenantId()).isEqualTo(TENANT_A);
    }

    @Test
    @DisplayName("findValidInvitationByToken: トークンが空白の場合、INVALID_TOKENエラー")
    void findValidInvitationByToken_throwsInvalidToken_whenBlank() {
        assertThatThrownBy(() -> service.findValidInvitationByToken(""))
                .isInstanceOf(TenantInvitationException.class)
                .extracting(e -> ((TenantInvitationException) e).getError())
                .isEqualTo(TenantInvitationError.INVALID_TOKEN);
    }

    @Test
    @DisplayName("findValidInvitationByToken: トークンがnullの場合、INVALID_TOKENエラー")
    void findValidInvitationByToken_throwsInvalidToken_whenNull() {
        assertThatThrownBy(() -> service.findValidInvitationByToken(null))
                .isInstanceOf(TenantInvitationException.class)
                .extracting(e -> ((TenantInvitationException) e).getError())
                .isEqualTo(TenantInvitationError.INVALID_TOKEN);
    }

    @Test
    @DisplayName("findValidInvitationByToken: 該当tokenHashが存在しない場合、INVALID_TOKENエラー")
    void findValidInvitationByToken_throwsInvalidToken_whenTokenHashNotFound() {
        when(invitationRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findValidInvitationByToken("nonexistent"))
                .isInstanceOf(TenantInvitationException.class)
                .extracting(e -> ((TenantInvitationException) e).getError())
                .isEqualTo(TenantInvitationError.INVALID_TOKEN);
    }

    @Test
    @DisplayName("findValidInvitationByToken: 旧バージョンのtoken(再発行で無効化済み)の場合、INVALID_TOKENエラー")
    void findValidInvitationByToken_throwsInvalidToken_whenOldVersionToken() {
        String oldToken = "old-token";
        String oldHash = sha256Hex(oldToken);
        String newHash = sha256Hex("new-token");
        TenantAdminInvitation oldInv = invOf("inv-1", TENANT_A, BASE_TIME, oldHash,
                BASE_TIME.plusDays(1));
        TenantAdminInvitation latestInv = invOf("inv-1", TENANT_A, BASE_TIME.plusHours(1),
                newHash, BASE_TIME.plusDays(2));

        when(invitationRepository.findByTokenHash(oldHash))
                .thenReturn(Optional.of(oldInv));
        when(invitationRepository.findLatestByInvitationId("inv-1"))
                .thenReturn(Optional.of(latestInv));

        assertThatThrownBy(() -> service.findValidInvitationByToken(oldToken))
                .isInstanceOf(TenantInvitationException.class)
                .extracting(e -> ((TenantInvitationException) e).getError())
                .isEqualTo(TenantInvitationError.INVALID_TOKEN);
    }

    @Test
    @DisplayName("findValidInvitationByToken: 有効期限切れの場合、EXPIREDエラー")
    void findValidInvitationByToken_throwsExpired_whenPastExpiresAt() {
        String plainToken = "expired-token";
        String tokenHash = sha256Hex(plainToken);
        OffsetDateTime past = OffsetDateTime.now(ZoneOffset.UTC).minusSeconds(1);
        TenantAdminInvitation inv = invOf("inv-1", TENANT_A, BASE_TIME, tokenHash, past);

        when(invitationRepository.findByTokenHash(tokenHash))
                .thenReturn(Optional.of(inv));
        when(invitationRepository.findLatestByInvitationId("inv-1"))
                .thenReturn(Optional.of(inv));

        assertThatThrownBy(() -> service.findValidInvitationByToken(plainToken))
                .isInstanceOf(TenantInvitationException.class)
                .extracting(e -> ((TenantInvitationException) e).getError())
                .isEqualTo(TenantInvitationError.EXPIRED);
    }

    @Test
    @DisplayName("findValidInvitationByToken: ステータスがUSEDの場合、ALREADY_USEDエラー")
    void findValidInvitationByToken_throwsAlreadyUsed_whenStatusUsed() {
        setupValidTokenWithStatus("used-token", "inv-used",
                TenantInvitationStatusValue.USED);

        assertThatThrownBy(() -> service.findValidInvitationByToken("used-token"))
                .isInstanceOf(TenantInvitationException.class)
                .extracting(e -> ((TenantInvitationException) e).getError())
                .isEqualTo(TenantInvitationError.ALREADY_USED);
    }

    @Test
    @DisplayName("findValidInvitationByToken: ステータスがCANCELLEDの場合、CANCELLEDエラー")
    void findValidInvitationByToken_throwsCancelled_whenStatusCancelled() {
        setupValidTokenWithStatus("cancelled-token", "inv-c",
                TenantInvitationStatusValue.CANCELLED);

        assertThatThrownBy(() -> service.findValidInvitationByToken("cancelled-token"))
                .isInstanceOf(TenantInvitationException.class)
                .extracting(e -> ((TenantInvitationException) e).getError())
                .isEqualTo(TenantInvitationError.CANCELLED);
    }

    @Test
    @DisplayName("findValidInvitationByToken: ステータスが存在しない場合、INVALID_TOKENエラー")
    void findValidInvitationByToken_throwsInvalidToken_whenStatusNotFound() {
        String plainToken = "no-status-token";
        String tokenHash = sha256Hex(plainToken);
        OffsetDateTime futureExpiry = OffsetDateTime.now(ZoneOffset.UTC).plusDays(1);
        TenantAdminInvitation inv = invOf("inv-1", TENANT_A, BASE_TIME, tokenHash, futureExpiry);
        when(invitationRepository.findByTokenHash(tokenHash))
                .thenReturn(Optional.of(inv));
        when(invitationRepository.findLatestByInvitationId("inv-1"))
                .thenReturn(Optional.of(inv));
        when(statusRepository.findLatestByInvitationId("inv-1"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findValidInvitationByToken(plainToken))
                .isInstanceOf(TenantInvitationException.class)
                .extracting(e -> ((TenantInvitationException) e).getError())
                .isEqualTo(TenantInvitationError.INVALID_TOKEN);
    }

    @Test
    @DisplayName("markAsUsed: 正常系 - PENDINGをUSEDに更新するステータスレコードを保存する")
    void markAsUsed_savesUsedStatus_whenCurrentlyPending() {
        when(invitationRepository.findLatestByInvitationId("inv-1"))
                .thenReturn(Optional.of(invOf("inv-1", TENANT_A, BASE_TIME,
                        "hash", BASE_TIME.plusDays(1))));
        when(statusRepository.findLatestByInvitationId("inv-1"))
                .thenReturn(Optional.of(statusOf("inv-1", BASE_TIME,
                        TenantInvitationStatusValue.PENDING)));

        service.markAsUsed(TENANT_A, "inv-1", "acceptor-1");

        ArgumentCaptor<TenantAdminInvitationStatus> captor =
                ArgumentCaptor.forClass(TenantAdminInvitationStatus.class);
        verify(statusRepository).save(captor.capture());
        TenantAdminInvitationStatus saved = captor.getValue();
        assertThat(saved.getInvitationId()).isEqualTo("inv-1");
        assertThat(saved.getStatus()).isEqualTo(TenantInvitationStatusValue.USED);
        assertThat(saved.getReason()).isNull();
        assertThat(saved.getCreatedBy()).isEqualTo("acceptor-1");
    }

    @Test
    @DisplayName("markAsUsed: 招待が存在しない場合、INVITATION_NOT_FOUNDエラー")
    void markAsUsed_throwsNotFound_whenInvitationNotFound() {
        when(invitationRepository.findLatestByInvitationId("missing"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markAsUsed(TENANT_A, "missing", "acceptor"))
                .isInstanceOf(TenantInvitationException.class)
                .extracting(e -> ((TenantInvitationException) e).getError())
                .isEqualTo(TenantInvitationError.INVITATION_NOT_FOUND);
        verify(statusRepository, never()).save(any());
    }

    @Test
    @DisplayName("markAsUsed: 別テナントの招待を指定した場合、INVITATION_NOT_FOUNDエラー（越境チェック）")
    void markAsUsed_throwsNotFound_whenTenantMismatch() {
        // 招待は tenant-B のものだが、tenant-A で操作しようとする
        when(invitationRepository.findLatestByInvitationId("inv-1"))
                .thenReturn(Optional.of(invOf("inv-1", TENANT_B, BASE_TIME,
                        "hash", BASE_TIME.plusDays(1))));

        assertThatThrownBy(() -> service.markAsUsed(TENANT_A, "inv-1", "acceptor"))
                .isInstanceOf(TenantInvitationException.class)
                .extracting(e -> ((TenantInvitationException) e).getError())
                .isEqualTo(TenantInvitationError.INVITATION_NOT_FOUND);
        verify(statusRepository, never()).save(any());
    }

    @Test
    @DisplayName("markAsUsed: 現ステータスがUSEDの場合(同時アクセスガード)、ALREADY_USEDエラー")
    void markAsUsed_throwsAlreadyUsed_whenAlreadyUsed() {
        when(invitationRepository.findLatestByInvitationId("inv-1"))
                .thenReturn(Optional.of(invOf("inv-1", TENANT_A, BASE_TIME,
                        "hash", BASE_TIME.plusDays(1))));
        when(statusRepository.findLatestByInvitationId("inv-1"))
                .thenReturn(Optional.of(statusOf("inv-1", BASE_TIME,
                        TenantInvitationStatusValue.USED)));

        assertThatThrownBy(() -> service.markAsUsed(TENANT_A, "inv-1", "acceptor"))
                .isInstanceOf(TenantInvitationException.class)
                .extracting(e -> ((TenantInvitationException) e).getError())
                .isEqualTo(TenantInvitationError.ALREADY_USED);
        verify(statusRepository, never()).save(any());
    }

    @Test
    @DisplayName("markAsUsed: 現ステータスがCANCELLEDの場合(同時アクセスガード)、ALREADY_USEDエラー")
    void markAsUsed_throwsAlreadyUsed_whenCancelled() {
        when(invitationRepository.findLatestByInvitationId("inv-1"))
                .thenReturn(Optional.of(invOf("inv-1", TENANT_A, BASE_TIME,
                        "hash", BASE_TIME.plusDays(1))));
        when(statusRepository.findLatestByInvitationId("inv-1"))
                .thenReturn(Optional.of(statusOf("inv-1", BASE_TIME,
                        TenantInvitationStatusValue.CANCELLED)));

        assertThatThrownBy(() -> service.markAsUsed(TENANT_A, "inv-1", "acceptor"))
                .isInstanceOf(TenantInvitationException.class)
                .extracting(e -> ((TenantInvitationException) e).getError())
                .isEqualTo(TenantInvitationError.ALREADY_USED);
        verify(statusRepository, never()).save(any());
    }

    @Test
    @DisplayName("cancelInvitation: 正常系 - PENDINGをCANCELLEDに更新するレコードを保存する")
    void cancelInvitation_savesCancelledStatus_whenCurrentlyPending() {
        when(invitationRepository.findLatestByInvitationId("inv-1"))
                .thenReturn(Optional.of(invOf("inv-1", TENANT_A, BASE_TIME,
                        "hash", BASE_TIME.plusDays(1))));
        when(statusRepository.findLatestByInvitationId("inv-1"))
                .thenReturn(Optional.of(statusOf("inv-1", BASE_TIME,
                        TenantInvitationStatusValue.PENDING)));

        service.cancelInvitation(TENANT_A, "inv-1", "誤発行", "admin-1");

        ArgumentCaptor<TenantAdminInvitationStatus> captor =
                ArgumentCaptor.forClass(TenantAdminInvitationStatus.class);
        verify(statusRepository).save(captor.capture());
        TenantAdminInvitationStatus saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(TenantInvitationStatusValue.CANCELLED);
        assertThat(saved.getReason()).isEqualTo("誤発行");
        assertThat(saved.getCreatedBy()).isEqualTo("admin-1");
    }

    @Test
    @DisplayName("cancelInvitation: reasonがnullでも取消可能")
    void cancelInvitation_acceptsNullReason() {
        when(invitationRepository.findLatestByInvitationId("inv-1"))
                .thenReturn(Optional.of(invOf("inv-1", TENANT_A, BASE_TIME,
                        "hash", BASE_TIME.plusDays(1))));
        when(statusRepository.findLatestByInvitationId("inv-1"))
                .thenReturn(Optional.of(statusOf("inv-1", BASE_TIME,
                        TenantInvitationStatusValue.PENDING)));

        service.cancelInvitation(TENANT_A, "inv-1", null, "admin-1");

        ArgumentCaptor<TenantAdminInvitationStatus> captor =
                ArgumentCaptor.forClass(TenantAdminInvitationStatus.class);
        verify(statusRepository).save(captor.capture());
        assertThat(captor.getValue().getReason()).isNull();
    }

    @Test
    @DisplayName("cancelInvitation: 招待が存在しない場合、INVITATION_NOT_FOUNDエラー")
    void cancelInvitation_throwsNotFound_whenInvitationMissing() {
        when(invitationRepository.findLatestByInvitationId("missing"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancelInvitation(TENANT_A, "missing", null, "admin"))
                .isInstanceOf(TenantInvitationException.class)
                .extracting(e -> ((TenantInvitationException) e).getError())
                .isEqualTo(TenantInvitationError.INVITATION_NOT_FOUND);
    }

    @Test
    @DisplayName("cancelInvitation: 別テナントの招待を指定した場合、INVITATION_NOT_FOUNDエラー（越境チェック）")
    void cancelInvitation_throwsNotFound_whenTenantMismatch() {
        when(invitationRepository.findLatestByInvitationId("inv-1"))
                .thenReturn(Optional.of(invOf("inv-1", TENANT_B, BASE_TIME,
                        "hash", BASE_TIME.plusDays(1))));

        assertThatThrownBy(() -> service.cancelInvitation(TENANT_A, "inv-1", null, "admin"))
                .isInstanceOf(TenantInvitationException.class)
                .extracting(e -> ((TenantInvitationException) e).getError())
                .isEqualTo(TenantInvitationError.INVITATION_NOT_FOUND);
        verify(statusRepository, never()).save(any());
    }

    @Test
    @DisplayName("cancelInvitation: 現ステータスがUSEDの場合、ALREADY_USED_FOR_CANCELエラー")
    void cancelInvitation_throwsAlreadyUsedForCancel_whenUsed() {
        when(invitationRepository.findLatestByInvitationId("inv-1"))
                .thenReturn(Optional.of(invOf("inv-1", TENANT_A, BASE_TIME,
                        "hash", BASE_TIME.plusDays(1))));
        when(statusRepository.findLatestByInvitationId("inv-1"))
                .thenReturn(Optional.of(statusOf("inv-1", BASE_TIME,
                        TenantInvitationStatusValue.USED)));

        assertThatThrownBy(() -> service.cancelInvitation(TENANT_A, "inv-1", null, "admin"))
                .isInstanceOf(TenantInvitationException.class)
                .extracting(e -> ((TenantInvitationException) e).getError())
                .isEqualTo(TenantInvitationError.ALREADY_USED_FOR_CANCEL);
        verify(statusRepository, never()).save(any());
    }

    @Test
    @DisplayName("cancelInvitation: 現ステータスがCANCELLEDの場合、ALREADY_CANCELLEDエラー")
    void cancelInvitation_throwsAlreadyCancelled_whenAlreadyCancelled() {
        when(invitationRepository.findLatestByInvitationId("inv-1"))
                .thenReturn(Optional.of(invOf("inv-1", TENANT_A, BASE_TIME,
                        "hash", BASE_TIME.plusDays(1))));
        when(statusRepository.findLatestByInvitationId("inv-1"))
                .thenReturn(Optional.of(statusOf("inv-1", BASE_TIME,
                        TenantInvitationStatusValue.CANCELLED)));

        assertThatThrownBy(() -> service.cancelInvitation(TENANT_A, "inv-1", null, "admin"))
                .isInstanceOf(TenantInvitationException.class)
                .extracting(e -> ((TenantInvitationException) e).getError())
                .isEqualTo(TenantInvitationError.ALREADY_CANCELLED);
        verify(statusRepository, never()).save(any());
    }

    @Test
    @DisplayName("listAllInvitations: PENDINGが先頭、その後は作成日時降順で並ぶ")
    void listAllInvitations_ordersPendingFirstThenByCreatedAtDesc() {
        TenantAdminInvitation oldUsed = invOf("u-1", TENANT_A, BASE_TIME, "hash-1",
                BASE_TIME.plusDays(1));
        TenantAdminInvitation newPending = invOf("p-1", TENANT_A, BASE_TIME.plusHours(2),
                "hash-2", BASE_TIME.plusDays(2));
        TenantAdminInvitation oldPending = invOf("p-2", TENANT_A, BASE_TIME.plusHours(1),
                "hash-3", BASE_TIME.plusDays(3));
        TenantAdminInvitation newCancelled = invOf("c-1", TENANT_A, BASE_TIME.plusHours(3),
                "hash-4", BASE_TIME.plusDays(4));

        when(invitationRepository.findAllLatestByTenantId(TENANT_A))
                .thenReturn(List.of(oldUsed, newPending, oldPending, newCancelled));
        when(statusRepository.findLatestByInvitationId("u-1"))
                .thenReturn(Optional.of(statusOf("u-1", BASE_TIME,
                        TenantInvitationStatusValue.USED)));
        when(statusRepository.findLatestByInvitationId("p-1"))
                .thenReturn(Optional.of(statusOf("p-1", BASE_TIME,
                        TenantInvitationStatusValue.PENDING)));
        when(statusRepository.findLatestByInvitationId("p-2"))
                .thenReturn(Optional.of(statusOf("p-2", BASE_TIME,
                        TenantInvitationStatusValue.PENDING)));
        when(statusRepository.findLatestByInvitationId("c-1"))
                .thenReturn(Optional.of(statusOf("c-1", BASE_TIME,
                        TenantInvitationStatusValue.CANCELLED)));

        List<TenantInvitationWithStatus> result = service.listAllInvitations(TENANT_A);

        assertThat(result).hasSize(4);
        // PENDING先頭、PENDING内は createdAt 降順(新しい順): p-1(+2h) → p-2(+1h)
        assertThat(result.get(0).getInvitation().getInvitationId()).isEqualTo("p-1");
        assertThat(result.get(1).getInvitation().getInvitationId()).isEqualTo("p-2");
        // その他も createdAt 降順: c-1(+3h) → u-1(+0h)
        assertThat(result.get(2).getInvitation().getInvitationId()).isEqualTo("c-1");
        assertThat(result.get(3).getInvitation().getInvitationId()).isEqualTo("u-1");
    }

    @Test
    @DisplayName("listAllInvitations: 招待が0件の場合、空のリストを返す")
    void listAllInvitations_returnsEmpty_whenNoInvitations() {
        when(invitationRepository.findAllLatestByTenantId(TENANT_A)).thenReturn(List.of());

        List<TenantInvitationWithStatus> result = service.listAllInvitations(TENANT_A);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("listAllInvitations: ステータスが存在しない招待はstatus=nullで末尾に並ぶ")
    void listAllInvitations_handlesInvitationsWithoutStatus() {
        TenantAdminInvitation withStatus = invOf("inv-1", TENANT_A, BASE_TIME, "h-1",
                BASE_TIME.plusDays(1));
        TenantAdminInvitation withoutStatus = invOf("inv-2", TENANT_A, BASE_TIME, "h-2",
                BASE_TIME.plusDays(1));
        when(invitationRepository.findAllLatestByTenantId(TENANT_A))
                .thenReturn(List.of(withStatus, withoutStatus));
        when(statusRepository.findLatestByInvitationId("inv-1"))
                .thenReturn(Optional.of(statusOf("inv-1", BASE_TIME,
                        TenantInvitationStatusValue.PENDING)));
        when(statusRepository.findLatestByInvitationId("inv-2"))
                .thenReturn(Optional.empty());

        List<TenantInvitationWithStatus> result = service.listAllInvitations(TENANT_A);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getInvitation().getInvitationId()).isEqualTo("inv-1");
        assertThat(result.get(0).getStatus()).isNotNull();
        assertThat(result.get(1).getInvitation().getInvitationId()).isEqualTo("inv-2");
        assertThat(result.get(1).getStatus()).isNull();
    }

    @Test
    @DisplayName("findInvitationDetail: 招待とステータスが存在する場合、TenantInvitationWithStatusを返す")
    void findInvitationDetail_returnsWithStatus_whenBothExist() {
        TenantAdminInvitation inv = invOf("inv-1", TENANT_A, BASE_TIME, "hash",
                BASE_TIME.plusDays(1));
        when(invitationRepository.findLatestByInvitationId("inv-1"))
                .thenReturn(Optional.of(inv));
        when(statusRepository.findLatestByInvitationId("inv-1"))
                .thenReturn(Optional.of(statusOf("inv-1", BASE_TIME,
                        TenantInvitationStatusValue.PENDING)));

        Optional<TenantInvitationWithStatus> result =
                service.findInvitationDetail(TENANT_A, "inv-1");

        assertThat(result).isPresent();
        assertThat(result.get().getInvitation().getInvitationId()).isEqualTo("inv-1");
        assertThat(result.get().getStatus().getStatus())
                .isEqualTo(TenantInvitationStatusValue.PENDING);
    }

    @Test
    @DisplayName("findInvitationDetail: 招待が存在しない場合、空のOptionalを返す")
    void findInvitationDetail_returnsEmpty_whenInvitationNotFound() {
        when(invitationRepository.findLatestByInvitationId("missing"))
                .thenReturn(Optional.empty());

        Optional<TenantInvitationWithStatus> result =
                service.findInvitationDetail(TENANT_A, "missing");

        assertThat(result).isEmpty();
        verify(statusRepository, never()).findLatestByInvitationId(any());
    }

    @Test
    @DisplayName("findInvitationDetail: 別テナントの招待の場合、空のOptionalを返す（越境チェック）")
    void findInvitationDetail_returnsEmpty_whenTenantMismatch() {
        // 招待は tenant-B のものだが、tenant-A で取得しようとする
        TenantAdminInvitation inv = invOf("inv-1", TENANT_B, BASE_TIME, "hash",
                BASE_TIME.plusDays(1));
        when(invitationRepository.findLatestByInvitationId("inv-1"))
                .thenReturn(Optional.of(inv));

        Optional<TenantInvitationWithStatus> result =
                service.findInvitationDetail(TENANT_A, "inv-1");

        assertThat(result).isEmpty();
        // 越境のためステータス取得まで進まない
        verify(statusRepository, never()).findLatestByInvitationId(any());
    }

    @Test
    @DisplayName("findInvitationDetail: 招待は存在するがステータスが無い場合、status=nullで返す")
    void findInvitationDetail_returnsWithNullStatus_whenStatusMissing() {
        TenantAdminInvitation inv = invOf("inv-1", TENANT_A, BASE_TIME, "hash",
                BASE_TIME.plusDays(1));
        when(invitationRepository.findLatestByInvitationId("inv-1"))
                .thenReturn(Optional.of(inv));
        when(statusRepository.findLatestByInvitationId("inv-1"))
                .thenReturn(Optional.empty());

        Optional<TenantInvitationWithStatus> result =
                service.findInvitationDetail(TENANT_A, "inv-1");

        assertThat(result).isPresent();
        assertThat(result.get().getStatus()).isNull();
    }

    private TenantAdminInvitation invOf(
            String id, String tenantId, OffsetDateTime version,
            String tokenHash, OffsetDateTime expiresAt) {
        return new TenantAdminInvitation(
                id, version, tenantId, tokenHash, expiresAt,
                "display-" + id, version, "creator-" + id);
    }

    private TenantAdminInvitationStatus statusOf(
            String id, OffsetDateTime version, TenantInvitationStatusValue value) {
        return new TenantAdminInvitationStatus(
                id, version, value, null, version, "creator-" + id);
    }

    private void setupValidTokenWithStatus(
            String plainToken, String invitationId, TenantInvitationStatusValue status) {
        String tokenHash = sha256Hex(plainToken);
        TenantAdminInvitation inv = invOf(invitationId, TENANT_A, BASE_TIME, tokenHash,
                BASE_TIME.plusDays(1).withYear(2999)); // 期限内であることを確実にするため遠い未来
        when(invitationRepository.findByTokenHash(tokenHash))
                .thenReturn(Optional.of(inv));
        when(invitationRepository.findLatestByInvitationId(invitationId))
                .thenReturn(Optional.of(inv));
        when(statusRepository.findLatestByInvitationId(invitationId))
                .thenReturn(Optional.of(statusOf(invitationId, BASE_TIME, status)));
    }

}
