-- =====================================================================
-- V32 — Seed KOL TikTok @m.thang3001 (Koi)
--
-- Nguồn: https://www.tiktok.com/@m.thang3001
-- Credentials dev:
--   Email   : m.thang3001@seed.local
--   Password: password123
-- =====================================================================

-- Xóa bản ghi thử nghiệm chưa verify email (nếu có) trước khi seed lại.
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
),
new_channel AS (
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
    FROM new_profile
    RETURNING kol_profile_id
),
new_package AS (
    INSERT INTO kol_pricing_package (kol_profile_id, type, platform, price, description)
    SELECT
        id,
        'VIDEO',
        'TIKTOK',
        3000000.00,
        '1 video TikTok 30–60 giây, quay và đăng trên kênh @m.thang3001'
    FROM new_profile
    RETURNING kol_profile_id
),
new_portfolio AS (
    INSERT INTO kol_portfolio_item (kol_profile_id, title, media_url, media_type, campaign_name)
    SELECT id, v.title, v.media_url, 'VIDEO', v.campaign_name
    FROM new_profile
    CROSS JOIN (VALUES
        ('Day 11 Hoàn thành — 30 ngày thay đổi bản thân', 'https://www.tiktok.com/@m.thang3001/video/7631296449514769684', 'Fitness challenge'),
        ('Buổi tối của người độc lập tình cảm', 'https://www.tiktok.com/@m.thang3001/video/7611216945299344660', 'Vlog đời sống'),
        ('Thức sớm nhất TikTok — bình minh', 'https://www.tiktok.com/@m.thang3001/video/7601665697478954261', 'Vlog buổi sáng'),
        ('Review trà sen vàng cùng Hồng Ngọc', 'https://www.tiktok.com/@m.thang3001/video/7597370056498056469', 'Review đồ uống'),
        ('Chạy rẽ khói là có thật', 'https://www.tiktok.com/@m.thang3001/video/7606174419240635669', 'Vlog viral')
    ) AS v(title, media_url, campaign_name)
    RETURNING kol_profile_id
)
INSERT INTO kol_category (kol_profile_id, category_id)
SELECT p.kol_profile_id, cat.id
FROM new_channel p
CROSS JOIN category cat
WHERE cat.slug IN ('fitness', 'lifestyle');
