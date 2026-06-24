-- =====================================================================
-- V48 — Seed demographics (gender / city / country / date_of_birth) cho
--        kol_profile và bổ sung review để có dữ liệu avg_rating /
--        review_count.
--
-- Mục tiêu:
--   1. Set country = 'VN' cho TẤT CẢ kol_profile (yêu cầu seed).
--   2. Điền gender / date_of_birth / city cho các profile đang NULL/empty.
--      Heuristic dựa trên bio và display_name (KOL V14 đa số có bio rất
--      chi tiết: nêu rõ "anh chàng"/"cô nàng", "sinh năm YYYY", quê quán)
--      thay vì gán random theo id — tránh case 1 KOL nữ bị set MALE.
--      Fallback deterministic theo id chỉ khi bio không có signal.
--      KHÔNG ghi đè giá trị seed manual (V32 m-thang3001 đã có gender='MALE',
--      city='Hà Nội' — preserve).
--   3. Với mỗi KOL chưa có review BRAND_TO_KOL nào, tạo 1 booking
--      COMPLETED synthetic round-robin qua các brand seed sẵn có.
--   4. Insert review BRAND_TO_KOL cho mọi booking COMPLETED còn thiếu
--      review (bao gồm cả booking V48 vừa tạo).
--   5. Recompute kol_profile.avg_rating / review_count từ toàn bộ
--      review BRAND_TO_KOL (khớp logic ReviewService#recomputeKolRating).
--
-- Phụ thuộc:
--   - V3  (kol_profile)
--   - V5  (booking)
--   - V7  (review)
--   - V14 (KOL imported từ kols-koc.com)
--   - V15 (brand seed: Vinamilk, Shopee Vietnam, The Coffee House, Biti's)
--   - V20 (brand seed: Highlands Coffee, Unilever Vietnam)
--   - V47 (city widened to VARCHAR(150))
--
-- Idempotency: tất cả UPDATE đều có guard NULL/empty, INSERT đều có
-- NOT EXISTS. Safe khi chạy lại trên DB đã có dữ liệu V48.
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1) country = 'VN' cho toàn bộ kol_profile.
-- ---------------------------------------------------------------------
UPDATE kol_profile
SET country    = 'VN',
    updated_at = NOW()
WHERE country IS DISTINCT FROM 'VN';

-- ---------------------------------------------------------------------
-- 2) gender — đoán từ bio + display_name thay vì random.
--    Chỉ fill khi đang NULL/empty (giữ giá trị seed thủ công như V32).
--
--    Heuristic priority (CASE WHEN — first match wins):
--      OTHER  ← bio nhắc "cặp đôi tiktoker"/"vợ chồng" (cặp KOL chung profile)
--      MALE   ← bio chứa "anh chàng" / "chàng trai" / "nam tiktoker"…
--      FEMALE ← bio chứa "cô nàng" / "cô gái" / "nữ tiktoker"…
--      FEMALE ← display_name bắt đầu "Mẹ "/"Chị "/"Nàng "/"Cô "
--      FEMALE ← display_name có tên đệm "Thị"
--      MALE   ← display_name bắt đầu "Anh "/"Bố "/"Chú "
--      FEMALE ← default (ngành review/beauty/family skew nữ rõ rệt)
-- ---------------------------------------------------------------------
UPDATE kol_profile
SET gender = CASE
        WHEN bio ~* '(cặp đôi tiktoker|cặp đôi nổi tiếng|cặp đôi sáng tạo|cặp đôi trẻ|cặp đôi tài năng|đôi tình nhân|hai vợ chồng|vợ chồng son)'
            THEN 'OTHER'
        WHEN bio ~* '(anh chàng|chàng trai|một chàng|chàng tiktoker|nam tiktoker|nam youtuber|nam blogger|nam beauty blogger|nam kol|nam genz|hot boy|anh tên thật|anh chính là|anh sinh năm|anh hiện đang sống|anh hiện đang sinh sống|anh hiện đang cư trú|anh là cử nhân|anh là một|anh là chàng|anh là người|anh là sinh viên)'
            THEN 'MALE'
        WHEN bio ~* '(cô nàng|cô gái|cô bé|nữ tiktoker|nữ youtuber|nữ blogger|nữ beauty blogger|nữ kol|nữ genz|hot girl|cô tên thật|cô chính là|cô sinh năm|cô hiện đang sống|cô hiện đang sinh sống|cô hiện đang cư trú|chị tên thật|chị chính là|chị sinh năm|cô là một|cô là người|cô là sinh viên|chị là chủ|chị là mẹ|chị là người|sinh ra là cô gái)'
            THEN 'FEMALE'
        WHEN display_name ~* '^(mẹ |chị |nàng |cô )'
            THEN 'FEMALE'
        WHEN display_name ~* '\mthị\M'
            THEN 'FEMALE'
        WHEN display_name ~* '^(anh |bố |chú )'
            THEN 'MALE'
        ELSE 'FEMALE'
    END,
    updated_at = NOW()
