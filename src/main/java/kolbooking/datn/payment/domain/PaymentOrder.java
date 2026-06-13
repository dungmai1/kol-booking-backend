package kolbooking.datn.payment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payment_order")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    @Column(name = "brand_user_id", nullable = false)
    private Long brandUserId;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PaymentProvider provider;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PaymentOrderStatus status;

    @Column(name = "payment_url", columnDefinition = "text")
    private String paymentUrl;

    @Column(name = "external_ref", length = 150, unique = true)
    private String externalRef;

    /** Gateway-side transaction id (e.g. VNPay vnp_TransactionNo), set on a confirmed callback. */
    @Column(name = "provider_txn_ref", length = 100)
    private String providerTxnRef;

    /** Raw callback payload kept for audit/dispute resolution. */
    @Column(name = "raw_callback", columnDefinition = "text")
    private String rawCallback;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (status == null) status = PaymentOrderStatus.PENDING;
    }
}
