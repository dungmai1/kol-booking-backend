-- Brand product postings ("đăng tin") and KOL applications to them.

CREATE TABLE product (
    id                BIGSERIAL PRIMARY KEY,
    brand_profile_id  BIGINT        NOT NULL REFERENCES brand_profile(id) ON DELETE CASCADE,
    title             VARCHAR(200)  NOT NULL,
    description       TEXT,
    image_url         VARCHAR(500),
    budget            NUMERIC(15,2),
    category_id       BIGINT        REFERENCES category(id) ON DELETE SET NULL,
    required_platform VARCHAR(32),
    min_followers     BIGINT,
    slots             INTEGER       NOT NULL DEFAULT 1 CHECK (slots >= 1),
    status            VARCHAR(32)   NOT NULL DEFAULT 'OPEN',
    deadline          DATE,
    application_count INTEGER       NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_product_brand    ON product(brand_profile_id);
CREATE INDEX idx_product_status   ON product(status);
CREATE INDEX idx_product_category ON product(category_id);

CREATE TABLE product_application (
    id              BIGSERIAL PRIMARY KEY,
    product_id      BIGINT        NOT NULL REFERENCES product(id) ON DELETE CASCADE,
    kol_profile_id  BIGINT        NOT NULL REFERENCES kol_profile(id) ON DELETE CASCADE,
    message         TEXT,
    proposed_price  NUMERIC(15,2),
    status          VARCHAR(32)   NOT NULL DEFAULT 'PENDING',
    booking_id      BIGINT        REFERENCES booking(id) ON DELETE SET NULL,
    reject_reason   TEXT,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_product_application UNIQUE (product_id, kol_profile_id)
);
CREATE INDEX idx_product_application_product ON product_application(product_id);
CREATE INDEX idx_product_application_kol     ON product_application(kol_profile_id);
CREATE INDEX idx_product_application_status  ON product_application(status);
