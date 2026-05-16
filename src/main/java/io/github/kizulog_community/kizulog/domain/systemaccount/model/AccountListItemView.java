package io.github.kizulog_community.kizulog.domain.systemaccount.model;

import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * システム管理アカウント一覧画面の表示用ビュー
 *
 * @author Jun Kobayashi
 */
@Getter
@AllArgsConstructor
public class AccountListItemView {

    /** アカウントID（表示用IDも兼ねる） */
    private final String accountId;

    /** 現在のステータス（最新版） */
    private final AccountStatus currentStatus;

    /** アカウントの作成日時 */
    private final OffsetDateTime createdAt;

    /** 現在ACTIVEな（最新ステータスがACTIVEの）identity数 */
    private final int activeIdentityCount;

    /** SYSTEM_ADMIN ロールが現在ACTIVEで付与されているか */
    private final boolean systemAdminRoleActive;

    /** ログイン中の操作者自身か（UIで自分自身に対する操作ボタンを非活性にする用途） */
    private final boolean self;

}
