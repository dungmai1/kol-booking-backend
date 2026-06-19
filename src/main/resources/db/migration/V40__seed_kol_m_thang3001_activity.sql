-- =====================================================================
-- V40 — Seed thống nhất: giá, booking, review cho KOL @m.thang3001 (Koi)
--
-- Gộp dữ liệu từ V32 (pricing/portfolio/categories) và V33 (bookings/reviews)
-- sau khi V39 recreate dev account nhưng chỉ giữ profile + channel cơ bản.
--
-- Kết quả mong đợi trên kol_profile (slug = m-thang3001):
--   min_price          = 3_000_000
--   avg_rating         = 4.60  (5 reviews BRAND_TO_KOL)
--   review_count       = 5
--   completed bookings = 5     (đếm runtime qua bảng booking, status COMPLETED)
--
-- Phụ thuộc: V15/V20 (brand seed), V39 (kol profile m-thang3001)
-- Idempotent: NOT EXISTS / ON CONFLICT DO NOTHING
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1) Pricing packages
-- ---------------------------------------------------------------------
INSERT INTO kol_pricing_package (kol_profile_id, type, platform, price, description)
SELECT kp.id, m.type, m.platform, m.price, m.description
FROM kol_profile kp
CROSS JOIN (VALUES
    ('VIDEO', 'TIKTOK', 3000000.00::NUMERIC(15,2),
     '1 video TikTok 30–60 giây, quay và đăng trên kênh @m.thang3001'),
    ('STORY', 'TIKTOK', 1500000.00::NUMERIC(15,2),
     '1 story TikTok 15 giây, mention brand tự nhiên'),
    ('VIDEO', 'INSTAGRAM', 3500000.00::NUMERIC(15,2),
     '1 Reels Instagram 30–60 giây, cross-post lifestyle/fitness')
) AS m(type, platform, price, description)
WHERE kp.slug = 'm-thang3001'
  AND NOT EXISTS (
      SELECT 1 FROM kol_pricing_package p
      WHERE p.kol_profile_id = kp.id
        AND p.type = m.type
        AND p.platform = m.platform
        AND p.price = m.price
  );

-- ---------------------------------------------------------------------
-- 2) Portfolio items
-- ---------------------------------------------------------------------
INSERT INTO kol_portfolio_item (kol_profile_id, title, media_url, media_type, campaign_name)
SELECT kp.id, v.title, v.media_url, 'VIDEO', v.campaign_name
FROM kol_profile kp
CROSS JOIN (VALUES
    ('Day 11 Hoàn thành — 30 ngày thay đổi bản thân', 'https://www.tiktok.com/@m.thang3001/video/7631296449514769684', 'Fitness challenge'),
    ('Buổi tối của người độc lập tình cảm', 'https://www.tiktok.com/@m.thang3001/video/7611216945299344660', 'Vlog đời sống'),
    ('Thức sớm nhất TikTok — bình minh', 'https://www.tiktok.com/@m.thang3001/video/7601665697478954261', 'Vlog buổi sáng'),
    ('Review trà sen vàng cùng Hồng Ngọc', 'https://www.tiktok.com/@m.thang3001/video/7597370056498056469', 'Review đồ uống'),
    ('Chạy rẽ khói là có thật', 'https://www.tiktok.com/@m.thang3001/video/7606174419240635669', 'Vlog viral')
) AS v(title, media_url, campaign_name)
WHERE kp.slug = 'm-thang3001'
  AND NOT EXISTS (
      SELECT 1 FROM kol_portfolio_item pi
      WHERE pi.kol_profile_id = kp.id AND pi.media_url = v.media_url
  );

-- ---------------------------------------------------------------------
-- 3) Categories
-- ---------------------------------------------------------------------
INSERT INTO kol_category (kol_profile_id, category_id)
SELECT kp.id, cat.id
FROM kol_profile kp
CROSS JOIN category cat
WHERE kp.slug = 'm-thang3001'
  AND cat.slug IN ('fitness', 'lifestyle')
  AND NOT EXISTS (
      SELECT 1 FROM kol_category kc
      WHERE kc.kol_profile_id = kp.id AND kc.category_id = cat.id
  );

