-- VNPay signed redirect URLs exceed VARCHAR(500) (HMAC + encoded order info).
ALTER TABLE payment_order
    ALTER COLUMN payment_url TYPE TEXT;
