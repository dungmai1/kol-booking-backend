package kolbooking.datn.payment.repository;

import kolbooking.datn.payment.domain.WithdrawRequest;
import kolbooking.datn.payment.domain.WithdrawStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WithdrawRequestRepository extends JpaRepository<WithdrawRequest, Long> {
    Page<WithdrawRequest> findByKolUserId(Long kolUserId, Pageable pageable);
    Page<WithdrawRequest> findByStatus(WithdrawStatus status, Pageable pageable);
}
