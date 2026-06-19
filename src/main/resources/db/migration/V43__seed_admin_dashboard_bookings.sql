-- =====================================================================
-- V43 — Seed booking 2 tuần gần đây cho admin dashboard
--
-- Rải ~22 booking trong 14 ngày (created_at tương đối NOW()) với mix status:
--   COMPLETED, IN_PROGRESS, ACCEPTED, DELIVERED, PENDING
-- Kèm FEE wallet_transaction cho booking COMPLETED → biểu đồ doanh thu.
--
-- Idempotent: campaign_title prefix "Dash Seed " + NOT EXISTS
-- Phụ thuộc: V15/V20 (brand seed), V42 (KOL APPROVED + pricing)
-- =====================================================================

INSERT INTO booking (
    brand_profile_id, kol_profile_id, campaign_title, campaign_brief,
    deliverables, budget, start_date, end_date, status,
    platform_fee_percent, platform_fee_amount, kol_net_amount,
    brand_company_name, kol_display_name,
    created_at, updated_at
)
SELECT
    bp.id,
    kp.id,
    m.campaign_title,
    m.campaign_brief,
    m.deliverables,
    m.budget,
    (CURRENT_DATE + m.start_offset)::DATE,
    (CURRENT_DATE + m.end_offset)::DATE,
    m.status,
    10.00,
    CASE WHEN m.status = 'COMPLETED'
         THEN ROUND(m.budget * 0.10, 2) END,
    CASE WHEN m.status = 'COMPLETED'
         THEN m.budget - ROUND(m.budget * 0.10, 2) END,
    bp.company_name,
    kp.display_name,
    NOW() - (m.days_ago || ' days')::INTERVAL - (m.hour_offset || ' hours')::INTERVAL,
    NOW() - (m.days_ago || ' days')::INTERVAL - (m.hour_offset || ' hours')::INTERVAL
        + INTERVAL '2 hours'
