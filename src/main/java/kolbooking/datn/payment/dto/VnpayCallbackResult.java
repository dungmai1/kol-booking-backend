package kolbooking.datn.payment.dto;

/**
 * Outcome of processing a VNPay callback. {@code rspCode}/{@code message} are returned verbatim to
 * VNPay on the IPN channel; {@code success}/{@code bookingId} drive the browser redirect on Return.
 */
public record VnpayCallbackResult(
        String rspCode,
        String message,
        boolean success,
        Long bookingId
) {}
