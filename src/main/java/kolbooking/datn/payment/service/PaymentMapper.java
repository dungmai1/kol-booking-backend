package kolbooking.datn.payment.service;

import kolbooking.datn.payment.domain.Wallet;
import kolbooking.datn.payment.domain.WalletTransaction;
import kolbooking.datn.payment.domain.WithdrawRequest;
import kolbooking.datn.payment.dto.WalletResponse;
import kolbooking.datn.payment.dto.WalletTransactionResponse;
import kolbooking.datn.payment.dto.WithdrawResponse;

public final class PaymentMapper {

    private PaymentMapper() {}

    public static WalletResponse toDto(Wallet w) {
        return new WalletResponse(w.getId(), w.getUserId(),
                w.getBalanceAvailable(), w.getBalanceHeld(), w.getCurrency());
    }

    public static WalletTransactionResponse toDto(WalletTransaction t) {
        return new WalletTransactionResponse(
                t.getId(), t.getType(), t.getAmount(), t.getBalanceAfter(),
                t.getBookingId(), t.getExternalRef(), t.getStatus(), t.getNote(), t.getCreatedAt()
        );
    }

    public static WithdrawResponse toDto(WithdrawRequest w) {
        return new WithdrawResponse(
                w.getId(), w.getKolUserId(), w.getAmount(),
                w.getBankName(), w.getBankAccount(), w.getAccountName(),
                w.getStatus(), w.getRejectReason(), w.getCreatedAt(), w.getProcessedAt()
        );
    }
}
