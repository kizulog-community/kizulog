package io.github.kizulog_community.kizulog.domain.systemaccount.model;

import java.time.OffsetDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * システム管理アカウント詳細画面の表示用ビュー
 *
 * @author Jun Kobayashi
 */
@Getter
@AllArgsConstructor
public class AccountDetailView {

    /** アカウントID */
    private final String accountId;

    /** アカウントの作成日時 */
    private final OffsetDateTime accountCreatedAt;

    /** アカウントの作成者 */
    private final String accountCreatedBy;

    /** 現在のステータス（最新版） */
    private final AccountStatus currentStatus;

    /** 現在ステータスの変更理由 */
    private final String currentStatusReason;

    /** 現在ステータスのバージョン（変更日時に相当） */
    private final OffsetDateTime currentStatusVersion;

    /** 現在ステータスの作成者 */
    private final String currentStatusUpdatedBy;

    /** SYSTEM_ADMIN ロールが現在ACTIVEで付与されているか */
    private final boolean systemAdminRoleActive;

    /** 現在ACTIVEな identity 数 */
    private final int activeIdentityCount;

    /** identity 数（INACTIVE 含む全件） */
    private final int totalIdentityCount;

    /** ステータス履歴（新しい順） */
    private final List<AccountStatusHistoryEntry> statusHistory;

    /** ログイン中の操作者自身か（自爆禁止の判定用） */
    private final boolean self;

}
