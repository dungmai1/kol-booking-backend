-- Performance indexes for /api/v1/kols/search
-- The default & "featured" sort hits:
--   WHERE status = 'APPROVED' ORDER BY avg_rating DESC, review_count DESC
-- Add a covering composite index so Postgres can stream sorted rows without a heap sort.
CREATE INDEX IF NOT EXISTS idx_kol_profile_status_rating_review
    ON kol_profile (status, avg_rating DESC, review_count DESC);

-- "newest" sort path
CREATE INDEX IF NOT EXISTS idx_kol_profile_status_created
    ON kol_profile (status, created_at DESC);

-- Channel filters (platform + follower range) used as EXISTS subquery from kol_profile.
CREATE INDEX IF NOT EXISTS idx_kol_channel_profile_platform_follower
    ON kol_social_channel (kol_profile_id, platform, follower_count);

-- Pricing filters (price range) used as EXISTS subquery from kol_profile.
CREATE INDEX IF NOT EXISTS idx_kol_package_profile_price
    ON kol_pricing_package (kol_profile_id, price);
