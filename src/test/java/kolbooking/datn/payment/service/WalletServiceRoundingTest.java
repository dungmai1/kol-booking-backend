package kolbooking.datn.payment.service;

import kolbooking.datn.payment.domain.Wallet;
import kolbooking.datn.payment.domain.WalletTransaction;
import kolbooking.datn.payment.repository.WalletRepository;
import kolbooking.datn.payment.repository.WalletTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * EC-5: fee + net == gross exactly for all amounts (including decimal edge cases).
 * EC-2: duplicate externalRef on recordDeposit is silently ignored.
 *
 * Strictness.LENIENT: platform-wallet stub is set up per-test only when feePercent > 0.
 * HALF_UP rounding in releaseToKol ensures net = gross - fee (no penny loss).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WalletServiceRoundingTest {

    @Mock WalletRepository walletRepository;
    @Mock WalletTransactionRepository transactionRepository;

    private WalletService walletService;
    private final AtomicLong walletIdSeq = new AtomicLong(1);

    @BeforeEach
    void setUp() {
        walletService = new WalletService(walletRepository, transactionRepository);
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any(WalletTransaction.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Wallet makeWallet(long userId, String available, String held) {
        Wallet w = new Wallet();
        w.setId(walletIdSeq.getAndIncrement());
        w.setUserId(userId);
        w.setBalanceAvailable(new BigDecimal(available));
        w.setBalanceHeld(new BigDecimal(held));
        w.setCurrency("VND");
        return w;
    }

    /** BigDecimal equality ignoring scale (e.g. 0 == 0.00). */
    private static void assertBDEquals(BigDecimal expected, BigDecimal actual, String message) {
        assertEquals(0, expected.compareTo(actual), () ->
                message + " — expected " + expected.toPlainString() + " but was " + actual.toPlainString());
    }

    // ── EC-5: fee + net == gross exactly ─────────────────────────────────────

    @ParameterizedTest(name = "gross={0}, feePercent={1}")
    @CsvSource({
        "100.00, 10",
        "1000000.00, 10",
        "100.01, 10",   // critical decimal case
        "100.05, 10",
        "100.09, 10",
        "333.33, 10",
        "1.01, 10",
        "0.01, 10",
    })
    void releaseToKol_feeAndNetSumToGross(String grossStr, String feePercentStr) {
        BigDecimal gross = new BigDecimal(grossStr);
        BigDecimal feePercent = new BigDecimal(feePercentStr);

        Wallet brandWallet = makeWallet(1L, "0.00", grossStr);
        Wallet kolWallet   = makeWallet(2L, "0.00", "0.00");
        Wallet platform    = makeWallet(WalletService.PLATFORM_WALLET_USER_ID, "0.00", "0.00");

        when(walletRepository.findByUserIdForUpdate(1L)).thenReturn(Optional.of(brandWallet));
        when(walletRepository.findByUserIdForUpdate(2L)).thenReturn(Optional.of(kolWallet));
        when(walletRepository.findByUserIdForUpdate(WalletService.PLATFORM_WALLET_USER_ID))
                .thenReturn(Optional.of(platform));

        WalletService.ReleaseResult result = walletService.releaseToKol(1L, 2L, gross, feePercent, 99L);

        // EC-5: fee + net must exactly equal gross (no penny lost or gained).
        assertBDEquals(gross, result.fee().add(result.net()),
                "fee + net must equal gross");

        // KOL balance must reflect exactly the net amount.
        assertBDEquals(result.net(), kolWallet.getBalanceAvailable(),
                "KOL balance after release");

        // Brand held balance fully consumed.
        assertBDEquals(BigDecimal.ZERO, brandWallet.getBalanceHeld(),
                "brand held balance after release");
    }

    @Test
    void releaseToKol_zeroFeePercent_noFeeCharged() {
        BigDecimal gross = new BigDecimal("500.00");

        Wallet brand    = makeWallet(1L, "0.00", "500.00");
        Wallet kol      = makeWallet(2L, "0.00", "0.00");

        when(walletRepository.findByUserIdForUpdate(1L)).thenReturn(Optional.of(brand));
        when(walletRepository.findByUserIdForUpdate(2L)).thenReturn(Optional.of(kol));

        WalletService.ReleaseResult result = walletService.releaseToKol(1L, 2L, gross, BigDecimal.ZERO, 10L);

        assertBDEquals(BigDecimal.ZERO, result.fee(), "fee must be zero");
        assertBDEquals(gross, result.net(), "net must equal full gross");
        // Platform wallet must NOT be touched when fee == 0.
        verify(walletRepository, never()).findByUserIdForUpdate(WalletService.PLATFORM_WALLET_USER_ID);
    }

    // ── EC-2 idempotency ─────────────────────────────────────────────────────

    @Test
    void recordDeposit_duplicateExternalRef_isIgnoredWithNoSideEffects() {
        String ref = "ORD-DUPLICATE-001";
        when(transactionRepository.existsByExternalRef(ref)).thenReturn(true);

        WalletTransaction result = walletService.recordDeposit(1L, new BigDecimal("500000"), 10L, ref, "note");

        assertNull(result, "duplicate externalRef must return null");
        verify(walletRepository, never()).findByUserIdForUpdate(anyLong());
        verify(walletRepository, never()).save(any());
    }

    @Test
    void recordDeposit_freshExternalRef_holdsBalanceExactly() {
        String ref = "ORD-FRESH-001";
        when(transactionRepository.existsByExternalRef(ref)).thenReturn(false);

        Wallet wallet = makeWallet(5L, "0.00", "0.00");
        when(walletRepository.findByUserIdForUpdate(5L)).thenReturn(Optional.of(wallet));

        walletService.recordDeposit(5L, new BigDecimal("200000"), 20L, ref, "Brand deposit");

        assertBDEquals(new BigDecimal("200000"), wallet.getBalanceHeld(), "held balance after deposit");
        assertBDEquals(BigDecimal.ZERO, wallet.getBalanceAvailable(), "available unchanged");
    }
}
