package kolbooking.datn.payment.repository;

import kolbooking.datn.payment.domain.TransactionType;
import kolbooking.datn.payment.domain.WalletTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {
    Page<WalletTransaction> findByWalletIdOrderByCreatedAtDesc(Long walletId, Pageable pageable);

    Page<WalletTransaction> findByTypeOrderByCreatedAtDesc(TransactionType type, Pageable pageable);

    boolean existsByExternalRef(String externalRef);
    boolean existsByBookingIdAndType(Long bookingId, TransactionType type);
}
