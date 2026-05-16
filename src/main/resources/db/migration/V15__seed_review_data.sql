-- =====================================================================
-- V14 — Seed Brand profiles, completed Bookings, and Reviews
--
-- Mục tiêu:
--   1. Tạo dữ liệu mẫu cho bảng `review` (cả hai chiều BRAND_TO_KOL và
--      KOL_TO_BRAND) — phục vụ test API reviews + thuật toán recompute
--      avg_rating / review_count trên kol_profile.
--   2. Để insert được review, bắt buộc phải có booking ở trạng thái COMPLETED
--      (xem ReviewService#create). Tạo kèm Brand profiles + Bookings.
--
-- Phụ thuộc:
--   - V2 (app_user)
--   - V3 (brand_profile, kol_profile)
--   - V5 (booking)
--   - V7 (review)
--   - V8 (KOL seed — tra qua slug, KHÔNG hardcode id)
--   - V11.1 (booking.deliverables = TEXT)
--   - V12 (kol_profile aggregates)
--
-- Password hash: bcrypt cho "password123" (verified) — dùng đúng chuỗi đã
-- được V11.2 fix cho mọi seed user, tránh future migration phải patch lại.
-- Unique constraint cần tôn trọng:
--   - app_user.email UNIQUE
--   - kol_profile.slug UNIQUE
--   - review (booking_id, direction) UNIQUE
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1) Brand users (role = BRAND, ACTIVE, email_verified = TRUE)
-- ---------------------------------------------------------------------
INSERT INTO app_user (email, password_hash, role, status, email_verified) VALUES
    ('brand.vinamilk@seed.local', '$2a$10$ZJo6Pxi.C7ichgif9MD6DOZCjfn3AxLh2q18qP39T5qHtxF3u9d7C', 'BRAND', 'ACTIVE', TRUE),
    ('brand.shopee@seed.local',   '$2a$10$ZJo6Pxi.C7ichgif9MD6DOZCjfn3AxLh2q18qP39T5qHtxF3u9d7C', 'BRAND', 'ACTIVE', TRUE),
    ('brand.tch@seed.local',      '$2a$10$ZJo6Pxi.C7ichgif9MD6DOZCjfn3AxLh2q18qP39T5qHtxF3u9d7C', 'BRAND', 'ACTIVE', TRUE),
    ('brand.bitis@seed.local',    '$2a$10$ZJo6Pxi.C7ichgif9MD6DOZCjfn3AxLh2q18qP39T5qHtxF3u9d7C', 'BRAND', 'ACTIVE', TRUE);

-- ---------------------------------------------------------------------
-- 2) Brand profiles (status = APPROVED) — join về app_user qua email
-- ---------------------------------------------------------------------
INSERT INTO brand_profile (user_id, company_name, industry, contact_name, status)
SELECT u.id, m.company_name, m.industry, m.contact_name, 'APPROVED'
FROM (VALUES
    ('brand.vinamilk@seed.local', 'Vinamilk',         'FMCG / Sữa',         'Nguyễn Văn Vinamilk'),
    ('brand.shopee@seed.local',   'Shopee Vietnam',   'E-commerce',         'Trần Thị Shopee'),
    ('brand.tch@seed.local',      'The Coffee House', 'F&B',                'Lê Minh Coffee'),
    ('brand.bitis@seed.local',    'Biti''s',          'Fashion / Footwear', 'Phạm Quang Bitis')
) AS m(email, company_name, industry, contact_name)
JOIN app_user u ON u.email = m.email;

