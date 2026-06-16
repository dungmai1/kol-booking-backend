package kolbooking.datn.payment.service;

import kolbooking.datn.auth.domain.AppUser;
import kolbooking.datn.auth.domain.Role;
import kolbooking.datn.auth.repository.AppUserRepository;
import kolbooking.datn.common.exception.BusinessException;
import kolbooking.datn.common.exception.ErrorCode;
import kolbooking.datn.common.exception.ResourceNotFoundException;
import kolbooking.datn.common.util.SecurityUtils;
import kolbooking.datn.notification.domain.NotificationType;
import kolbooking.datn.notification.service.NotificationService;
import kolbooking.datn.payment.domain.WithdrawRequest;
import kolbooking.datn.payment.domain.WithdrawStatus;
import kolbooking.datn.payment.dto.WithdrawCreateRequest;
import kolbooking.datn.payment.dto.WithdrawResponse;
import kolbooking.datn.payment.repository.WithdrawRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class WithdrawService {

    private final WithdrawRequestRepository withdrawRepository;
    private final WalletService walletService;
    private final AppUserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional
    public WithdrawResponse create(WithdrawCreateRequest req) {
        Role role = SecurityUtils.currentRole();
        if (role != Role.KOL && role != Role.BRAND) {
            throw new BusinessException("Only KOL or BRAND can request withdraw",
                    ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        }
        Long userId = SecurityUtils.currentUserId();
        walletService.holdForWithdraw(userId, req.amount());

        WithdrawRequest saved = withdrawRepository.save(WithdrawRequest.builder()
                .userId(userId)
                .amount(req.amount())
                .bankName(req.bankName())
                .bankAccount(req.bankAccount())
                .accountName(req.accountName())
                .status(WithdrawStatus.PENDING)
                .build());
        log.info("Withdraw request created: id={}, userId={}, role={}, amount={}",
                saved.getId(), userId, role, saved.getAmount());
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public Page<WithdrawResponse> myRequests(Pageable pageable) {
        return withdrawRepository.findByUserId(SecurityUtils.currentUserId(), pageable)
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Page<WithdrawResponse> listByStatus(WithdrawStatus status, Pageable pageable) {
        return withdrawRepository.findByStatus(status, pageable).map(this::toDto);
    }

    @Transactional
    public WithdrawResponse approve(Long id) {
        WithdrawRequest w = load(id);
        requirePending(w);
        w.setStatus(WithdrawStatus.APPROVED);
        w.setProcessedAt(Instant.now());
        WithdrawRequest saved = withdrawRepository.save(w);
        notifyUser(w.getUserId(), NotificationType.WITHDRAW_APPROVED,
                "Yêu cầu rút tiền được duyệt",
                vndText(w.getAmount()) + " đang được chuyển khoản. Vui lòng chờ 1-3 ngày làm việc.",
                "/kol-dashboard/wallet");
        return toDto(saved);
    }

    @Transactional
    public WithdrawResponse markPaid(Long id) {
        WithdrawRequest w = load(id);
        if (w.getStatus() != WithdrawStatus.PENDING && w.getStatus() != WithdrawStatus.APPROVED) {
            throw new BusinessException("Withdraw must be PENDING or APPROVED to mark paid",
                    ErrorCode.BUSINESS_ERROR, HttpStatus.CONFLICT);
        }
        walletService.finalizeWithdraw(w.getUserId(), w.getAmount());
        w.setStatus(WithdrawStatus.PAID);
        w.setProcessedAt(Instant.now());
        WithdrawRequest saved = withdrawRepository.save(w);
        notifyUser(w.getUserId(), NotificationType.WITHDRAW_APPROVED,
                "Rút tiền thành công",
                vndText(w.getAmount()) + " đã được chuyển vào tài khoản " + w.getBankAccount() + " (" + w.getBankName() + ").",
                "/kol-dashboard/wallet");
        return toDto(saved);
    }

    @Transactional
    public WithdrawResponse reject(Long id, String reason) {
        WithdrawRequest w = load(id);
        requirePending(w);
        walletService.cancelWithdraw(w.getUserId(), w.getAmount(),
                "Withdraw rejected: " + (reason == null ? "" : reason));
        w.setStatus(WithdrawStatus.REJECTED);
        w.setRejectReason(reason);
        w.setProcessedAt(Instant.now());
        WithdrawRequest saved = withdrawRepository.save(w);
        String reasonText = (reason != null && !reason.isBlank()) ? " Lý do: " + reason : "";
        notifyUser(w.getUserId(), NotificationType.WITHDRAW_REJECTED,
                "Yêu cầu rút tiền bị từ chối",
                vndText(w.getAmount()) + " đã được hoàn lại vào ví của bạn." + reasonText,
                "/kol-dashboard/wallet");
        return toDto(saved);
    }

    private void notifyUser(Long userId, NotificationType type, String title, String body, String link) {
        try {
            notificationService.send(userId, type, title, body, link);
        } catch (Exception ex) {
            log.warn("Failed to send withdraw notification to userId={}: {}", userId, ex.getMessage());
        }
    }

    private WithdrawResponse toDto(WithdrawRequest w) {
        AppUser user = userRepository.findById(w.getUserId()).orElse(null);
        Role role = user != null ? user.getRole() : null;
        String email = user != null ? user.getEmail() : null;
        return PaymentMapper.toDto(w, role, email);
    }

    private WithdrawRequest load(Long id) {
        return withdrawRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Withdraw request not found: " + id));
    }

    private void requirePending(WithdrawRequest w) {
        if (w.getStatus() != WithdrawStatus.PENDING) {
            throw new BusinessException("Withdraw request is not pending",
                    ErrorCode.BUSINESS_ERROR, HttpStatus.CONFLICT);
        }
    }

    private static String vndText(BigDecimal amount) {
        return String.format("%,.0f₫", amount);
    }
}
