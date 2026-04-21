CREATE TABLE category (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    slug       VARCHAR(120) NOT NULL UNIQUE,
    parent_id  BIGINT REFERENCES category(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_category_parent ON category(parent_id);

CREATE TABLE kol_category (
    kol_profile_id BIGINT NOT NULL REFERENCES kol_profile(id) ON DELETE CASCADE,
    category_id    BIGINT NOT NULL REFERENCES category(id) ON DELETE CASCADE,
    PRIMARY KEY (kol_profile_id, category_id)
);
CREATE INDEX idx_kol_category_category ON kol_category(category_id);

-- Seed default categories
INSERT INTO category (name, slug) VALUES
 ('Beauty', 'beauty'),
 ('Fashion', 'fashion'),
 ('Tech', 'tech'),
 ('Food', 'food'),
 ('Lifestyle', 'lifestyle'),
 ('Travel', 'travel'),
 ('Fitness', 'fitness'),
 ('Gaming', 'gaming'),
 ('Education', 'education'),
 ('Parenting', 'parenting'),
 ('Finance', 'finance'),
 ('Entertainment', 'entertainment');

-- Brand favorites (Phase 04)
CREATE TABLE brand_favorite (
    brand_profile_id BIGINT NOT NULL REFERENCES brand_profile(id) ON DELETE CASCADE,
    kol_profile_id   BIGINT NOT NULL REFERENCES kol_profile(id) ON DELETE CASCADE,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (brand_profile_id, kol_profile_id)
);
CREATE INDEX idx_brand_favorite_kol ON brand_favorite(kol_profile_id);