-- ---------------------------------------------------------------------
-- 3) Bookings (status = COMPLETED — điều kiện cho phép review)
--    Tra brand_profile qua company_name (vừa seed ở bước 2),
--    tra kol_profile qua slug (đã seed ở V8).
--    campaign_title được giữ duy nhất để bước 4 lookup booking lại được.
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
    ('Vinamilk',         'nguyen-thi-thanh-nha', 'Vinamilk Athlete Drive 2026',  'Lan toả thông điệp dinh dưỡng cho vận động viên trẻ qua TikTok.',                     '2 TikTok videos + 1 IG reel',  18000000.00::NUMERIC(15,2), DATE '2026-03-10', DATE '2026-03-25'),
    ('Vinamilk',         'lam-bao-ngoc',         'Vinamilk Voice of Tet',        'Music collab dịp Tết với chủ đề "Nhà mình là Vinamilk".',                              '1 TikTok music video',         32000000.00::NUMERIC(15,2), DATE '2026-01-15', DATE '2026-02-05'),
    ('Vinamilk',         'phuong-di-dau',        'Vinamilk On The Road',         'Travel series uống sữa khắp Việt Nam, kết hợp khám phá vùng cao.',                     '3 TikTok travel vlogs',        25000000.00::NUMERIC(15,2), DATE '2026-02-20', DATE '2026-03-15'),
    ('Shopee Vietnam',   'tien-tien',            'Shopee 3.3 Sale Anthem',       'Sáng tác đoạn nhạc theme cho campaign sale 3/3.',                                       '1 TikTok music + 2 short cuts',45000000.00::NUMERIC(15,2), DATE '2026-02-15', DATE '2026-03-03'),
    ('Shopee Vietnam',   'hoang-duyen',          'Shopee 4.4 Beauty Box',        'Unbox + thử makeup từ Shopee Mall.',                                                    '2 TikTok unboxing',            22000000.00::NUMERIC(15,2), DATE '2026-03-25', DATE '2026-04-04'),
    ('Shopee Vietnam',   'hoang-soi',            'Shopee Daily Pick Review',     'Review FMCG hàng ngày trên TikTok.',                                                    '5 TikTok review shorts',       15000000.00::NUMERIC(15,2), DATE '2026-03-01', DATE '2026-04-01'),
    ('Shopee Vietnam',   'tebefood',             'Shopee FoodFest',              'Series review món quốc tế dùng phụ kiện bếp từ Shopee.',                               '3 TikTok video',               20000000.00::NUMERIC(15,2), DATE '2026-04-05', DATE '2026-04-25'),
    ('The Coffee House', 'phuong-di-dau',        'TCH Road Trip',                'Travel + cà phê take-away trên cung đường mô-tô.',                                      '2 TikTok travel videos',       17000000.00::NUMERIC(15,2), DATE '2026-02-01', DATE '2026-02-20'),
    ('The Coffee House', 'tebefood',             'TCH Pastry Pairing',           'Review món bánh mới của TCH theo bộ menu mùa xuân.',                                    '2 TikTok review',              14000000.00::NUMERIC(15,2), DATE '2026-03-12', DATE '2026-03-28'),
    ('The Coffee House', 'tuan-di-dau',          'TCH Da Nang Opening',          'Khai trương TCH chi nhánh Đà Nẵng — vlog trải nghiệm.',                                 '1 TikTok vlog + 1 IG post',    12000000.00::NUMERIC(15,2), DATE '2026-03-20', DATE '2026-04-02'),
    ('Biti''s',          'hoang-duyen',          'Bitis Hunter Spring 2026',     'Thử BST giày Hunter Spring qua look thường nhật.',                                      '2 TikTok lookbook',            28000000.00::NUMERIC(15,2), DATE '2026-03-08', DATE '2026-03-28'),
    ('Biti''s',          'hoang-soi',            'Bitis Daily Combat',           'Review độ bền giày Biti''s Combat trong sinh hoạt thường ngày.',                        '3 TikTok review',              16000000.00::NUMERIC(15,2), DATE '2026-02-25', DATE '2026-03-20')
) AS m(brand_name, kol_slug, campaign_title, campaign_brief, deliverables, budget, start_date, end_date)
JOIN brand_profile bp ON bp.company_name = m.brand_name
JOIN kol_profile kp   ON kp.slug         = m.kol_slug;

-- ---------------------------------------------------------------------
-- 4a) Reviews — BRAND_TO_KOL (12 reviews, 1 per booking)
--     author_id = brand user, target_id = kol user
-- ---------------------------------------------------------------------
INSERT INTO review (booking_id, author_id, target_id, direction, rating, comment)
SELECT b.id, brand_u.id, kol_u.id, 'BRAND_TO_KOL', r.rating, r.comment
FROM (VALUES
    ('Vinamilk Athlete Drive 2026',  5::SMALLINT, 'KOL chuyên nghiệp, đúng deadline, hình ảnh khoẻ khoắn phù hợp tone Vinamilk.'),
    ('Vinamilk Voice of Tet',        5::SMALLINT, 'Giọng hát truyền cảm, sản phẩm âm nhạc đẹp, brand mention được lồng ghép tự nhiên.'),
    ('Vinamilk On The Road',         4::SMALLINT, 'Nội dung travel sáng tạo, một số shot cần chỉnh sáng tốt hơn nhưng tổng thể ổn.'),
    ('Shopee 3.3 Sale Anthem',       5::SMALLINT, 'Theme song bắt tai, trend lan rộng, đẩy traffic về app Shopee rất tốt.'),
    ('Shopee 4.4 Beauty Box',        5::SMALLINT, 'Unbox tinh tế, reach Gen Z rất tốt, engagement vượt KPI cam kết.'),
    ('Shopee Daily Pick Review',     4::SMALLINT, 'Review thẳng thắn, đáng tin, tuy nhiên tần suất ra bài có thể đều hơn.'),
    ('Shopee FoodFest',              5::SMALLINT, 'Visual chỉn chu, food porn đỉnh, conversion về Shopee Mall ấn tượng.'),
    ('TCH Road Trip',                5::SMALLINT, 'Travel content giàu cảm xúc, take-away cà phê được lồng ghép rất tự nhiên.'),
    ('TCH Pastry Pairing',           5::SMALLINT, 'Quay đẹp, mô tả vị ngon dễ hình dung, lượng order online tăng rõ sau campaign.'),
    ('TCH Da Nang Opening',          4::SMALLINT, 'Reach Đà Nẵng tốt, nội dung đầu video hơi dài — cần trim mở bài lần sau.'),
    ('Bitis Hunter Spring 2026',     5::SMALLINT, 'Look styling trẻ trung, đúng concept Spring, sales lookbook tăng rõ rệt.'),
    ('Bitis Daily Combat',           3::SMALLINT, 'Nội dung review chân thực nhưng feel slot quảng cáo còn lộ, cần tự nhiên hơn.')
) AS r(campaign_title, rating, comment)
JOIN booking b         ON b.campaign_title    = r.campaign_title
JOIN brand_profile bp  ON bp.id               = b.brand_profile_id
JOIN kol_profile kp    ON kp.id               = b.kol_profile_id
JOIN app_user brand_u  ON brand_u.id          = bp.user_id
JOIN app_user kol_u    ON kol_u.id            = kp.user_id;

