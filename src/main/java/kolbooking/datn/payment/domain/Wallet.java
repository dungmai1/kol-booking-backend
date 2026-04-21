package kolbooking.datn.payment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "wallet")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "balance_available", nullable = false, precision = 18, scale = 2)
    private BigDecimal balanceAvailable;

    @Column(name = "balance_held", nullable = false, precision = 18, scale = 2)
    private BigDecimal balanceHeld;

    @Column(nullable = false, length = 10)
    private String currency;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (balanceAvailable == null) balanceAvailable = BigDecimal.ZERO;
        if (balanceHeld == null) balanceHeld = BigDecimal.ZERO;
        if (currency == null) currency = "VND";
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
