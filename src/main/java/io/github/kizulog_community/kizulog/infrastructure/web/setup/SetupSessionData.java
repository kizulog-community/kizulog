package io.github.kizulog_community.kizulog.infrastructure.web.setup;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import io.github.kizulog_community.kizulog.domain.shared.SupportedLanguage;
import io.github.kizulog_community.kizulog.domain.shared.SupportedTimezone;
import lombok.Getter;
import lombok.Setter;

/**
 * セットアップウィザードセッションデータ。
 *
 * <p>ウィザードの各ステップで入力されたデータを
 * 一時保存するセッションスコープのDTOクラス。</p>
 *
 * @author Jun Kobayashi
 */
@Component
@SessionScope
@Getter
@Setter
public class SetupSessionData implements Serializable {

    private static final long serialVersionUID = 758730933041353765L;

    /** セットアップ言語 */
    private SupportedLanguage setupLanguage;

	/** OIDC設定リスト */
    private List<OidcSetting> oidcSettings = new ArrayList<>();

    /** デフォルト言語 */
    private SupportedLanguage defaultLanguage;

    /** 利用可能言語リスト */
    private List<SupportedLanguage> availableLanguages = new ArrayList<>();

    /** デフォルトタイムゾーン */
    private SupportedTimezone defaultTimezone;

    /** 利用可能タイムゾーンリスト */
    private List<SupportedTimezone> availableTimezones = new ArrayList<>();
    /** ホスト名（Step2で設定）. */
    private String host;

    /** 初期管理者のiss */
    private String adminIss;

    /** 初期管理者のaud */
    private String adminAud;

    /** 初期管理者のsub */
    private String adminSub;

    /** 初期管理者の氏名 */
    private String adminName;

    /** 初期管理者のメールアドレス */
    private String adminEmail;
    
}