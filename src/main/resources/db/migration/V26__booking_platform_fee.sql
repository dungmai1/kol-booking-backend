-- Snapshot the platform commission rate onto each booking at creation time so historical
-- bookings keep the rate that applied when they were made, even if the global rate changes later.
-- Existing rows are backfilled with 10 (the rate that was in effect via app.platform.fee-percent).
ALTER TABLE booking
    ADD COLUMN platform_fee_percent NUMERIC(5,2)  NOT NULL DEFAULT 10,
    ADD COLUMN platform_fee_amount  NUMERIC(15,2),
    ADD COLUMN kol_net_amount       NUMERIC(15,2);

-- Backfill settlement amounts for already-completed bookings so the admin revenue views are
-- consistent for historical data (fee = budget * percent / 100, net = budget - fee).
UPDATE booking
   SET platform_fee_amount = ROUND(budget * platform_fee_percent / 100, 2),
       kol_net_amount      = budget - ROUND(budget * platform_fee_percent / 100, 2)
 WHERE status = 'COMPLETED';

COMMENT ON COLUMN booking.platform_fee_percent IS 'Commission rate (%) snapshotted at booking creation';
COMMENT ON COLUMN booking.platform_fee_amount  IS 'Commission amount credited to the platform on completion';
COMMENT ON COLUMN booking.kol_net_amount       IS 'Net amount credited to the KOL on completion';
