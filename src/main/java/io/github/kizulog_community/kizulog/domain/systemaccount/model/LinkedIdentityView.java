package io.github.kizulog_community.kizulog.domain.systemaccount.model;

import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * リンク済みidentity一覧表示用ビュー
 *
 * @author Jun Kobayashi
 */
@Getter
@AllArgsConstructor
public class LinkedIdentityView {

    /** identity ID */
    private final String identityId;

    /** OIDC Issuer URI（識別用） */
    private final String iss;

    /** OIDC Subject */
    private final String sub;

    /** プロバイダーID（system_oidc_providers.provider_id、表示と「同一provider検出」に使う） */
    private final String providerId;

    /** プロバイダー表示名（system_oidc_providers.display_name） */
    private final String providerDisplayName;

    /** プロバイダーがENABLEDか（DISABLEDの場合UIで補足表示する） */
    private final boolean providerEnabled;

    /** identity自体のステータスがACTIVEか */
    private final boolean active;

    /** このidentityが現在ログイン中のセッションで使用されているか */
    private final boolean currentSession;

    /** identityの作成日時 */
    private final OffsetDateTime createdAt;

    /** identityステータスの最新版（INACTIVE化日時としても解釈可能） */
    private final OffsetDateTime statusVersion;

}
