package kolbooking.datn.admin.service;

import kolbooking.datn.admin.domain.AdminAuditLog;
import kolbooking.datn.admin.repository.AdminAuditLogRepository;
import kolbooking.datn.common.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AdminAuditLogRepository auditRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String action, String targetType, Long targetId, String payload) {
        Long adminId = SecurityUtils.currentUserIdSafe();
        if (adminId == null) return;
        auditRepository.save(AdminAuditLog.builder()
                .adminId(adminId)
                .action(action)
                .targetType(targetType)
                .targetId(targetId)
                .payload(payload)
                .build());
    }
}
