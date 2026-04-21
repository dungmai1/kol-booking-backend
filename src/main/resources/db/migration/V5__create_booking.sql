CREATE TABLE booking (
    id                BIGSERIAL PRIMARY KEY,
    brand_profile_id  BIGINT       NOT NULL REFERENCES brand_profile(id) ON DELETE RESTRICT,
    kol_profile_id    BIGINT       NOT NULL REFERENCES kol_profile(id)   ON DELETE RESTRICT,
    campaign_title    VARCHAR(200) NOT NULL,
    campaign_brief    TEXT,
    deliverables      JSONB,
    budget            NUMERIC(15,2) NOT NULL,
    start_date        DATE,
    end_date          DATE,
    status            VARCHAR(32)   NOT NULL DEFAULT 'PENDING',
    reject_reason     TEXT,
    cancel_reason     TEXT,
    invoice_url       VARCHAR(500),
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_booking_brand   ON booking(brand_profile_id);
CREATE INDEX idx_booking_kol     ON booking(kol_profile_id);
CREATE INDEX idx_booking_status  ON booking(status);

CREATE TABLE booking_message (
    id             BIGSERIAL PRIMARY KEY,
    booking_id     BIGINT       NOT NULL REFERENCES booking(id) ON DELETE CASCADE,
    sender_user_id BIGINT       NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    content        TEXT         NOT NULL,
    attachment_url VARCHAR(500),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_booking_message_booking ON booking_message(booking_id);

CREATE TABLE booking_deliverable (
    id            BIGSERIAL PRIMARY KEY,
    booking_id    BIGINT       NOT NULL REFERENCES booking(id) ON DELETE CASCADE,
    type          VARCHAR(32)  NOT NULL,
    platform      VARCHAR(32)  NOT NULL,
    submitted_url VARCHAR(500),
    submitted_at  TIMESTAMPTZ,
    status        VARCHAR(32)  NOT NULL DEFAULT 'PENDING',
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_booking_deliverable_booking ON booking_deliverable(booking_id);

CREATE TABLE booking_status_history (
    id              BIGSERIAL PRIMARY KEY,
    booking_id      BIGINT       NOT NULL REFERENCES booking(id) ON DELETE CASCADE,
    from_status     VARCHAR(32),
    to_status       VARCHAR(32)  NOT NULL,
    changed_by_user BIGINT REFERENCES app_user(id) ON DELETE SET NULL,
    note            TEXT,
    changed_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_booking_status_history_booking ON booking_status_history(booking_id);
