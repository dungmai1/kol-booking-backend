-- Denormalize aggregates lên kol_profile để xoá N+1 trong /kols/search và /kols/featured.
-- Trước fix: mỗi page 20 KOL = 1 query profile + 1 batched query channels + 1 batched query packages
-- = 3-5 roundtrip × 100ms RTT (Supabase Tokyo) → 1100-1500ms.
-- Sau fix: 1 query duy nhất, đọc max_follower_count + min_price trực tiếp từ kol_profile.
-- avg_rating / review_count đã được denormalize sẵn từ V8 — cùng nguyên lý.
--
-- Indexes phục vụ filter min_follower / min_price / max_price trong search.

ALTER TABLE kol_profile
    ADD COLUMN max_follower_count BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN min_price NUMERIC(15, 2);

-- Backfill từ dữ liệu hiện có
UPDATE kol_profile k SET
    max_follower_count = COALESCE(
        (SELECT MAX(c.follower_count) FROM kol_social_channel c WHERE c.kol_profile_id = k.id),
        0
    ),
    min_price = (
        SELECT MIN(p.price) FROM kol_pricing_package p WHERE p.kol_profile_id = k.id
    );

CREATE INDEX idx_kol_profile_max_follower ON kol_profile (max_follower_count);
CREATE INDEX idx_kol_profile_min_price ON kol_profile (min_price);
