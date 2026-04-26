package io.github.kizulog_community.kizulog.domain.port;

/**
 * 暗号化・復号ポートインターフェース（Output Port）
 *
 * <p>機密情報の暗号・復号化処理を行う。
 * 実装はインフラ層が担い、ドメイン層はこのインターフェースのみに依存する。</p>
 *
 * @author Jun Kobayashi
 */
public interface CryptoPort {

    /**
     * 文字列を暗号化する.
     *
     * @param plainText 暗号化する平文
     * @return 暗号化された文字列
     */
    String encrypt(String plainText);

    /**
     * 暗号文を復号する.
     *
     * @param encryptedText 暗号化された文字列
     * @return 復号化された平文
     */
    String decrypt(String encryptedText);

}