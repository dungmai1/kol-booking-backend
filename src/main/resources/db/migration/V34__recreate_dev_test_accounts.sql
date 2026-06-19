-- =====================================================================
-- V34 — Recreate dev/test accounts (verified, correct role & password)
--
-- KOL   m.thang3001@seed.local                         / password123
-- ADMIN admin@dev.local                                / password123
-- KOL   test.kol.20260613170517@kolbooking.test        / TestPass123!
-- BRAND test.brand.20260613170517@kolbooking.test      / TestPass123!
-- =====================================================================

-- ADMIN (upsert)
INSERT INTO app_user (email, password_hash, role, status, email_verified)
VALUES (
    'admin@dev.local',
    '$2a$10$ZJo6Pxi.C7ichgif9MD6DOZCjfn3AxLh2q18qP39T5qHtxF3u9d7C',
    'ADMIN',
    'ACTIVE',
    TRUE
)
ON CONFLICT (email) DO UPDATE
SET password_hash = EXCLUDED.password_hash,
    role = 'ADMIN',
    status = 'ACTIVE',
    email_verified = TRUE,
    updated_at = NOW();

-- m.thang3001 handled by V32 (re-run safe: deletes then re-seeds full KOL profile)
DELETE FROM app_user WHERE email = 'm.thang3001@seed.local';

WITH new_user AS (
    INSERT INTO app_user (email, password_hash, role, status, email_verified)
    VALUES (
        'm.thang3001@seed.local',
        '$2a$10$ZJo6Pxi.C7ichgif9MD6DOZCjfn3AxLh2q18qP39T5qHtxF3u9d7C',
        'KOL',
        'ACTIVE',
        TRUE
    )
    RETURNING id
),
new_profile AS (
    INSERT INTO kol_profile (
        user_id, display_name, slug, bio, gender, city, country,
        status, avg_rating, review_count, max_follower_count, min_price
    )
    SELECT
        id,
        'Koi',
        'm-thang3001',
        'TikToker chia sẻ nội dung gym, fitness, push-up và vlog đời sống. Kênh TikTok @m.thang3001.',
        'MALE',
        'Hà Nội',
        'VN',
        'APPROVED',
        0,
        0,
        1235,
        3000000.00
    FROM new_user
    RETURNING id
)
INSERT INTO kol_social_channel (
    kol_profile_id, platform, url, username, follower_count, engagement_rate, verified
)
SELECT
    id,
    'TIKTOK',
    'https://www.tiktok.com/@m.thang3001',
    'm.thang3001',
    1235,
    5.00,
    FALSE
FROM new_profile;

-- Test KOL (hard recreate)
DELETE FROM app_user WHERE email = 'test.kol.20260613170517@kolbooking.test';

WITH new_user AS (
    INSERT INTO app_user (email, password_hash, role, status, email_verified)
    VALUES (
        'test.kol.20260613170517@kolbooking.test',
        '$2a$10$mNYjvhXr61/A0z5UuHnFc.GujlK9IxiK2dRZ/rZ/r18Ia06ajkxOW',
        'KOL',
        'ACTIVE',
        TRUE
    )
    RETURNING id
)
INSERT INTO kol_profile (user_id, display_name, slug, status)
SELECT id, 'Test KOL 20260613', 'test-kol-20260613170517', 'DRAFT'
FROM new_user;

-- Test BRAND (hard recreate)
DELETE FROM app_user WHERE email = 'test.brand.20260613170517@kolbooking.test';

WITH new_user AS (
    INSERT INTO app_user (email, password_hash, role, status, email_verified)
    VALUES (
        'test.brand.20260613170517@kolbooking.test',
        '$2a$10$mNYjvhXr61/A0z5UuHnFc.GujlK9IxiK2dRZ/rZ/r18Ia06ajkxOW',
        'BRAND',
        'ACTIVE',
        TRUE
    )
    RETURNING id
)
INSERT INTO brand_profile (user_id, company_name, status)
SELECT id, 'Test Brand 20260613', 'DRAFT'
FROM new_user;
