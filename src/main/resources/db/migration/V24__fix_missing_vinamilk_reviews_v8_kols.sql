-- =====================================================================
-- V24 — Fix 4 V8 KOL thiếu review sau V22.
--
-- Trong V22, 4 campaign dùng brand 'Vinamilk' cho các V8 KOL
-- (hoang-duyen, hoang-soi, tebefood, tuan-di-dau) không được insert
-- — cùng pattern đã xảy ra với V14 KOLs trong V20/V22 (fix ở V21/V23).
--
-- Vinamilk JOIN silently trả về 0 rows với một số KOL — nguyên nhân
-- chưa xác định rõ. Workaround: thay brand 'Vinamilk' bằng các brand
-- khác đã hoạt động tốt (Unilever Vietnam, Highlands Coffee, TCH, Biti's).
--
-- Rating giữ nguyên với rating Vinamilk ban đầu dự kiến trong V22
-- để đảm bảo avg_rating cuối cùng khớp với kỳ vọng ban đầu:
--   hoang-duyen : +4★ → tổng 5 reviews, avg 4.60
--   hoang-soi   : +4★ → tổng 4 reviews, avg 3.50
--   tebefood    : +5★ → tổng 5 reviews, avg 4.80
--   tuan-di-dau : +4★ → tổng 4 reviews, avg 4.25
-- =====================================================================

-- 1) Booking COMPLETED cho 4 KOL còn thiếu review
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
    ('Unilever Vietnam', 'hoang-duyen', 'Dove Beauty Inside Out',
        'Campaign sắc đẹp từ bên trong với Dove — beauty kết hợp lifestyle sáng sớm.',
        '2 TikTok + 1 IG reel',              22000000.00::NUMERIC(15,2),
        DATE '2026-05-05', DATE '2026-05-22'),
    ('Highlands Coffee', 'hoang-soi',   'Highlands Active Boost',
        'Năng lượng thể thao từ cà phê Highlands — khởi đầu ngày mới theo phong cách fitness.',
        '3 TikTok fitness + lifestyle clips', 16000000.00::NUMERIC(15,2),
        DATE '2026-04-01', DATE '2026-04-18'),
    ('The Coffee House', 'tebefood',    'TCH Chef Secret Menu',
        'Bí quyết bếp núc kết hợp đồ uống The Coffee House — ẩm thực sáng tạo từ nguyên liệu tươi.',
        '3 TikTok cooking + lifestyle videos',20000000.00::NUMERIC(15,2),
        DATE '2026-04-08', DATE '2026-04-25'),
    ('Biti''s',          'tuan-di-dau', 'Biti''s Da Nang Wander',
        'Hành trình phiêu lưu Đà Nẵng cùng giày Biti''s — khám phá ẩm thực và danh thắng địa phương.',
        '2 TikTok food travel vlogs',         14000000.00::NUMERIC(15,2),
        DATE '2026-04-05', DATE '2026-04-20')
) AS m(brand_name, kol_slug, campaign_title, campaign_brief, deliverables, budget, start_date, end_date)
JOIN brand_profile bp ON bp.company_name = m.brand_name
JOIN kol_profile   kp ON kp.slug         = m.kol_slug;

-- 2) Reviews BRAND_TO_KOL cho 4 booking vừa tạo
INSERT INTO review (booking_id, author_id, target_id, direction, rating, comment)
SELECT b.id, brand_u.id, kol_u.id, 'BRAND_TO_KOL', r.rating, r.comment
FROM (VALUES
    ('Dove Beauty Inside Out',  4::SMALLINT, 'Thông điệp sắc đẹp từ bên trong lan toả tốt, sản phẩm Dove được đưa vào content hợp lý. Interaction rate tốt, sẽ collab thêm trong Q3.'),
    ('Highlands Active Boost',  4::SMALLINT, 'Nội dung thể lực kết hợp cà phê khởi đầu ngày mới phù hợp thông điệp brand. Cần đa dạng setting quay hơn để tránh đơn điệu trong lần sau.'),
    ('TCH Chef Secret Menu',    5::SMALLINT, 'Công thức nấu ăn sáng tạo kết hợp đồ uống TCH, visual food cực đẹp và hấp dẫn. Tỷ lệ save video cao bất thường — nội dung chất lượng!'),
    ('Biti''s Da Nang Wander',  4::SMALLINT, 'Hành trình Đà Nẵng kết hợp giày Biti''s tự nhiên, đậm bản sắc ẩm thực địa phương. Reach tệp khách du lịch và food lover rất tốt.')
) AS r(campaign_title, rating, comment)
JOIN booking       b        ON b.campaign_title  = r.campaign_title
JOIN brand_profile bp       ON bp.id             = b.brand_profile_id
JOIN kol_profile   kp       ON kp.id             = b.kol_profile_id
JOIN app_user      brand_u  ON brand_u.id        = bp.user_id
JOIN app_user      kol_u    ON kol_u.id          = kp.user_id;

-- 3) Recompute aggregates cho đúng 4 KOL này
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
      AND kp.slug IN ('hoang-duyen', 'hoang-soi', 'tebefood', 'tuan-di-dau')
    GROUP BY r.target_id
) stats
WHERE k.user_id = stats.kol_user_id;
