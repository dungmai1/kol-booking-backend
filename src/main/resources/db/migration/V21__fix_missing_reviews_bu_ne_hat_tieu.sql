-- =====================================================================
-- V21 — Fix 2 V14 KOL (bu-ne, hat-tieu-foodie) thiếu review sau V20.
--
-- Trong V20 các booking dùng brand 'Vinamilk' cho bu-ne và hat-tieu-foodie
-- không được insert (JOIN trả về 0 rows). Các KOL khác hoạt động bình thường.
-- Migration này dùng brand 'Highlands Coffee' (tạo trong V20) để tạo lại
-- booking + review cho 2 KOL còn thiếu.
-- =====================================================================

-- 1) Booking COMPLETED cho 2 KOL còn thiếu review
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
    ('Highlands Coffee', 'bu-ne',         'Highlands Quán Ăn Vỉa Hè 2026',
        'Khám phá quán ăn vỉa hè Sài Gòn kết hợp cà phê Highlands take-away.',
        '2 TikTok food + lifestyle videos', 13000000.00::NUMERIC(15,2), DATE '2026-04-10', DATE '2026-04-28'),
    ('Highlands Coffee', 'hat-tieu-foodie', 'Highlands Food & Coffee Series',
        'Series kết hợp ẩm thực đặc sắc với cà phê Highlands trên khắp 3 miền.',
        '3 TikTok food travel videos',      16000000.00::NUMERIC(15,2), DATE '2026-04-15', DATE '2026-05-05')
) AS m(brand_name, kol_slug, campaign_title, campaign_brief, deliverables, budget, start_date, end_date)
JOIN brand_profile bp ON bp.company_name = m.brand_name
JOIN kol_profile   kp ON kp.slug         = m.kol_slug;

-- 2) Reviews BRAND_TO_KOL cho 2 booking vừa tạo
INSERT INTO review (booking_id, author_id, target_id, direction, rating, comment)
SELECT b.id, brand_u.id, kol_u.id, 'BRAND_TO_KOL', r.rating, r.comment
FROM (VALUES
    ('Highlands Quán Ăn Vỉa Hè 2026',  5::SMALLINT, 'Nội dung food vỉa hè chân thực, cà phê Highlands xuất hiện tự nhiên giữa bối cảnh Sài Gòn sống động. Reach trẻ rất tốt.'),
    ('Highlands Food & Coffee Series',   5::SMALLINT, 'Visual food đẹp mắt, hành trình 3 miền cuốn hút, thương hiệu Highlands được lồng ghép khéo léo mà không gượng ép.')
) AS r(campaign_title, rating, comment)
JOIN booking      b         ON b.campaign_title  = r.campaign_title
JOIN brand_profile bp       ON bp.id             = b.brand_profile_id
JOIN kol_profile   kp       ON kp.id             = b.kol_profile_id
JOIN app_user      brand_u  ON brand_u.id        = bp.user_id
JOIN app_user      kol_u    ON kol_u.id          = kp.user_id;

-- 3) Recompute aggregates cho đúng 2 KOL này
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
      AND kp.slug IN ('bu-ne', 'hat-tieu-foodie')
    GROUP BY r.target_id
) stats
WHERE k.user_id = stats.kol_user_id;
