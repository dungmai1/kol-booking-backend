-- =====================================================================
-- V33 — Seed bookings + reviews cho KOL @m.thang3001 (Koi)
--
-- Phụ thuộc: V32 (kol slug m-thang3001), V15/V20 (brand users)
-- Join brand qua email seed để tránh lệch tên company_name giữa môi trường.
-- =====================================================================

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
JOIN kol_profile   kp ON kp.slug = m.kol_slug;

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
WHERE kp.slug = 'm-thang3001';

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
WHERE kp.slug = 'm-thang3001';

UPDATE kol_profile k SET
    avg_rating   = stats.avg_rating,
    review_count = stats.review_count,
    updated_at   = NOW()
FROM (
    SELECT
        r.target_id                      AS kol_user_id,
        ROUND(AVG(r.rating)::NUMERIC, 2) AS avg_rating,
        COUNT(*)::INTEGER                AS review_count
    FROM review r
    JOIN kol_profile kp ON kp.user_id = r.target_id
    WHERE r.direction = 'BRAND_TO_KOL'
      AND kp.slug = 'm-thang3001'
    GROUP BY r.target_id
) stats
WHERE k.user_id = stats.kol_user_id;
