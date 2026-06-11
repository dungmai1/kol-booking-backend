package kolbooking.datn.payment.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * HMAC helpers for signing/verifying payment-gateway payloads. Hex output is lowercase to match
 * VNPay's convention. Verification uses a constant-time comparison to avoid timing leaks.
 */
public final class HmacUtil {

    private HmacUtil() {}

    public static String hmacSha512(String key, String data) {
        return hmacHex("HmacSHA512", key, data);
    }

    public static String hmacSha256(String key, String data) {
        return hmacHex("HmacSHA256", key, data);
    }

    private static String hmacHex(String algorithm, String key, String data) {
        if (key == null || key.isEmpty()) {
            throw new IllegalStateException("HMAC secret key is not configured");
        }
        try {
            Mac mac = Mac.getInstance(algorithm);
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), algorithm));
            byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(raw.length * 2);
            for (byte b : raw) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to compute " + algorithm, ex);
        }
    }

    /** Timing-safe, case-insensitive comparison of two hex signatures. */
    public static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        byte[] ab = a.toLowerCase().getBytes(StandardCharsets.UTF_8);
        byte[] bb = b.toLowerCase().getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(ab, bb);
    }
}
