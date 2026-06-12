package kolbooking.datn.payment.service;

import kolbooking.datn.common.exception.BusinessException;
import kolbooking.datn.common.exception.ErrorCode;
import kolbooking.datn.payment.domain.TransactionStatus;
import kolbooking.datn.payment.domain.TransactionType;
import kolbooking.datn.payment.domain.Wallet;
import kolbooking.datn.payment.domain.WalletTransaction;
import kolbooking.datn.payment.repository.WalletRepository;
import kolbooking.datn.payment.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Centralized wallet operations. All balance mutations MUST go through this service so
 * {@link WalletTransaction} rows and balance changes stay consistent.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WalletService {

    static final Long PLATFORM_WALLET_USER_ID = 0L;

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;

    @Transactional(propagation = Propagation.REQUIRED)
    public Wallet getOrCreate(Long userId) {
        return walletRepository.findByUserId(userId).orElseGet(() ->
                walletRepository.save(Wallet.builder().userId(userId).build()));
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public Wallet getOrCreatePlatformWallet() {
        return getOrCreate(PLATFORM_WALLET_USER_ID);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public WalletTransaction recordDeposit(Long userId, BigDecimal amount, Long bookingId,
                                            String externalRef, String note) {
        if (externalRef != null && transactionRepository.existsByExternalRef(externalRef)) {
            log.info("Duplicate deposit externalRef={} ignored", externalRef);
            return null;
        }
        Wallet wallet = lockWallet(userId);
        wallet.setBalanceHeld(wallet.getBalanceHeld().add(amount));
        walletRepository.save(wallet);
        return saveTx(wallet, TransactionType.HOLD, amount, bookingId, externalRef, note);
    }

    /** Breakdown of a booking settlement: platform commission and the KOL's net credit. */
    public record ReleaseResult(BigDecimal fee, BigDecimal net) {}

    @Transactional(propagation = Propagation.REQUIRED)
    public ReleaseResult releaseToKol(Long brandUserId, Long kolUserId, BigDecimal grossAmount,
                                      BigDecimal feePercent, Long bookingId) {
        Wallet brandWallet = lockWallet(brandUserId);
        if (brandWallet.getBalanceHeld().compareTo(grossAmount) < 0) {
            throw new BusinessException("Insufficient held balance on brand wallet",
                    ErrorCode.BUSINESS_ERROR, HttpStatus.CONFLICT);
        }
        brandWallet.setBalanceHeld(brandWallet.getBalanceHeld().subtract(grossAmount));
        walletRepository.save(brandWallet);
        saveTx(brandWallet, TransactionType.RELEASE, grossAmount.negate(), bookingId, null,
                "Release from HOLD to KOL");

        // Round half-up to 2 decimals (VND amounts) so fee + net == gross exactly.
        BigDecimal fee = grossAmount.multiply(feePercent)
                .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
        BigDecimal net = grossAmount.subtract(fee);

        Wallet kolWallet = lockWallet(kolUserId);
        kolWallet.setBalanceAvailable(kolWallet.getBalanceAvailable().add(net));
        walletRepository.save(kolWallet);
        saveTx(kolWallet, TransactionType.RELEASE, net, bookingId, null, "Credit net from booking");

        if (fee.signum() > 0) {
            Wallet platform = lockWallet(PLATFORM_WALLET_USER_ID);
            platform.setBalanceAvailable(platform.getBalanceAvailable().add(fee));
            walletRepository.save(platform);
            saveTx(platform, TransactionType.FEE, fee, bookingId, null, "Platform fee");
        }
        return new ReleaseResult(fee, net);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void refundBrand(Long brandUserId, BigDecimal amount, Long bookingId, String note) {
        Wallet wallet = lockWallet(brandUserId);
        if (wallet.getBalanceHeld().compareTo(amount) < 0) {
            throw new BusinessException("Insufficient held balance to refund",
                    ErrorCode.BUSINESS_ERROR, HttpStatus.CONFLICT);
        }
        wallet.setBalanceHeld(wallet.getBalanceHeld().subtract(amount));
        wallet.setBalanceAvailable(wallet.getBalanceAvailable().add(amount));
        walletRepository.save(wallet);
        saveTx(wallet, TransactionType.REFUND, amount, bookingId, null, note);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void holdForWithdraw(Long kolUserId, BigDecimal amount) {
        Wallet wallet = lockWallet(kolUserId);
        if (wallet.getBalanceAvailable().compareTo(amount) < 0) {
            throw new BusinessException("Insufficient available balance",
                    ErrorCode.BUSINESS_ERROR, HttpStatus.BAD_REQUEST);
        }
        wallet.setBalanceAvailable(wallet.getBalanceAvailable().subtract(amount));
        wallet.setBalanceHeld(wallet.getBalanceHeld().add(amount));
        walletRepository.save(wallet);
        saveTx(wallet, TransactionType.HOLD, amount, null, null, "Hold for withdraw request");
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void finalizeWithdraw(Long kolUserId, BigDecimal amount) {
        Wallet wallet = lockWallet(kolUserId);
        if (wallet.getBalanceHeld().compareTo(amount) < 0) {
            throw new BusinessException("Held amount insufficient for withdraw",
                    ErrorCode.BUSINESS_ERROR, HttpStatus.CONFLICT);
        }
        wallet.setBalanceHeld(wallet.getBalanceHeld().subtract(amount));
        walletRepository.save(wallet);
        saveTx(wallet, TransactionType.WITHDRAW, amount.negate(), null, null, "Withdraw paid out");
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void cancelWithdraw(Long kolUserId, BigDecimal amount, String note) {
        Wallet wallet = lockWallet(kolUserId);
        if (wallet.getBalanceHeld().compareTo(amount) < 0) {
            throw new BusinessException("Held amount insufficient to cancel withdraw",
                    ErrorCode.BUSINESS_ERROR, HttpStatus.CONFLICT);
        }
        wallet.setBalanceHeld(wallet.getBalanceHeld().subtract(amount));
        wallet.setBalanceAvailable(wallet.getBalanceAvailable().add(amount));
        walletRepository.save(wallet);
        saveTx(wallet, TransactionType.REFUND, amount, null, null, note);
    }

    private Wallet lockWallet(Long userId) {
        return walletRepository.findByUserIdForUpdate(userId).orElseGet(() ->
                walletRepository.save(Wallet.builder().userId(userId).build()));
    }

    private WalletTransaction saveTx(Wallet wallet, TransactionType type, BigDecimal amount,
                                     Long bookingId, String externalRef, String note) {
        BigDecimal balanceAfter = wallet.getBalanceAvailable().add(wallet.getBalanceHeld());
        return transactionRepository.save(WalletTransaction.builder()
                .walletId(wallet.getId())
                .type(type)
                .amount(amount)
                .balanceAfter(balanceAfter)
                .bookingId(bookingId)
                .externalRef(externalRef)
                .status(TransactionStatus.SUCCESS)
                .note(note)
                .build());
    }
}
