-- KOL profile
CREATE TABLE kol_profile (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT       NOT NULL UNIQUE REFERENCES app_user(id) ON DELETE CASCADE,
    display_name    VARCHAR(150) NOT NULL,
    slug            VARCHAR(150) NOT NULL UNIQUE,
    avatar_url      VARCHAR(500),
    cover_url       VARCHAR(500),
    bio             TEXT,
    gender          VARCHAR(16),
    date_of_birth   DATE,
    city            VARCHAR(100),
    country         VARCHAR(100),
    status          VARCHAR(32)  NOT NULL DEFAULT 'DRAFT',
    avg_rating      NUMERIC(3,2) NOT NULL DEFAULT 0,
    review_count    INTEGER      NOT NULL DEFAULT 0,
    reject_reason   TEXT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_kol_profile_status  ON kol_profile(status);
CREATE INDEX idx_kol_profile_city    ON kol_profile(city);
CREATE INDEX idx_kol_profile_country ON kol_profile(country);

-- KOL social channels (1-n)
CREATE TABLE kol_social_channel (
    id                BIGSERIAL PRIMARY KEY,
    kol_profile_id    BIGINT       NOT NULL REFERENCES kol_profile(id) ON DELETE CASCADE,
    platform          VARCHAR(32)  NOT NULL,
    url               VARCHAR(500) NOT NULL,
    username          VARCHAR(150),
    follower_count    BIGINT       NOT NULL DEFAULT 0,
    engagement_rate   NUMERIC(5,2) NOT NULL DEFAULT 0,
    verified          BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_kol_channel_profile   ON kol_social_channel(kol_profile_id);
CREATE INDEX idx_kol_channel_platform  ON kol_social_channel(platform);
CREATE INDEX idx_kol_channel_follower  ON kol_social_channel(follower_count);

-- KOL pricing packages (1-n)
CREATE TABLE kol_pricing_package (
    id                BIGSERIAL PRIMARY KEY,
    kol_profile_id    BIGINT       NOT NULL REFERENCES kol_profile(id) ON DELETE CASCADE,
    type              VARCHAR(32)  NOT NULL,
    platform          VARCHAR(32)  NOT NULL,
    price             NUMERIC(15,2) NOT NULL,
    description       TEXT,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_kol_package_profile   ON kol_pricing_package(kol_profile_id);
CREATE INDEX idx_kol_package_price     ON kol_pricing_package(price);
CREATE INDEX idx_kol_package_platform  ON kol_pricing_package(platform);

-- KOL portfolio items (1-n)
CREATE TABLE kol_portfolio_item (
    id                BIGSERIAL PRIMARY KEY,
    kol_profile_id    BIGINT       NOT NULL REFERENCES kol_profile(id) ON DELETE CASCADE,
    title             VARCHAR(200) NOT NULL,
    media_url         VARCHAR(500) NOT NULL,
    media_type        VARCHAR(32)  NOT NULL,
    campaign_name     VARCHAR(200),
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_kol_portfolio_profile ON kol_portfolio_item(kol_profile_id);

-- Brand profile
CREATE TABLE brand_profile (
    id             BIGSERIAL PRIMARY KEY,
    user_id        BIGINT       NOT NULL UNIQUE REFERENCES app_user(id) ON DELETE CASCADE,
    company_name   VARCHAR(200) NOT NULL,
    tax_code       VARCHAR(50),
    industry       VARCHAR(150),
    logo_url       VARCHAR(500),
    website        VARCHAR(300),
    contact_name   VARCHAR(150),
    contact_phone  VARCHAR(30),
    address        VARCHAR(500),
    status         VARCHAR(32)  NOT NULL DEFAULT 'DRAFT',
    reject_reason  TEXT,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_brand_profile_status ON brand_profile(status);
