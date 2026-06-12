-- Audit columns for the payment gateway callbacks (VNPay IPN/Return, mock webhook).
ALTER TABLE payment_order
    ADD COLUMN provider_txn_ref VARCHAR(100),
    ADD COLUMN raw_callback     TEXT;

COMMENT ON COLUMN payment_order.provider_txn_ref IS 'Gateway transaction id (e.g. VNPay vnp_TransactionNo)';
COMMENT ON COLUMN payment_order.raw_callback     IS 'Raw verified callback payload, kept for audit';
