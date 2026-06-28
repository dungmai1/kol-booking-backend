ALTER TABLE booking
    ADD COLUMN IF NOT EXISTS attachment_url VARCHAR(500);

COMMENT ON COLUMN booking.attachment_url IS 'Optional booking brief or contract attachment URL visible to booking participants';
