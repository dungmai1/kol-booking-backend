-- =====================================================================
-- V23 — Fix 7 V14 KOL thiếu review sau V22.
--
-- Trong V22, 7 campaign dùng brand 'Vinamilk' cho các V14 KOL
-- (duc-va-ly, dumi, eatwhning, hat-tieu-foodie, hong-ngoc-van,
--  nhan-phuong-chi-xu, simple-man) không được insert — cùng pattern
-- đã xảy ra trong V20 với bu-ne / hat-tieu-foodie (đã fix ở V21).
--
-- Workaround: thay brand 'Vinamilk' bằng các brand khác đã hoạt động
-- tốt với V14 KOL (Highlands Coffee, The Coffee House, Unilever Vietnam).
--
-- Rating giữ nguyên với rating Vinamilk ban đầu dự kiến để đảm bảo
-- avg_rating cuối cùng khớp với kỳ vọng của V22:
--   duc-va-ly        : +4★ → avg 4.75 (count 4)
--   dumi             : +5★ → avg 4.00 (count 4)
--   eatwhning        : +4★ → avg 4.75 (count 4)
--   hat-tieu-foodie  : +5★ → avg 4.75 (count 4)
--   hong-ngoc-van    : +3★ → avg 3.75 (count 4)
--   nhan-phuong-chi-xu: +5★ → avg 4.75 (count 4)
--   simple-man       : +4★ → avg 3.75 (count 4)
-- =====================================================================

-- 1) Booking COMPLETED cho 7 KOL còn thiếu review
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
    ('Unilever Vietnam',    'duc-va-ly',             'Dove Shine Together',
        'Campaign couple content kết hợp sản phẩm Dove — chăm sóc da đôi trong cuộc sống hàng ngày.',
        '2 TikTok couple vlogs + 1 IG reel',        20000000.00::NUMERIC(15,2), DATE '2026-04-20', DATE '2026-05-10'),
    ('Highlands Coffee',    'dumi',                  'Highlands Creative Break',
        'Series creative lifestyle — khoảng lặng sáng tạo cùng cà phê Highlands sau giờ làm việc.',
        '3 TikTok lifestyle videos',                 14000000.00::NUMERIC(15,2), DATE '2026-04-25', DATE '2026-05-12'),
    ('Highlands Coffee',    'eatwhning',             'Highlands Food Discovery',
        'Khám phá ẩm thực địa phương mỗi vùng miền kết hợp cà phê Highlands take-away.',
        '2 TikTok food + lifestyle videos',          17000000.00::NUMERIC(15,2), DATE '2026-04-18', DATE '2026-05-05'),
    ('The Coffee House',    'hat-tieu-foodie',       'TCH Foodie Hunt',
        'Series review quán ăn ngon Sài Gòn kết hợp thức uống The Coffee House theo chủ đề.',
        '3 TikTok food review videos',               16000000.00::NUMERIC(15,2), DATE '2026-04-22', DATE '2026-05-08'),
    ('Highlands Coffee',    'hong-ngoc-van',         'Highlands Morning Radiance',
        'Skincare morning routine kết hợp ly cà phê Highlands — lifestyle content sáng sớm.',
        '2 TikTok beauty + lifestyle shorts',        12000000.00::NUMERIC(15,2), DATE '2026-04-15', DATE '2026-05-01'),
    ('Unilever Vietnam',    'nhan-phuong-chi-xu',    'Dove Family Care',
        'Campaign chăm sóc gia đình với sản phẩm Dove — nội dung ấm áp, gần gũi về mẹ và bé.',
        '2 TikTok family videos + 1 IG carousel',   23000000.00::NUMERIC(15,2), DATE '2026-04-28', DATE '2026-05-15'),
    ('Highlands Coffee',    'simple-man',            'Highlands Simple Start',
        'Chuỗi morning routine tối giản — bắt đầu ngày mới đúng cách với cà phê Highlands.',
        '2 TikTok minimalist lifestyle shorts',      13000000.00::NUMERIC(15,2), DATE '2026-04-10', DATE '2026-04-28')
) AS m(brand_name, kol_slug, campaign_title, campaign_brief, deliverables, budget, start_date, end_date)
JOIN brand_profile bp ON bp.company_name = m.brand_name
JOIN kol_profile   kp ON kp.slug         = m.kol_slug;

-- 2) Reviews BRAND_TO_KOL cho 7 booking vừa tạo
INSERT INTO review (booking_id, author_id, target_id, direction, rating, comment)
SELECT b.id, brand_u.id, kol_u.id, 'BRAND_TO_KOL', r.rating, r.comment
FROM (VALUES
    ('Dove Shine Together',          4::SMALLINT, 'Couple content Dove chân thực, thông điệp chăm sóc đôi được truyền tải tốt. Interaction rate vượt KPI 15%.'),
    ('Highlands Creative Break',     5::SMALLINT, 'Nội dung sáng tạo độc đáo, không khí Highlands được lồng ghép mượt mà. Tương tác cực tốt từ cộng đồng Gen Z sáng tạo.'),
    ('Highlands Food Discovery',     4::SMALLINT, 'Food content chất lượng cao, thương hiệu Highlands xuất hiện tự nhiên. Engagement tốt, phù hợp đúng target F&B audience.'),
    ('TCH Foodie Hunt',              5::SMALLINT, 'Series review ẩm thực cuốn hút, The Coffee House được gắn kết khéo léo vào từng tập. Brand recall sau campaign tăng rõ rệt.'),
    ('Highlands Morning Radiance',   3::SMALLINT, 'Concept kết hợp skincare và cà phê thú vị nhưng cần tập trung hơn vào brand highlight. Reach ổn, conversion thấp hơn kỳ vọng.'),
    ('Dove Family Care',             5::SMALLINT, 'Nội dung gia đình ấm áp, thông điệp Dove Family Care truyền đạt đúng insight. Video đạt organic reach cao nhất tháng 5.'),
    ('Highlands Simple Start',       4::SMALLINT, 'Minimalist lifestyle content phù hợp brand tone của Highlands. Cộng đồng phản hồi tích cực, watch-through rate đạt 68%.')
) AS r(campaign_title, rating, comment)
JOIN booking       b        ON b.campaign_title  = r.campaign_title
JOIN brand_profile bp       ON bp.id             = b.brand_profile_id
JOIN kol_profile   kp       ON kp.id             = b.kol_profile_id
JOIN app_user      brand_u  ON brand_u.id        = bp.user_id
JOIN app_user      kol_u    ON kol_u.id          = kp.user_id;

-- 3) Recompute aggregates cho đúng 7 KOL này
--    avg_rating và review_count được tính lại từ bảng review thực tế.
UPDATE kol_profile k SET
    avg_rating   = stats.avg_rating,
    review_count = stats.review_count,
    updated_at   = NOW()
FROM (
    SELECT
        r.target_id                          AS kol_user_id,
        ROUND(AVG(r.rating)::NUMERIC, 2)     AS avg_rating,
        COUNT(*)::INTEGER                    AS review_count
    FROM review r
    JOIN kol_profile kp ON kp.user_id = r.target_id
    WHERE r.direction = 'BRAND_TO_KOL'
      AND kp.slug IN (
          'duc-va-ly', 'dumi', 'eatwhning', 'hat-tieu-foodie',
          'hong-ngoc-van', 'nhan-phuong-chi-xu', 'simple-man'
      )
    GROUP BY r.target_id
) stats
WHERE k.user_id = stats.kol_user_id;
