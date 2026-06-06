package me.ihqqq.authLogin.utils;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;


public final class SecretEncryptor {

    private static final String ALGORITHM     = "AES/GCM/NoPadding";
    private static final int    GCM_IV_LEN    = 12;
    private static final int    GCM_TAG_BITS  = 128;
    private static final int    AES_KEY_BITS  = 256;

    public static final String ENC_PREFIX = "ENC:";

    private final SecretKey aesKey;
    private final SecureRandom rng = new SecureRandom();

    public SecretEncryptor(byte[] keyBytes) {
        if (keyBytes.length != 32) {
            throw new IllegalArgumentException(
                    "AES-256 key phải đúng 32 bytes, nhận được: " + keyBytes.length);
        }
        this.aesKey = new SecretKeySpec(keyBytes, "AES");
    }

    public String encrypt(String plaintext) {
        try {
            byte[] iv = new byte[GCM_IV_LEN];
            rng.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, new GCMParameterSpec(GCM_TAG_BITS, iv));

            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            byte[] combined = new byte[GCM_IV_LEN + ciphertext.length];
            System.arraycopy(iv,         0, combined, 0,          GCM_IV_LEN);
            System.arraycopy(ciphertext, 0, combined, GCM_IV_LEN, ciphertext.length);

            return ENC_PREFIX + Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi mã hóa TOTP secret: " + e.getMessage(), e);
        }
    }

    public String decrypt(String stored) {
        if (!stored.startsWith(ENC_PREFIX)) {
            throw new IllegalArgumentException("Giá trị không có prefix ENC: — không phải ciphertext hợp lệ");
        }

        try {
            byte[] combined = Base64.getDecoder().decode(stored.substring(ENC_PREFIX.length()));

            if (combined.length < GCM_IV_LEN + 16) {
                throw new IllegalArgumentException("Ciphertext quá ngắn, dữ liệu có thể bị hỏng");
            }

            byte[] iv         = new byte[GCM_IV_LEN];
            byte[] ciphertext = new byte[combined.length - GCM_IV_LEN];
            System.arraycopy(combined, 0,          iv,         0, GCM_IV_LEN);
            System.arraycopy(combined, GCM_IV_LEN, ciphertext, 0, ciphertext.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, aesKey, new GCMParameterSpec(GCM_TAG_BITS, iv));

            byte[] plainBytes = cipher.doFinal(ciphertext);
            return new String(plainBytes, java.nio.charset.StandardCharsets.UTF_8);

        } catch (javax.crypto.AEADBadTagException e) {

            throw new RuntimeException("GCM authentication thất bại — dữ liệu bị giả mạo hoặc sai server key", e);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi giải mã TOTP secret: " + e.getMessage(), e);
        }
    }

    public static boolean isEncrypted(String value) {
        return value != null && value.startsWith(ENC_PREFIX);
    }

    public static byte[] generateKey() {
        try {
            KeyGenerator kg = KeyGenerator.getInstance("AES");
            kg.init(AES_KEY_BITS, new SecureRandom());
            return kg.generateKey().getEncoded();
        } catch (Exception e) {
            throw new RuntimeException("Không thể tạo AES key: " + e.getMessage(), e);
        }
    }
}