WHERE gender IS NULL OR gender = '';

-- ---------------------------------------------------------------------
-- 3) date_of_birth — ưu tiên parse từ bio:
--      "sinh ngày DD/MM/YYYY"  → đúng ngày
--      "sinh năm YYYY"         → 01/01/YYYY
--    Nếu bio không có, fallback deterministic theo id (1990-01-01 +
--    ((id*73) % 5840) ngày → khoảng 1990-2005, độ tuổi 20-36 vào 2026).
-- ---------------------------------------------------------------------
UPDATE kol_profile
SET date_of_birth = CASE
        WHEN bio ~* 'sinh ngày \d{1,2}/\d{1,2}/\d{4}'
            THEN TO_DATE(substring(bio FROM '(?i)sinh ngày (\d{1,2}/\d{1,2}/\d{4})'), 'DD/MM/YYYY')
        WHEN bio ~* 'sinh năm \d{4}'
            THEN TO_DATE(substring(bio FROM '(?i)sinh năm (\d{4})'), 'YYYY')
        ELSE DATE '1990-01-01' + ((id * 73) % 5840)::INTEGER
    END,
    updated_at = NOW()
WHERE date_of_birth IS NULL;

-- ---------------------------------------------------------------------
-- 4) city — ưu tiên detect city từ bio (rất nhiều KOL nêu rõ nơi sống/
--    quê quán). Nếu bio không nhắc city nào → fallback round-robin pool
--    13 thành phố lớn theo id % 13.
-- ---------------------------------------------------------------------
UPDATE kol_profile
SET city = COALESCE(
        CASE
            WHEN bio ~* '(thành phố hồ chí minh|tp\.?\s*hồ chí minh|tp\.?\s*hcm|tphcm|hồ chí minh|sài gòn)' THEN 'Hồ Chí Minh'
            WHEN bio ~* '(thành phố hà nội|tp\.?\s*hà nội|\mhà nội\M)'                                     THEN 'Hà Nội'
            WHEN bio ~* 'đà nẵng'                                                                          THEN 'Đà Nẵng'
            WHEN bio ~* 'hải phòng'                                                                        THEN 'Hải Phòng'
            WHEN bio ~* 'cần thơ'                                                                          THEN 'Cần Thơ'
            WHEN bio ~* 'nha trang'                                                                        THEN 'Nha Trang'
            WHEN bio ~* '(đà lạt|lâm đồng)'                                                                THEN 'Đà Lạt'
            WHEN bio ~* 'vũng tàu'                                                                         THEN 'Vũng Tàu'
            WHEN bio ~* '(biên hòa|biên hoà)'                                                              THEN 'Biên Hòa'
            WHEN bio ~* 'quy nhơn'                                                                         THEN 'Quy Nhơn'
            WHEN bio ~* 'bắc ninh'                                                                         THEN 'Bắc Ninh'
            WHEN bio ~* '(buôn ma thuột|đắk lắk|dak lak)'                                                  THEN 'Buôn Ma Thuột'
            WHEN bio ~* '(gia lai|pleiku)'                                                                 THEN 'Pleiku'
            WHEN bio ~* 'bắc giang'                                                                        THEN 'Bắc Giang'
            WHEN bio ~* 'thái bình'                                                                        THEN 'Thái Bình'
            WHEN bio ~* 'lai châu'                                                                         THEN 'Lai Châu'
            WHEN bio ~* 'phú yên'                                                                          THEN 'Phú Yên'
            WHEN bio ~* 'an giang'                                                                         THEN 'An Giang'
            WHEN bio ~* 'huế'                                                                              THEN 'Huế'
            ELSE NULL
        END,
        (ARRAY[
            'Hồ Chí Minh','Hà Nội','Đà Nẵng','Hải Phòng','Cần Thơ',
            'Huế','Nha Trang','Đà Lạt','Vũng Tàu','Biên Hòa',
            'Quy Nhơn','Bắc Ninh','Buôn Ma Thuột'
        ])[(id % 13) + 1]
    ),
    updated_at = NOW()
WHERE city IS NULL OR city = '';