-- ---------------------------------------------------------------------
-- 4) Completed bookings (lượt booking)
-- ---------------------------------------------------------------------
INSERT INTO booking (
    brand_profile_id, kol_profile_id, campaign_title, campaign_brief,
    deliverables, budget, start_date, end_date, status
)
SELECT
    bp.id, kp.id, m.campaign_title, m.campaign_brief,
    m.deliverables, m.budget, m.start_date, m.end_date, 'COMPLETED'
FROM (VALUES
    ('brand.vinamilk@seed.local', 'm-thang3001', 'Koi Vinamilk Fit Morning',
     'Series buổi sáng tập luyện kết hợp uống sữa Vinamilk — thông điệp năng lượng cho ngày mới.',
     '2 TikTok fitness vlogs + 1 IG story', 12000000.00::NUMERIC(15,2),
     DATE '2026-04-10', DATE '2026-04-28'),
    ('brand.tch@seed.local', 'm-thang3001', 'Koi TCH Night Vlog',
     'Vlog buổi tối chill tại The Coffee House — lifestyle độc lập, tự tin.',
     '2 TikTok vlog clips', 9000000.00::NUMERIC(15,2),
     DATE '2026-03-15', DATE '2026-04-01'),
    ('brand.shopee@seed.local', 'm-thang3001', 'Koi Shopee Gym Gear',
     'Review dụng cụ tập gym từ Shopee Mall — push-up, resistance band, phụ kiện fitness.',
     '3 TikTok review shorts', 10000000.00::NUMERIC(15,2),
     DATE '2026-04-20', DATE '2026-05-08'),
    ('brand.bitis@seed.local', 'm-thang3001', 'Koi Bitis Active Steps',
     'Test giày Biti''s trong lịch tập và vlog đi bộ hàng ngày — active lifestyle.',
     '2 TikTok test videos', 11000000.00::NUMERIC(15,2),
     DATE '2026-05-01', DATE '2026-05-18'),
    ('brand.highlands@seed.local', 'm-thang3001', 'Koi Highlands Sunrise Run',
     'Chạy bộ buổi sáng kết hợp cà phê Highlands — fitness meets lifestyle content.',
     '2 TikTok morning vlogs', 9500000.00::NUMERIC(15,2),
     DATE '2026-05-10', DATE '2026-05-25')
) AS m(brand_email, kol_slug, campaign_title, campaign_brief, deliverables, budget, start_date, end_date)
JOIN app_user brand_u ON brand_u.email = m.brand_email
JOIN brand_profile bp ON bp.user_id = brand_u.id
JOIN kol_profile   kp ON kp.slug = m.kol_slug
WHERE NOT EXISTS (
    SELECT 1 FROM booking eb
    WHERE eb.campaign_title = m.campaign_title
      AND eb.kol_profile_id = kp.id
);

