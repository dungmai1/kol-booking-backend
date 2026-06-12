package kolbooking.datn.payment.gateway;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VnPayGatewayTest {

    private final VnPayGateway gateway = new VnPayGateway();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(gateway, "tmnCode", "TESTTMN01");
        ReflectionTestUtils.setField(gateway, "hashSecret", "VNPAYHASHSECRETFORTESTONLY");
        ReflectionTestUtils.setField(gateway, "payUrl", "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html");
        ReflectionTestUtils.setField(gateway, "returnUrl", "http://localhost:8080/api/v1/payments/vnpay/return");
    }

    @Test
    void buildPaymentUrl_thenVerifySignature_roundtrips() {
        String url = gateway.buildPaymentUrl(
                "ORD0001", new BigDecimal("1800000"), "Thanh toan booking #1", "1.2.3.4");

        Map<String, String> params = parseQuery(url);
        assertTrue(params.containsKey("vnp_SecureHash"), "URL must carry vnp_SecureHash");
        // vnp_Amount must be VND * 100
        assertTrue(params.get("vnp_Amount").equals("180000000"));
        assertTrue(gateway.verifySignature(params), "freshly-built params must verify");
    }

    @Test
    void verifySignature_rejectsTamperedAmount() {
        String url = gateway.buildPaymentUrl(
                "ORD0002", new BigDecimal("500000"), "Thanh toan booking #2", "1.2.3.4");
        Map<String, String> params = parseQuery(url);

        params.put("vnp_Amount", "100"); // attacker lowers the amount
        assertFalse(gateway.verifySignature(params), "tampered amount must fail verification");
    }

    @Test
    void verifySignature_rejectsMissingHash() {
        String url = gateway.buildPaymentUrl(
                "ORD0003", new BigDecimal("500000"), "Thanh toan booking #3", "1.2.3.4");
        Map<String, String> params = parseQuery(url);
        params.remove("vnp_SecureHash");
        assertFalse(gateway.verifySignature(params));
    }

    private static Map<String, String> parseQuery(String url) {
        Map<String, String> map = new HashMap<>();
        String query = url.substring(url.indexOf('?') + 1);
        for (String pair : query.split("&")) {
            int eq = pair.indexOf('=');
            String key = pair.substring(0, eq);
            String value = URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.US_ASCII);
            map.put(key, value);
        }
        return map;
    }
}
