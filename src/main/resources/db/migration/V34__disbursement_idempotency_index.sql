-- Prevent duplicate RELEASE or REFUND ledger entries for the same (booking, wallet) pair.
-- releaseToKol() writes two RELEASE rows: one on the brand wallet (debit) and one on the
-- KOL wallet (credit). Each pair must be unique per wallet, hence the (booking_id, wallet_id)
-- composite. The application-level guard in WalletService is the first line of defence;
-- this index is the hard DB backstop that survives event replay or duplicate webhook calls.
CREATE UNIQUE INDEX IF NOT EXISTS uq_wallet_tx_booking_wallet_release
    ON wallet_transaction (booking_id, wallet_id)
    WHERE type = 'RELEASE' AND booking_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_wallet_tx_booking_wallet_refund
    ON wallet_transaction (booking_id, wallet_id)
    WHERE type = 'REFUND' AND booking_id IS NOT NULL;
