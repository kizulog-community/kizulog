package io.github.kizulog_community.kizulog.domain.systemadmininvitation.model;

import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * システム管理者招待ドメインモデル
 *
 * <p>新たな管理者を招待するための招待情報を表現する。
 * 招待先は招待リンク経由でアクセスし、任意のENABLED な OIDC プロバイダーで
 * 認証を行うことで新規 system_account として登録される。</p>
 *
 * <p>バージョン管理: 同一invitation_idの中でversionが
 * 最大のレコードが有効値となる。
 * バージョン追加時は常に新token_hashを生成し、過去バージョンの
 * token は無効化される（再発行を新バージョンで表現）。</p>
 *
 * <p>token は平文では保持せず、SHA-256 ハッシュ値のみ保存する。
 * 招待URL に含まれる平文 token は発行直後のみ表示し、以後は取り出せない。</p>
 *
 * @author Jun Kobayashi
 */
@Getter
@AllArgsConstructor
public class SystemAdminInvitation {

    /** 招待ID（UUID v4） */
    private final String invitationId;

    /** バージョン */
    private final OffsetDateTime version;

    /** トークンのSHA-256ハッシュ（16進64文字） */
    private final String tokenHash;

    /** 有効期限 */
    private final OffsetDateTime expiresAt;

    /** 招待先表示名（管理用ラベル） */
    private final String displayName;

    /** 作成日時（UTC） */
    private final OffsetDateTime createdAt;

    /** 作成者 */
    private final String createdBy;

}
