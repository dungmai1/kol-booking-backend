ALTER TABLE booking
    ADD COLUMN IF NOT EXISTS revision_feedback TEXT,
    ADD COLUMN IF NOT EXISTS revision_requested_at TIMESTAMPTZ;

ALTER TABLE booking_deliverable
    ADD COLUMN IF NOT EXISTS brand_feedback TEXT;
