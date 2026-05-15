package io.github.kizulog_community.kizulog.domain.systemaccount.exception;

import lombok.Getter;

/**
 * Identityリンク関連のドメイン例外
 *
 * @author Jun Kobayashi
 */
@Getter
public class IdentityLinkException extends RuntimeException {

    /** シリアライズUID */
    private static final long serialVersionUID = 1L;

    /** エラー種別 */
    private final IdentityLinkError error;

    /**
     * コンストラクタ
     *
     * @param error エラー種別
     */
    public IdentityLinkException(IdentityLinkError error) {
        super("Identity link error: " + error.name());
        this.error = error;
    }

}
