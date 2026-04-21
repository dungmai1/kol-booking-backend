package kolbooking.datn.payment.controller;

import kolbooking.datn.common.util.SecurityUtils;
import kolbooking.datn.payment.domain.Wallet;
import kolbooking.datn.payment.dto.WalletResponse;
import kolbooking.datn.payment.dto.WalletTransactionResponse;
import kolbooking.datn.payment.repository.WalletTransactionRepository;
import kolbooking.datn.payment.service.PaymentMapper;
import kolbooking.datn.payment.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/wallet")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class WalletController {

    private final WalletService walletService;
    private final WalletTransactionRepository transactionRepository;

    @GetMapping("/me")
    public ResponseEntity<WalletResponse> me() {
        Wallet wallet = walletService.getOrCreate(SecurityUtils.currentUserId());
        return ResponseEntity.ok(PaymentMapper.toDto(wallet));
    }

    @GetMapping("/me/transactions")
    public ResponseEntity<Page<WalletTransactionResponse>> transactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Wallet wallet = walletService.getOrCreate(SecurityUtils.currentUserId());
        Page<WalletTransactionResponse> result = transactionRepository
                .findByWalletIdOrderByCreatedAtDesc(wallet.getId(), PageRequest.of(page, size))
                .map(PaymentMapper::toDto);
        return ResponseEntity.ok(result);
    }
}
