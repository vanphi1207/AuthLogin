package me.ihqqq.authLogin.utils;

import org.apache.commons.codec.binary.Base32;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class TotpUtil {

    private static int TIME_STEP = 30; // 30 giây reset mã 1 lần
    private static final int CODE_DIGITS = 6; // 6 chữ số mã xác thực
    private static final int WINDOW = 1;

    public static String generateSecret() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[20];
        random.nextBytes(bytes);
        Base32 base32 = new Base32();
        return base32.encodeToString(bytes).replace("=", "");
    }

    public static boolean verifyCode(String secret, int userCode) {
        long currentInterval = System.currentTimeMillis() / 1000L / TIME_STEP;
        for (int i = -WINDOW; i <= WINDOW; i++) {
            int generatedCode = generateCode(secret, currentInterval + i);
            if (generatedCode == userCode) {
                return true;
            }
        }
        return false;
    }

    private static int generateCode(String secret, long interval) {
        Base32 base32 = new Base32();
        byte[] key = base32.decode(secret.toUpperCase());

        byte[] data = ByteBuffer.allocate(8).putLong(interval).array();

        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] hash = mac.doFinal(data);

            int offset = hash[hash.length - 1] & 0x0F;
            int truncated = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);

            return truncated % (int) Math.pow(10, CODE_DIGITS);

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Lỗi TOTP: " + e.getMessage(), e);
        }
    }

    public static String buildOtpAuthUri(String secret, String playerName, String issuer) {
        String encodedName = playerName.replace(" ", "%20");
        String encodedIssuer = issuer.replace(" ", "%20");
        return String.format(
                "otpauth://totp/%s:%s?secret=%s&issuer=%s&algorithm=SHA1&digits=%d&period=%d",
                encodedIssuer, encodedName, secret, encodedIssuer, CODE_DIGITS, TIME_STEP
        );
    }
}