FROM (VALUES
    -- 13–8 ngày trước: chủ yếu COMPLETED
    ('brand.vinamilk@seed.local',   'hat-tieu-foodie',      'Dash Seed D13 Vinamilk Foodie Morning',
     'Series TikTok review sữa buổi sáng cùng Hạt Tiêu Foodie.', '2 TikTok review', 18000000.00::NUMERIC(15,2),
     13, 8, 7, 21, 'COMPLETED'),
    ('brand.shopee@seed.local',     'bu-ne',                'Dash Seed D13 Shopee Saigon Eats',
     'Review quán ăn Sài Gòn qua Shopee Food.', '3 TikTok shorts', 12000000.00::NUMERIC(15,2),
     13, 10, 5, 18, 'COMPLETED'),
    ('brand.tch@seed.local',        'dumi',                 'Dash Seed D12 TCH Pastry Week',
     'Thử bánh mới TCH — content lifestyle.', '2 TikTok + 1 story', 14000000.00::NUMERIC(15,2),
     12, 9, 3, 17, 'COMPLETED'),
    ('brand.bitis@seed.local',      'eatwhning',            'Dash Seed D12 Bitis Urban Walk',
     'Test giày Biti''s trong vlog đi bộ phố.', '2 TikTok vlogs', 16000000.00::NUMERIC(15,2),
     12, 11, 4, 20, 'COMPLETED'),
    ('brand.highlands@seed.local',  'linh-baeli',           'Dash Seed D11 Highlands Glow Up',
     'Morning coffee routine + makeup nhẹ.', '2 TikTok videos', 15000000.00::NUMERIC(15,2),
     11, 7, 2, 16, 'COMPLETED'),
    ('brand.unilever@seed.local',   'ngo-thi-kim-yen',      'Dash Seed D11 Unilever Skin Care',
     'Review chuỗi skincare Unilever cho da nhạy cảm.', '3 TikTok review', 20000000.00::NUMERIC(15,2),
     11, 13, 1, 15, 'COMPLETED'),
    ('brand.vinamilk@seed.local',   'nhan-phuong-chi-xu',   'Dash Seed D10 Vinamilk Family Vlog',
     'Gia đình trẻ + sữa Vinamilk trong routine hàng ngày.', '2 TikTok vlogs', 17000000.00::NUMERIC(15,2),
     10, 6, 0, 14, 'COMPLETED'),
    ('brand.shopee@seed.local',     'quynh-tran-ne',        'Dash Seed D10 Shopee Beauty Haul',
     'Unbox beauty box Shopee Mall.', '2 TikTok unboxing', 13000000.00::NUMERIC(15,2),
     10, 14, -2, 12, 'COMPLETED'),
    ('brand.tch@seed.local',        'simple-man',           'Dash Seed D09 TCH Workday Chill',
     'Vlog cà phê giữa giờ làm — tone trẻ trung.', '2 TikTok clips', 11000000.00::NUMERIC(15,2),
     9, 8, -1, 13, 'COMPLETED'),
    ('brand.bitis@seed.local',      'm-thang3001',          'Dash Seed D09 Bitis Active Run',
     'Chạy bộ + test giày thể thao Biti''s.', '2 TikTok fitness', 19000000.00::NUMERIC(15,2),
     9, 12, 1, 15, 'COMPLETED'),
    ('brand.highlands@seed.local',  'duc-va-ly',            'Dash Seed D08 Highlands Couple Date',
     'Date coffee Highlands — couple content.', '2 TikTok vlogs', 12500000.00::NUMERIC(15,2),
     8, 10, 0, 14, 'COMPLETED'),
    ('brand.vinamilk@seed.local',   'di-di',                'Dash Seed D07 Vinamilk Kitchen',
     'Nấu ăn healthy + sữa Vinamilk.', '3 TikTok cooking', 21000000.00::NUMERIC(15,2),
     7, 9, -3, 11, 'COMPLETED'),
    ('brand.shopee@seed.local',     'hanyone',              'Dash Seed D06 Shopee Fashion Drop',
     'Lookbook thời trang từ Shopee Mall.', '2 TikTok lookbook', 22000000.00::NUMERIC(15,2),
     6, 11, 2, 16, 'COMPLETED'),
    ('brand.tch@seed.local',        'thau-di-dau',          'Dash Seed D05 TCH Travel Stop',
     'Travel vlog dừng chân TCH trên đường.', '1 TikTok vlog', 10000000.00::NUMERIC(15,2),
     5, 7, 4, 18, 'COMPLETED'),
    -- 4–2 ngày trước: mix active + completed
    ('brand.bitis@seed.local',      'nha-hieu-review',      'Dash Seed D04 Bitis Street Style',
     'Phối đồ streetwear với giày Biti''s.', '2 TikTok', 17500000.00::NUMERIC(15,2),
     4, 13, 3, 17, 'COMPLETED'),
    ('brand.highlands@seed.local',  'van-pham',             'Dash Seed D03 Highlands Night Study',
     'Cà phê đêm + review sách/study vlog.', '2 TikTok', 9000000.00::NUMERIC(15,2),
     3, 8, 5, 19, 'IN_PROGRESS'),
    ('brand.unilever@seed.local',   'chi-ho-vlog',          'Dash Seed D02 Unilever Hair Care',
     'Review dầu gội Unilever — hair routine.', '2 TikTok review', 14500000.00::NUMERIC(15,2),
     2, 10, 6, 20, 'IN_PROGRESS'),
    ('brand.vinamilk@seed.local',   'ha-cookie',            'Dash Seed D02 Vinamilk Sweet Treat',
     'Dessert + sữa — content Gen Z.', '2 TikTok', 15500000.00::NUMERIC(15,2),
     2, 15, 7, 21, 'ACCEPTED'),
    ('brand.shopee@seed.local',     'yul-daily',            'Dash Seed D01 Shopee Home Decor',
     'Setup góc học tập với đồ Shopee.', '3 TikTok setup', 13000000.00::NUMERIC(15,2),
     1, 6, 8, 22, 'DELIVERED'),
    -- Hôm nay: pipeline mới
    ('brand.tch@seed.local',        'le-kha-duy',           'Dash Seed D00 TCH Summer Launch',
     'Khai mùa hè TCH — teaser campaign.', '1 TikTok teaser', 8500000.00::NUMERIC(15,2),
     0, 9, 10, 24, 'PENDING'),
    ('brand.bitis@seed.local',      'duong-jin',            'Dash Seed D00 Bitis Campus Walk',
     'Back-to-school với giày Biti''s.', '2 TikTok', 16500000.00::NUMERIC(15,2),
     0, 14, 12, 26, 'ACCEPTED')
) AS m(
    brand_email, kol_slug, campaign_title, campaign_brief, deliverables, budget,
    days_ago, hour_offset, start_offset, end_offset, status
)
JOIN app_user brand_u ON brand_u.email = m.brand_email
JOIN brand_profile bp ON bp.user_id = brand_u.id
JOIN kol_profile   kp ON kp.slug = m.kol_slug AND kp.status = 'APPROVED'
WHERE NOT EXISTS (
    SELECT 1 FROM booking eb WHERE eb.campaign_title = m.campaign_title
);

-- FEE ledger cho platform wallet (user_id = 0) — doanh thu dashboard
INSERT INTO wallet_transaction (
    wallet_id, type, amount, balance_after, booking_id, external_ref, status, note, created_at
)
SELECT
    w.id,
    'FEE',
    b.platform_fee_amount,
    w.balance_available + b.platform_fee_amount,
    b.id,
    'FEE-DASH-' || b.id,
    'SUCCESS',
    'Phí nền tảng 10% — ' || b.campaign_title,
    b.created_at + INTERVAL '1 day'
FROM booking b
JOIN wallet w ON w.user_id = 0
WHERE b.campaign_title LIKE 'Dash Seed %'
  AND b.status = 'COMPLETED'
  AND b.platform_fee_amount IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM wallet_transaction tx
      WHERE tx.booking_id = b.id AND tx.type = 'FEE'
  );

-- Cập nhật balance platform wallet (best-effort snapshot)
UPDATE wallet w
SET balance_available = COALESCE(fee_total.total, 0),
    updated_at = NOW()
FROM (
    SELECT COALESCE(SUM(amount), 0) AS total
    FROM wallet_transaction
    WHERE wallet_id = (SELECT id FROM wallet WHERE user_id = 0 LIMIT 1)
      AND type = 'FEE'
      AND status = 'SUCCESS'
) fee_total
WHERE w.user_id = 0;
