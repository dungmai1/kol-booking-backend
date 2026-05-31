-- =====================================================================
-- V20 — Seed reviews cho các KOL nhập khẩu từ kols-koc.com (V14).
--
-- Mục tiêu:
--   Tất cả KOL từ V14 (@kols-koc.imported) đang có reviewCount=0 và
--   avgRating=0.0 vì V15 chỉ seed review cho V8 KOLs (@seed.local).
--   Migration này bổ sung booking + review (BRAND_TO_KOL) cho 12 KOL
--   V14 và recompute avg_rating / review_count trên kol_profile.
--
-- Phụ thuộc:
--   - V2  (app_user)
--   - V3  (brand_profile, kol_profile)
--   - V5  (booking)
--   - V7  (review)
--   - V14 (KOL import — slug lookup)
--   - V15 (brand users: Vinamilk, Shopee đã tồn tại)
--
-- Lưu ý:
--   - Reuse các brand user/profile đã tạo ở V15 (Vinamilk, Shopee Vietnam).
--   - Thêm 2 brand mới: Highlands Coffee và Unilever Vietnam.
--   - Campaign titles phải UNIQUE (khác với V15 titles).
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1) Brand users mới (chỉ những brand chưa tồn tại)
-- ---------------------------------------------------------------------
INSERT INTO app_user (email, password_hash, role, status, email_verified) VALUES
    ('brand.highlands@seed.local',  '$2a$10$ZJo6Pxi.C7ichgif9MD6DOZCjfn3AxLh2q18qP39T5qHtxF3u9d7C', 'BRAND', 'ACTIVE', TRUE),
    ('brand.unilever@seed.local',   '$2a$10$ZJo6Pxi.C7ichgif9MD6DOZCjfn3AxLh2q18qP39T5qHtxF3u9d7C', 'BRAND', 'ACTIVE', TRUE);

-- ---------------------------------------------------------------------
-- 2) Brand profiles mới (status = APPROVED)
-- ---------------------------------------------------------------------
INSERT INTO brand_profile (user_id, company_name, industry, contact_name, status)
SELECT u.id, m.company_name, m.industry, m.contact_name, 'APPROVED'
FROM (VALUES
    ('brand.highlands@seed.local',  'Highlands Coffee',  'F&B',                       'Trần Cao Nguyên'),
    ('brand.unilever@seed.local',   'Unilever Vietnam',  'FMCG / Chăm sóc cá nhân',   'Nguyễn Thu Unilever')
) AS m(email, company_name, industry, contact_name)
JOIN app_user u ON u.email = m.email;

-- ---------------------------------------------------------------------
-- 3) Bookings COMPLETED cho các V14 KOL
--    Tra brand_profile qua company_name, tra kol_profile qua slug.
-- ---------------------------------------------------------------------
INSERT INTO booking (
    brand_profile_id, kol_profile_id, campaign_title, campaign_brief,
    deliverables, budget, start_date, end_date, status
)
SELECT
    bp.id,
    kp.id,
    m.campaign_title,
    m.campaign_brief,
    m.deliverables,
    m.budget,
    m.start_date,
    m.end_date,
    'COMPLETED'