-- ---------------------------------------------------------------------
-- 5) Reviews — BRAND_TO_KOL
-- ---------------------------------------------------------------------
INSERT INTO review (booking_id, author_id, target_id, direction, rating, comment)
SELECT b.id, brand_u.id, kol_u.id, 'BRAND_TO_KOL', r.rating, r.comment
FROM (VALUES
    ('Koi Vinamilk Fit Morning', 5::SMALLINT,
     'Nội dung tập luyện buổi sáng rất năng lượng, sữa Vinamilk được lồng ghép tự nhiên. Video dễ viral trong cộng đồng fitness.'),
    ('Koi TCH Night Vlog', 5::SMALLINT,
     'Vlog buổi tối chill, đúng tone lifestyle trẻ trung của TCH. Góc quay đẹp, brand mention không gượng ép.'),
    ('Koi Shopee Gym Gear', 4::SMALLINT,
     'Review dụng cụ gym chi tiết, link Shopee rõ ràng. Nên thêm demo bài tập cụ thể để tăng thời lượng xem.'),
    ('Koi Bitis Active Steps', 4::SMALLINT,
     'Test giày Biti''s trong vlog đi bộ chân thực, phù hợp target active young male. CTA mua hàng có thể mạnh hơn.'),
    ('Koi Highlands Sunrise Run', 5::SMALLINT,
     'Concept chạy bộ bình minh + cà phê Highlands rất cuốn. KOL giao hàng đúng deadline, nội dung sáng tạo.')
) AS r(campaign_title, rating, comment)
JOIN booking       b       ON b.campaign_title = r.campaign_title
JOIN brand_profile bp      ON bp.id            = b.brand_profile_id
JOIN kol_profile   kp      ON kp.id            = b.kol_profile_id
JOIN app_user      brand_u ON brand_u.id       = bp.user_id
JOIN app_user      kol_u   ON kol_u.id         = kp.user_id
WHERE kp.slug = 'm-thang3001'
ON CONFLICT ON CONSTRAINT uk_review_booking_direction DO NOTHING;

-- ---------------------------------------------------------------------
-- 6) Reviews — KOL_TO_BRAND
-- ---------------------------------------------------------------------
INSERT INTO review (booking_id, author_id, target_id, direction, rating, comment)
SELECT b.id, kol_u.id, brand_u.id, 'KOL_TO_BRAND', r.rating, r.comment
FROM (VALUES
    ('Koi Vinamilk Fit Morning', 5::SMALLINT, 'Brief rõ ràng, team hỗ trợ nhanh, thanh toán đúng hạn.'),
    ('Koi TCH Night Vlog', 5::SMALLINT, 'Brand linh hoạt về concept, feedback xây dựng, dễ hợp tác.'),
    ('Koi Shopee Gym Gear', 4::SMALLINT, 'Quy trình duyệt ổn, mong timeline gửi sản phẩm sớm hơn một chút.'),
    ('Koi Highlands Sunrise Run', 5::SMALLINT, 'Không gian Highlands đẹp, team on-set hỗ trợ tận tình.')
) AS r(campaign_title, rating, comment)
JOIN booking       b       ON b.campaign_title = r.campaign_title
JOIN brand_profile bp      ON bp.id            = b.brand_profile_id
JOIN kol_profile   kp      ON kp.id            = b.kol_profile_id
JOIN app_user      kol_u   ON kol_u.id         = kp.user_id
JOIN app_user      brand_u ON brand_u.id       = bp.user_id
WHERE kp.slug = 'm-thang3001'
ON CONFLICT ON CONSTRAINT uk_review_booking_direction DO NOTHING;

-- ---------------------------------------------------------------------
-- 7) Recompute denormalized aggregates trên kol_profile
--    (min_price, avg_rating, review_count, max_follower_count)
-- ---------------------------------------------------------------------
UPDATE kol_profile k SET
    min_price = COALESCE(
        (SELECT MIN(p.price) FROM kol_pricing_package p WHERE p.kol_profile_id = k.id),
        k.min_price
    ),
    max_follower_count = COALESCE(
        (SELECT MAX(c.follower_count) FROM kol_social_channel c WHERE c.kol_profile_id = k.id),
        k.max_follower_count
    ),
    avg_rating = COALESCE(stats.avg_rating, 0),
    review_count = COALESCE(stats.review_count, 0),
    updated_at = NOW()
FROM (
    SELECT
        kp.id AS kol_profile_id,
        ROUND(AVG(r.rating)::NUMERIC, 2) AS avg_rating,
        COUNT(*)::INTEGER                AS review_count
    FROM kol_profile kp
    LEFT JOIN review r
        ON r.target_id = kp.user_id
       AND r.direction = 'BRAND_TO_KOL'
    WHERE kp.slug = 'm-thang3001'
    GROUP BY kp.id
) stats
WHERE k.id = stats.kol_profile_id;
