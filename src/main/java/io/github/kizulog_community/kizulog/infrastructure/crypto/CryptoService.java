package io.github.kizulog_community.kizulog.infrastructure.crypto;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 暗号・復号化サービス
 *
 * <p>AES-256-GCMを使用して機密情報の暗号化・復号を行う。
 * 暗号化キーはアプリケーションプロパティから取得する。</p>
 *
 * <p>暗号化フォーマット：Base64（IV + 暗号文 + 認証タグ）</p>
 *
 * @author Jun Kobayashi
 */
@Component
public class CryptoService {

    /** GCMの認証タグ長（ビット） */
    private static final int GCM_TAG_LENGTH = 128;

    /** IVのバイト長 */
    private static final int IV_LENGTH = 12;

    /** アルゴリズム名 */
    private static final String ALGORITHM = "AES/GCM/NoPadding";

    /** 秘密鍵 */
    private final SecretKey secretKey;

    /**
     * コンストラクタ
     *
     * @param secretKeyString プロパティから取得する秘密鍵文字列（32文字以上）
     */
    public CryptoService(
            @Value("${kizulog.crypto.secret-key}") String secretKeyString) {
        byte[] keyBytes = secretKeyString.getBytes(StandardCharsets.UTF_8);
        byte[] key256 = new byte[32];
        System.arraycopy(keyBytes, 0, key256, 0, Math.min(keyBytes.length, 32));
        this.secretKey = new SecretKeySpec(key256, "AES");
    }

    /**
     * 文字列を暗号化する。
     *
     * @param plainText 暗号化する平文
     * @return Base64エンコードされた暗号文（IV込み）
     * @throws RuntimeException 暗号化に失敗した場合
     */
    public String encrypt(String plainText) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey,
                    new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            byte[] encrypted = cipher.doFinal(
                    plainText.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[IV_LENGTH + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, IV_LENGTH);
            System.arraycopy(encrypted, 0, combined, IV_LENGTH, encrypted.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("暗号化に失敗しました", e);
        }
    }

    /**
     * 暗号文を復号化する。
     *
     * @param encryptedText Base64エンコードされた暗号文（IV込み）
     * @return 復号された平文
     * @throws RuntimeException 復号に失敗した場合
     */
    public String decrypt(String encryptedText) {
        try {
            byte[] combined = Base64.getDecoder().decode(encryptedText);

            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH);

            byte[] encrypted = new byte[combined.length - IV_LENGTH];
            System.arraycopy(combined, IV_LENGTH, encrypted, 0, encrypted.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey,
                    new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("復号に失敗しました", e);
        }
    }
    
}