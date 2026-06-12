package kolbooking.datn.payment.gateway;

import kolbooking.datn.common.exception.BusinessException;
import kolbooking.datn.common.exception.ErrorCode;
import kolbooking.datn.payment.util.HmacUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;

/**
 * VNPay (pay.vnpay.vn) gateway: builds the signed redirect URL and verifies the HMAC-SHA512
 * signature on the Return/IPN callbacks. Implements VNPay's canonical hashing exactly — sorted
 * field names, URL-encoded values, joined with {@code &} — so signatures match the gateway.
 *
 * <p>Credentials are injected from {@code app.payment.vnpay.*} (env-backed). When the hash secret
 * is absent the gateway refuses to build a URL, so a misconfigured prod fails loudly instead of
 * generating unverifiable links.
 */
@Slf4j
@Component
public class VnPayGateway {

    private static final String VERSION = "2.1.0";
    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Value("${app.payment.vnpay.tmn-code:}")
    private String tmnCode;

    @Value("${app.payment.vnpay.hash-secret:}")
    private String hashSecret;

    @Value("${app.payment.vnpay.pay-url:https://sandbox.vnpayment.vn/paymentv2/vpcpay.html}")
    private String payUrl;

    @Value("${app.payment.vnpay.return-url:http://localhost:8080/api/v1/payments/vnpay/return}")
    private String returnUrl;

    public boolean isConfigured() {
        return tmnCode != null && !tmnCode.isBlank() && hashSecret != null && !hashSecret.isBlank();
    }

    /**
     * Builds the signed VNPay payment URL.
     *
     * @param txnRef    our unique order reference (vnp_TxnRef)
     * @param amountVnd gross amount in VND (will be multiplied by 100 per VNPay spec)
     * @param orderInfo human-readable order description
     * @param clientIp  payer IP address
     */
    public String buildPaymentUrl(String txnRef, BigDecimal amountVnd, String orderInfo, String clientIp) {
        if (!isConfigured()) {
            throw new BusinessException("Cổng thanh toán VNPay chưa được cấu hình",
                    ErrorCode.PAYMENT_ERROR, HttpStatus.SERVICE_UNAVAILABLE);
        }
        long amount = amountVnd.movePointRight(2).longValueExact(); // VND * 100, integer
        LocalDateTime now = LocalDateTime.now(VN_ZONE);

        TreeMap<String, String> params = new TreeMap<>();
        params.put("vnp_Version", VERSION);
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", tmnCode);
        params.put("vnp_Amount", String.valueOf(amount));
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", txnRef);
        params.put("vnp_OrderInfo", orderInfo);
        params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", returnUrl);
        params.put("vnp_IpAddr", clientIp == null || clientIp.isBlank() ? "127.0.0.1" : clientIp);
        params.put("vnp_CreateDate", now.format(TS));
        params.put("vnp_ExpireDate", now.plusMinutes(15).format(TS));

        String hashData = buildCanonical(params);
        String secureHash = HmacUtil.hmacSha512(hashSecret, hashData);
        return payUrl + "?" + hashData + "&vnp_SecureHash=" + secureHash;
    }

    /**
     * Verifies the {@code vnp_SecureHash} on a callback. The signature is computed over all params
     * except the hash fields, sorted and URL-encoded identically to the outbound request.
     */
    public boolean verifySignature(Map<String, String> params) {
        String received = params.get("vnp_SecureHash");
        if (received == null || received.isBlank() || !isConfigured()) {
            return false;
        }
        TreeMap<String, String> signed = new TreeMap<>(params);
        signed.remove("vnp_SecureHash");
        signed.remove("vnp_SecureHashType");
        String computed = HmacUtil.hmacSha512(hashSecret, buildCanonical(signed));
        return HmacUtil.constantTimeEquals(computed, received);
    }

    /** Joins sorted, non-empty params as {@code name=urlencode(value)} with {@code &} (VNPay canonical form). */
    private static String buildCanonical(TreeMap<String, String> params) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : params.entrySet()) {
            String value = e.getValue();
            if (value == null || value.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append('&');
            }
            sb.append(e.getKey())
              .append('=')
              .append(URLEncoder.encode(value, StandardCharsets.US_ASCII));
        }
        return sb.toString();
    }
}
