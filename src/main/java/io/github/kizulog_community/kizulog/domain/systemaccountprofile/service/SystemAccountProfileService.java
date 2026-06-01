package io.github.kizulog_community.kizulog.domain.systemaccountprofile.service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.kizulog_community.kizulog.domain.systemaccountprofile.exception.AccountProfileError;
import io.github.kizulog_community.kizulog.domain.systemaccountprofile.exception.AccountProfileException;
import io.github.kizulog_community.kizulog.domain.systemaccountprofile.model.SystemAccountProfile;
import io.github.kizulog_community.kizulog.domain.systemaccountprofile.port.SystemAccountProfileRepository;
import lombok.RequiredArgsConstructor;

/**
 * システム管理アカウントプロファイルサービス
 *
 * @author Jun Kobayashi
 */
@Service
@RequiredArgsConstructor
public class SystemAccountProfileService {

    /** ロガー */
    private static final Logger log =
            LoggerFactory.getLogger(SystemAccountProfileService.class);

    /** プロファイルリポジトリ */
    private final SystemAccountProfileRepository repository;

    /**
     * identityIdで最新版のプロファイルを取得する。
     *
     * @param identityId Identity ID
     * @return 最新版のプロファイル（未登録時は空）
     * @throws AccountProfileException identityIdが未指定の場合
     */
    @Transactional(readOnly = true)
    public Optional<SystemAccountProfile> getProfile(String identityId) {
        validateIdentityId(identityId);
        return repository.findLatestByIdentityId(identityId);
    }

    /**
     * クレームに差分があった場合のみプロファイルを新バージョンとして保存する。
     *
     * @param identityId Identity ID
     * @param newClaims ホワイトリストフィルタ済みのクレーム
     * @param createdBy 作成者（通常はOIDC subクレーム）
     * @throws AccountProfileException identityIdまたはclaimsが未指定の場合
     */
    @Transactional
    public void upsertIfChanged(
            String identityId,
            Map<String, Object> newClaims,
            String createdBy) {
        validateIdentityId(identityId);
        validateClaims(newClaims);

        Optional<SystemAccountProfile> latest =
                repository.findLatestByIdentityId(identityId);
        if (latest.isPresent() && Objects.equals(latest.get().getClaims(), newClaims)) {
            // 差分なし: DB肥大化抑制のためsaveスキップ
            log.debug("プロファイルクレームに差分なし、save をスキップしました: identityId={}",
                    identityId);
            return;
        }

        OffsetDateTime version = OffsetDateTime.now(ZoneOffset.UTC);
        SystemAccountProfile profile = new SystemAccountProfile(
                identityId,
                version,
                newClaims,
                version,
                createdBy);
        repository.save(profile);
        log.info("プロファイルキャッシュを更新しました: identityId={}, createdBy={}",
                identityId, createdBy);
    }

    /**
     * identityIdのバリデーション。
     *
     * @param identityId Identity ID
     * @throws AccountProfileException nullまたは空文字の場合
     */
    private void validateIdentityId(String identityId) {
        if (identityId == null || identityId.isBlank()) {
            throw new AccountProfileException(AccountProfileError.IDENTITY_ID_REQUIRED);
        }
    }

    /**
     * claimsのバリデーション。
     *
     * @param claims クレーム
     * @throws AccountProfileException nullまたは空マップの場合
     */
    private void validateClaims(Map<String, Object> claims) {
        if (claims == null || claims.isEmpty()) {
            throw new AccountProfileException(AccountProfileError.CLAIMS_REQUIRED);
        }
    }

}