FROM (VALUES
    -- Highlands Coffee campaigns
    ('Highlands Coffee', 'nhan-phuong-chi-xu', 'Highlands Kết Nối Gia Đình',
        'Campaign cà phê gia đình — khoảnh khắc sum vầy bên ly cà phê Highlands.',
        '2 TikTok videos + 1 IG reel', 22000000.00::NUMERIC(15,2), DATE '2026-03-01', DATE '2026-03-20'),
    ('Highlands Coffee', 'duc-va-ly',           'Highlands Date Night',
        'Couple content — buổi hẹn hò lý tưởng với không gian Highlands Coffee.',
        '2 TikTok couple vlogs',       18000000.00::NUMERIC(15,2), DATE '2026-03-15', DATE '2026-04-01'),
    ('Highlands Coffee', 'dumi',                'Highlands Morning Ritual',
        'Series lifestyle buổi sáng — bắt đầu ngày mới cùng cà phê Highlands.',
        '3 TikTok lifestyle shorts',   15000000.00::NUMERIC(15,2), DATE '2026-02-10', DATE '2026-03-05'),
    ('Highlands Coffee', 'nha-hieu-review',     'Highlands Menu Mới 2026',
        'Review toàn bộ menu mới Q1/2026 của Highlands Coffee theo phong cách honest review.',
        '2 TikTok review videos',      12000000.00::NUMERIC(15,2), DATE '2026-02-20', DATE '2026-03-10'),

    -- Unilever Vietnam campaigns
    ('Unilever Vietnam', 'linh-baeli',          'Dove Skincare Routine Challenge',
        'Skincare routine hàng ngày với sản phẩm Dove — target Gen Z female.',
        '2 TikTok + 1 IG carousel',    28000000.00::NUMERIC(15,2), DATE '2026-04-01', DATE '2026-04-20'),
    ('Unilever Vietnam', 'ngo-thi-kim-yen',     'Sunsilk Tóc Đẹp Mỗi Ngày',
        'Review và hướng dẫn chăm sóc tóc với dầu gội Sunsilk.',
        '2 TikTok hair care videos',   20000000.00::NUMERIC(15,2), DATE '2026-03-25', DATE '2026-04-15'),
    ('Unilever Vietnam', 'hong-ngoc-van',       'Clear Anti-Dandruff Campaign',
        'Chia sẻ trải nghiệm dùng dầu gội Clear — honest & relatable content.',
        '1 TikTok vlog + 1 short',     14000000.00::NUMERIC(15,2), DATE '2026-03-10', DATE '2026-03-28'),

    -- Vinamilk campaigns (reuse existing brand)
    ('Vinamilk', 'bu-ne',                       'Vinamilk Ăn Ngon Uống Khoẻ',
        'Kết hợp review quán ăn với uống sữa Vinamilk sau bữa ăn — healthy lifestyle.',
        '2 TikTok food + drink videos',17000000.00::NUMERIC(15,2), DATE '2026-03-05', DATE '2026-03-25'),
    ('Vinamilk', 'hat-tieu-foodie',             'Vinamilk Ẩm Thực 3 Miền',
        'Series ẩm thực 3 miền kết hợp sản phẩm sữa Vinamilk tươi nguyên chất.',
        '3 TikTok food travel videos', 24000000.00::NUMERIC(15,2), DATE '2026-02-15', DATE '2026-03-10'),

    -- Shopee Vietnam campaigns (reuse existing brand)
    ('Shopee Vietnam', 'quynh-tran-ne',         'Shopee Live Cooking Show',
        'Livestream nấu ăn và giới thiệu nguyên liệu từ Shopee Fresh.',
        '2 Shopee Live sessions + 1 TikTok recap', 19000000.00::NUMERIC(15,2), DATE '2026-04-05', DATE '2026-04-20'),
    ('Shopee Vietnam', 'eatwhning',             'Shopee Đặc Sản Vùng Miền',
        'Giới thiệu đặc sản địa phương qua Shopee Mall — series 3 tập.',
        '3 TikTok videos',             21000000.00::NUMERIC(15,2), DATE '2026-03-20', DATE '2026-04-10'),
    ('Shopee Vietnam', 'simple-man',            'Shopee Gear Review',
        'Review đồ công nghệ, phụ kiện nam từ Shopee Mall theo góc nhìn minimalist.',
        '2 TikTok review shorts',      16000000.00::NUMERIC(15,2), DATE '2026-03-01', DATE '2026-03-22')
) AS m(brand_name, kol_slug, campaign_title, campaign_brief, deliverables, budget, start_date, end_date)
JOIN brand_profile bp ON bp.company_name = m.brand_name
JOIN kol_profile   kp ON kp.slug         = m.kol_slug;

