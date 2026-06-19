CREATE TABLE application_message (
    id              BIGSERIAL PRIMARY KEY,
    application_id  BIGINT      NOT NULL REFERENCES product_application(id),
    sender_user_id  BIGINT      NOT NULL,
    sender_role     VARCHAR(16) NOT NULL,   -- 'KOL' or 'BRAND'
    content         TEXT        NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_app_msg_application ON application_message (application_id, created_at DESC);