-- ---------------------------------------------------------------------
-- 4b) Reviews — KOL_TO_BRAND (8 reviews — không phải KOL nào cũng review lại)
--     author_id = kol user, target_id = brand user
-- ---------------------------------------------------------------------
INSERT INTO review (booking_id, author_id, target_id, direction, rating, comment)
SELECT b.id, kol_u.id, brand_u.id, 'KOL_TO_BRAND', r.rating, r.comment
FROM (VALUES
    ('Vinamilk Athlete Drive 2026',  5::SMALLINT, 'Brief rõ ràng, brand team hỗ trợ tận tình, thanh toán đúng hạn.'),
    ('Vinamilk Voice of Tet',        5::SMALLINT, 'Brand cho không gian sáng tạo, feedback nhanh và mang tính xây dựng.'),
    ('Shopee 3.3 Sale Anthem',       5::SMALLINT, 'Quy trình duyệt brief gọn, brand team chuyên nghiệp, deadline thực tế.'),
    ('Shopee 4.4 Beauty Box',        5::SMALLINT, 'Sản phẩm gửi đúng lịch, brief chi tiết, kế hoạch booster post chu đáo.'),
    ('Shopee FoodFest',              4::SMALLINT, 'Nhìn chung tốt, mong brand cải thiện timeline duyệt video ở lần sau.'),
    ('TCH Road Trip',                5::SMALLINT, 'Sản phẩm phù hợp lifestyle, không gò ép concept, sẵn sàng hợp tác tiếp.'),
    ('TCH Pastry Pairing',           5::SMALLINT, 'Brand chu đáo, gửi sample đa dạng, đoàn quay hỗ trợ tận tâm.'),
    ('Bitis Hunter Spring 2026',     4::SMALLINT, 'Sản phẩm chất lượng, một số lookbook giao hơi sát deadline cần buffer hơn.')
) AS r(campaign_title, rating, comment)
JOIN booking b         ON b.campaign_title    = r.campaign_title
JOIN brand_profile bp  ON bp.id               = b.brand_profile_id
JOIN kol_profile kp    ON kp.id               = b.kol_profile_id
JOIN app_user kol_u    ON kol_u.id            = kp.user_id
JOIN app_user brand_u  ON brand_u.id          = bp.user_id;

-- ---------------------------------------------------------------------
-- 5) Recompute kol_profile aggregates từ BRAND_TO_KOL reviews vừa seed.
--    Khớp với logic ReviewService#recomputeKolRating (sum/count theo
--    target_id = kol user, direction = BRAND_TO_KOL, scale 2 HALF_UP).
--    Chỉ update các KOL có ít nhất 1 review để không reset KOL khác về 0.
-- ---------------------------------------------------------------------
UPDATE kol_profile k SET
    avg_rating   = stats.avg_rating,
    review_count = stats.review_count,
    updated_at   = NOW()
FROM (
    SELECT
        r.target_id                                            AS kol_user_id,
        ROUND(AVG(r.rating)::NUMERIC, 2)                       AS avg_rating,
        COUNT(*)::INTEGER                                      AS review_count
    FROM review r
    WHERE r.direction = 'BRAND_TO_KOL'
    GROUP BY r.target_id
) stats
WHERE k.user_id = stats.kol_user_id;