-- ---------------------------------------------------------------------
-- 4) Reviews — BRAND_TO_KOL (12 reviews, 1 per booking)
-- ---------------------------------------------------------------------
INSERT INTO review (booking_id, author_id, target_id, direction, rating, comment)
SELECT b.id, brand_u.id, kol_u.id, 'BRAND_TO_KOL', r.rating, r.comment
FROM (VALUES
    ('Highlands Kết Nối Gia Đình',    5::SMALLINT, 'Nội dung ấm áp, chân thực — thể hiện đúng tinh thần "kết nối" của Highlands. Lượt xem và interaction vượt kỳ vọng.'),
    ('Highlands Date Night',           5::SMALLINT, 'Couple content rất cuốn, không gian quán được thể hiện đẹp, phù hợp target audience 20-30 tuổi.'),
    ('Highlands Morning Ritual',       4::SMALLINT, 'Lifestyle content tự nhiên, cà phê xuất hiện hợp lý. Một số clip ánh sáng chưa đều nhưng overall ổn.'),
    ('Highlands Menu Mới 2026',        5::SMALLINT, 'Review thẳng thắn và trung thực — đúng tone honest review. Nhiều khách đến quán nhắc đến video này.'),
    ('Dove Skincare Routine Challenge',5::SMALLINT, 'Sản phẩm được demo đúng cách, reach Gen Z female chính xác, bình luận tích cực áp đảo. Sẽ tiếp tục hợp tác.'),
    ('Sunsilk Tóc Đẹp Mỗi Ngày',     4::SMALLINT, 'Hướng dẫn chăm sóc tóc chi tiết, cảm nhận sản phẩm thật. Nên rút ngắn intro để giữ watch time tốt hơn.'),
    ('Clear Anti-Dandruff Campaign',   4::SMALLINT, 'Chia sẻ chân thực, đúng insight người dùng. Thumbnail cần bắt mắt hơn để tăng CTR.'),
    ('Vinamilk Ăn Ngon Uống Khoẻ',   5::SMALLINT, 'Kết hợp review ăn và uống sữa rất tự nhiên, không gượng ép. Traffic về web Vinamilk tăng rõ sau campaign.'),
    ('Vinamilk Ẩm Thực 3 Miền',      5::SMALLINT, 'Visual food cực đẹp, brand mention đủ mà không chiếm spotlight. Một trong những collab ấn tượng nhất Q1.'),
    ('Shopee Live Cooking Show',       4::SMALLINT, 'Livestream tương tác tốt, conversion rate đạt 80% KPI. Cần tối ưu thêm phần giới thiệu link sản phẩm.'),
    ('Shopee Đặc Sản Vùng Miền',     5::SMALLINT, 'Series chất lượng cao, storytelling đặc sản vùng miền sáng tạo, tỉ lệ click vào Shopee Mall vượt trội.'),
    ('Shopee Gear Review',             4::SMALLINT, 'Review khách quan, rõ ràng theo phong cách minimalist. Sản phẩm được highlight tốt. Rất phù hợp target male 18-30.')
) AS r(campaign_title, rating, comment)
JOIN booking      b         ON b.campaign_title   = r.campaign_title
JOIN brand_profile bp       ON bp.id              = b.brand_profile_id
JOIN kol_profile   kp       ON kp.id              = b.kol_profile_id
JOIN app_user      brand_u  ON brand_u.id         = bp.user_id
JOIN app_user      kol_u    ON kol_u.id           = kp.user_id;

-- ---------------------------------------------------------------------
-- 5) Recompute kol_profile aggregates — chỉ V14 KOL vừa nhận review.
--    Lọc bằng email LIKE '%@kols-koc.imported' để không đụng V8 KOLs.
-- ---------------------------------------------------------------------
UPDATE kol_profile k SET
    avg_rating   = stats.avg_rating,
    review_count = stats.review_count,
    updated_at   = NOW()
FROM (
    SELECT
        r.target_id                              AS kol_user_id,
        ROUND(AVG(r.rating)::NUMERIC, 2)          AS avg_rating,
        COUNT(*)::INTEGER                         AS review_count
    FROM review r
    JOIN app_user au ON au.id = r.target_id
    WHERE r.direction = 'BRAND_TO_KOL'
      AND au.email LIKE '%@kols-koc.imported'
    GROUP BY r.target_id
) stats
WHERE k.user_id = stats.kol_user_id;