-- ---------------------------------------------------------------------
-- 5) Booking COMPLETED synthetic cho KOL chưa có review BRAND_TO_KOL nào.
--    Round-robin qua 6 brand seed (V15 + V20).
-- ---------------------------------------------------------------------
WITH brand_pool AS (
    SELECT bp.id                                AS brand_profile_id,
           ROW_NUMBER() OVER (ORDER BY bp.id)   AS rn,
           COUNT(*)     OVER ()                 AS total
    FROM brand_profile bp
    WHERE bp.company_name IN (
        'Vinamilk', 'Shopee Vietnam', 'The Coffee House', 'Biti''s',
        'Highlands Coffee', 'Unilever Vietnam'
    )
),
kols_to_seed AS (
    SELECT kp.id                              AS kol_profile_id,
           kp.slug,
           kp.display_name,
           ROW_NUMBER() OVER (ORDER BY kp.id) AS rn
    FROM kol_profile kp
    WHERE NOT EXISTS (
        SELECT 1
        FROM booking b
        JOIN review  r ON r.booking_id = b.id AND r.direction = 'BRAND_TO_KOL'
        WHERE b.kol_profile_id = kp.id
    )
)
INSERT INTO booking (
    brand_profile_id, kol_profile_id, campaign_title, campaign_brief,
    deliverables, budget, start_date, end_date, status
)
SELECT
    bp.brand_profile_id,
    k.kol_profile_id,
    'V48 Seed Campaign - ' || k.slug,
    'Campaign auto-seed (V48) nhằm bổ sung dữ liệu review/rating cho KOL ' || k.display_name || '.',
    '1 TikTok video + 1 IG post',
    (10000000 + (k.rn % 5) * 2000000)::NUMERIC(15,2),
    DATE '2025-09-01' + (k.rn % 60)::INTEGER,
    DATE '2025-09-20' + (k.rn % 60)::INTEGER,
    'COMPLETED'
FROM kols_to_seed k
JOIN brand_pool bp ON bp.rn = ((k.rn - 1) % bp.total) + 1;

-- ---------------------------------------------------------------------
-- 6) Review BRAND_TO_KOL cho mọi booking COMPLETED còn thiếu review.
--    Rating deterministic theo booking.id (skew 4-5, lác đác 3).
--    Comment chọn từ pool 8 mẫu theo booking.id % 8.
-- ---------------------------------------------------------------------
INSERT INTO review (booking_id, author_id, target_id, direction, rating, comment)
SELECT
    b.id,
    bp.user_id,
    kp.user_id,
    'BRAND_TO_KOL',
    CASE b.id % 10
        WHEN 0 THEN 5::SMALLINT
        WHEN 1 THEN 5::SMALLINT
        WHEN 2 THEN 5::SMALLINT
        WHEN 3 THEN 4::SMALLINT
        WHEN 4 THEN 4::SMALLINT
        WHEN 5 THEN 5::SMALLINT
        WHEN 6 THEN 4::SMALLINT
        WHEN 7 THEN 5::SMALLINT
        WHEN 8 THEN 3::SMALLINT
        ELSE        4::SMALLINT
    END,
    CASE b.id % 8
        WHEN 0 THEN 'KOL chuyên nghiệp, deadline đúng cam kết, chất lượng content vượt mong đợi.'
        WHEN 1 THEN 'Brand voice được lồng ghép tự nhiên, engagement vượt KPI cam kết.'
        WHEN 2 THEN 'Hợp tác trơn tru, KOL chủ động trong các vòng feedback, hài lòng tổng thể.'
        WHEN 3 THEN 'Visual chỉn chu, brief được thực thi đúng tinh thần, sẵn sàng tái booking.'
        WHEN 4 THEN 'KOL phối hợp tốt, reach và view ổn định, có thể tối ưu thêm caption ở campaign sau.'
        WHEN 5 THEN 'Nội dung sáng tạo, format phù hợp Gen Z, conversion ổn.'
        WHEN 6 THEN 'Lịch trình giao bài hợp lý, KOL trao đổi rõ ràng, sản phẩm cuối chỉn chu.'
        ELSE        'Trải nghiệm hợp tác tích cực, sẽ cân nhắc tiếp tục booking trong các campaign sắp tới.'
    END
FROM booking b
JOIN brand_profile bp ON bp.id = b.brand_profile_id
JOIN kol_profile   kp ON kp.id = b.kol_profile_id
WHERE b.status = 'COMPLETED'
  AND NOT EXISTS (
      SELECT 1 FROM review r
      WHERE r.booking_id = b.id AND r.direction = 'BRAND_TO_KOL'
  );

-- ---------------------------------------------------------------------
-- 7) Recompute kol_profile.avg_rating / review_count.
--    Khớp ReviewService#recomputeKolRating: AVG(rating) HALF_UP scale 2,
--    group by target_id = kol user_id.
-- ---------------------------------------------------------------------
UPDATE kol_profile k SET
    avg_rating   = stats.avg_rating,
    review_count = stats.review_count,
    updated_at   = NOW()
FROM (
    SELECT
        r.target_id                              AS kol_user_id,
        ROUND(AVG(r.rating)::NUMERIC, 2)         AS avg_rating,
        COUNT(*)::INTEGER                        AS review_count
    FROM review r
    WHERE r.direction = 'BRAND_TO_KOL'
    GROUP BY r.target_id
) stats
WHERE k.user_id = stats.kol_user_id;
