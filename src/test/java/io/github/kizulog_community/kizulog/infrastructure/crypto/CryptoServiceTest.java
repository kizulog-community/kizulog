package io.github.kizulog_community.kizulog.infrastructure.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * CryptoServiceの単体テスト
 *
 * @author Jun Kobayashi
 */
class CryptoServiceTest {

    /** テスト用秘密鍵（32文字以上） */
    private static final String TEST_SECRET_KEY = "0123456789abcdef0123456789abcdef";

    @Test
    @DisplayName("encrypt()は平文をBase64エンコード文字列に変換する")
    void encrypt_returnsBase64EncodedString() {
        CryptoService service = new CryptoService(TEST_SECRET_KEY);
        String encrypted = service.encrypt("test-plaintext");
        assertThat(encrypted).isNotNull().isNotEmpty();
        assertThat(encrypted).matches("^[A-Za-z0-9+/=]+$");
    }

    @Test
    @DisplayName("encrypt()の結果は平文と異なる")
    void encrypt_resultDiffersFromPlainText() {
        CryptoService service = new CryptoService(TEST_SECRET_KEY);
        String plainText = "test-plaintext";
        String encrypted = service.encrypt(plainText);
        assertThat(encrypted).isNotEqualTo(plainText);
    }

    @Test
    @DisplayName("encrypt()は同じ平文でも毎回異なる結果を返す（IVがランダムなため）")
    void encrypt_sameInputProducesDifferentOutput() {
        CryptoService service = new CryptoService(TEST_SECRET_KEY);
        String plainText = "test-plaintext";
        String encrypted1 = service.encrypt(plainText);
        String encrypted2 = service.encrypt(plainText);
        assertThat(encrypted1).isNotEqualTo(encrypted2);
    }

    @Test
    @DisplayName("encrypt()→decrypt()で元の平文に戻る")
    void encryptThenDecrypt_returnsOriginalPlainText() {
        CryptoService service = new CryptoService(TEST_SECRET_KEY);
        String plainText = "test-plaintext";
        String encrypted = service.encrypt(plainText);
        String decrypted = service.decrypt(encrypted);
        assertThat(decrypted).isEqualTo(plainText);
    }

    @Test
    @DisplayName("encrypt()→decrypt()で日本語の平文も正しく復号できる")
    void encryptThenDecrypt_japaneseText() {
        CryptoService service = new CryptoService(TEST_SECRET_KEY);
        String plainText = "日本語のテスト文字列";
        String encrypted = service.encrypt(plainText);
        String decrypted = service.decrypt(encrypted);
        assertThat(decrypted).isEqualTo(plainText);
    }

    @Test
    @DisplayName("encrypt()→decrypt()で空文字列も正しく処理できる")
    void encryptThenDecrypt_emptyString() {
        CryptoService service = new CryptoService(TEST_SECRET_KEY);
        String plainText = "";
        String encrypted = service.encrypt(plainText);
        String decrypted = service.decrypt(encrypted);
        assertThat(decrypted).isEqualTo(plainText);
    }

    @Test
    @DisplayName("decrypt()に不正なBase64を渡すとRuntimeExceptionをスローする")
    void decrypt_invalidBase64_throwsException() {
        CryptoService service = new CryptoService(TEST_SECRET_KEY);
        assertThatThrownBy(() -> service.decrypt("invalid base64!!!"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("復号に失敗しました");
    }

    @Test
    @DisplayName("decrypt()に改ざんされた暗号文を渡すとRuntimeExceptionをスローする")
    void decrypt_tamperedCipherText_throwsException() {
        CryptoService service = new CryptoService(TEST_SECRET_KEY);
        String encrypted = service.encrypt("test-plaintext");
        // 末尾を改ざん
        String tampered = encrypted.substring(0, encrypted.length() - 4) + "XXXX";
        assertThatThrownBy(() -> service.decrypt(tampered))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("復号に失敗しました");
    }

    @Test
    @DisplayName("decrypt()に異なる秘密鍵で暗号化された文字列を渡すとRuntimeExceptionをスローする")
    void decrypt_wrongKey_throwsException() {
        CryptoService service1 = new CryptoService(TEST_SECRET_KEY);
        CryptoService service2 = new CryptoService(
                "differentkey0123456789abcdef0123");
        String encrypted = service1.encrypt("test-plaintext");
        assertThatThrownBy(() -> service2.decrypt(encrypted))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("復号に失敗しました");
    }

    @Test
    @DisplayName("32文字未満の秘密鍵でも初期化できる（残りはゼロパディング）")
    void constructor_shortKey_initializesWithZeroPadding() {
        CryptoService service = new CryptoService("shortkey");
        // 暗号化・復号できればOK
        String encrypted = service.encrypt("test");
        assertThat(service.decrypt(encrypted)).isEqualTo("test");
    }

    @Test
    @DisplayName("32文字を超える秘密鍵でも初期化できる（先頭32文字のみ使用）")
    void constructor_longKey_usesFirst32Bytes() {
        CryptoService service = new CryptoService(
                "0123456789abcdef0123456789abcdefEXTRA_DATA_IGNORED");
        String encrypted = service.encrypt("test");
        assertThat(service.decrypt(encrypted)).isEqualTo("test");
    }

}