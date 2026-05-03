package io.github.kizulog_community.kizulog.domain.systemaccount.model;

/**
 * システム管理アカウントのロール
 *
 * <p>system_account_roles.roleカラムに永続化される値を表す。
 * DBには"SYSTEM_ADMIN"等が文字列として保存される。</p>
 *
 * <p>本Enumはドメイン層に属し、Spring Securityには依存しない。
 * Spring SecurityのGrantedAuthority文字列（"ROLE_SYSTEM_ADMIN"等）への
 * 変換はインフラ層（SystemUserPrincipal）で行う。</p>
 *
 * @author Jun Kobayashi
 */
public enum SystemRole {

    /** システム管理者ロール: システム管理画面の全機能にアクセス可能 */
    SYSTEM_ADMIN;

}
