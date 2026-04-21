CREATE TABLE wallet (
    id                 BIGSERIAL PRIMARY KEY,
    user_id            BIGINT       NOT NULL UNIQUE REFERENCES app_user(id) ON DELETE CASCADE,
    balance_available  NUMERIC(18,2) NOT NULL DEFAULT 0,
    balance_held       NUMERIC(18,2) NOT NULL DEFAULT 0,
    currency           VARCHAR(10)   NOT NULL DEFAULT 'VND',
    created_at         TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CHECK (balance_available >= 0),
    CHECK (balance_held >= 0)
);

-- System wallet (platform) is a regular wallet identified by convention (user_id = 0 reserved).
-- We'll use a dedicated row inserted when needed via service logic (not here).

CREATE TABLE wallet_transaction (
    id             BIGSERIAL PRIMARY KEY,
    wallet_id      BIGINT        NOT NULL REFERENCES wallet(id) ON DELETE CASCADE,
    type           VARCHAR(32)   NOT NULL,
    amount         NUMERIC(18,2) NOT NULL,
    balance_after  NUMERIC(18,2) NOT NULL,
    booking_id     BIGINT REFERENCES booking(id) ON DELETE SET NULL,
    external_ref   VARCHAR(150),
    status         VARCHAR(32)   NOT NULL DEFAULT 'SUCCESS',
    note           TEXT,
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_wallet_transaction_wallet   ON wallet_transaction(wallet_id);
CREATE INDEX idx_wallet_transaction_booking  ON wallet_transaction(booking_id);
CREATE UNIQUE INDEX uq_wallet_transaction_ref ON wallet_transaction(external_ref) WHERE external_ref IS NOT NULL;

CREATE TABLE payment_order (
    id             BIGSERIAL PRIMARY KEY,
    booking_id     BIGINT        NOT NULL REFERENCES booking(id) ON DELETE CASCADE,
    brand_user_id  BIGINT        NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    amount         NUMERIC(18,2) NOT NULL,
    provider       VARCHAR(32)   NOT NULL,
    status         VARCHAR(32)   NOT NULL DEFAULT 'PENDING',
    payment_url    VARCHAR(500),
    external_ref   VARCHAR(150) UNIQUE,
    paid_at        TIMESTAMPTZ,
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_payment_order_booking ON payment_order(booking_id);
CREATE INDEX idx_payment_order_status  ON payment_order(status);

CREATE TABLE withdraw_request (
    id           BIGSERIAL PRIMARY KEY,
    kol_user_id  BIGINT        NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    amount       NUMERIC(18,2) NOT NULL,
    bank_name    VARCHAR(150)  NOT NULL,
    bank_account VARCHAR(50)   NOT NULL,
    account_name VARCHAR(150)  NOT NULL,
    status       VARCHAR(32)   NOT NULL DEFAULT 'PENDING',
    reject_reason TEXT,
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    processed_at TIMESTAMPTZ
);
CREATE INDEX idx_withdraw_request_user   ON withdraw_request(kol_user_id);
CREATE INDEX idx_withdraw_request_status ON withdraw_request(status);
