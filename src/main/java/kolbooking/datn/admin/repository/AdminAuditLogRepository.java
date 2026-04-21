package kolbooking.datn.admin.repository;

import kolbooking.datn.admin.domain.AdminAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, Long> {
    Page<AdminAuditLog> findByAdminIdOrderByCreatedAtDesc(Long adminId, Pageable pageable);
    Page<AdminAuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
