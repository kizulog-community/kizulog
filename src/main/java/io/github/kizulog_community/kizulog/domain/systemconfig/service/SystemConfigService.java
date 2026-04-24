package io.github.kizulog_community.kizulog.domain.systemconfig.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import io.github.kizulog_community.kizulog.domain.systemconfig.model.SystemConfig;
import io.github.kizulog_community.kizulog.domain.systemconfig.port.SystemConfigRepository;
import lombok.RequiredArgsConstructor;

/**
 * システム設定サービス
 *
 * <p>システム設定に関するユースケースを実装したクラス。
 * ドメイン層のOutput Port（{@link SystemConfigRepository}）を通じてDBのデータ取得・保存を行う。</p>
 *
 * @author Jun Kobayashi
 */
@Service
@RequiredArgsConstructor
public class SystemConfigService {

    /** システム設定リポジトリ */
    private final SystemConfigRepository systemConfigRepository;

    /**
     * 指定されたキーの最新バージョンのシステム設定を取得する。
     *
     * @param key 設定グループ名
     * @return 最新バージョンのシステム設定、存在しない場合は空のOptional
     */
    @Transactional(readOnly = true)
    public Optional<SystemConfig> findLatestByKey(String key) {
        return systemConfigRepository.findLatestByKey(key);
    }

    /**
     * 指定されたキーの指定バージョンのシステム設定を取得する。
     *
     * @param key 設定グループ名
     * @param version 設定バージョン
     * @return 指定バージョンのシステム設定、存在しない場合は空のOptional
     */
    @Transactional(readOnly = true)
    public Optional<SystemConfig> findByKeyAndVersion(String key, OffsetDateTime version) {
        return systemConfigRepository.findByKeyAndVersion(key, version);
    }

    /**
     * 指定されたキーの全バージョンのシステム設定を取得する。
     *
     * @param key 設定グループ名
     * @return 全バージョンのシステム設定のリスト、存在しない場合は空のリスト
     */
    @Transactional(readOnly = true)
    public List<SystemConfig> findAllByKey(String key) {
        return systemConfigRepository.findAllByKey(key);
    }

    /**
     * システム設定を保存する。
     *
     * @param systemConfig 保存するシステム設定
     */
    @Transactional
    public void save(SystemConfig systemConfig) {
        systemConfigRepository.save(systemConfig);
    }

}