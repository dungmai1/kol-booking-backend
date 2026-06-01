-- =====================================================================
-- V22 — Bulk seed thêm reviews cho tất cả 20 KOL (V8 + V14).
--
-- Mục tiêu:
--   Bổ sung 2-3 BRAND_TO_KOL review cho mỗi KOL để:
--   - Nâng tổng reviews mỗi KOL lên 3-5 reviews
--   - Tạo avg_rating đa dạng, thực tế hơn (3.50 – 4.80)
--   - Tăng độ tin cậy hiển thị trên trang KOL public
--
-- Phụ thuộc:
--   - V15 (brand profiles: Vinamilk, Shopee Vietnam, The Coffee House, Biti's)
--   - V20 (brand profiles: Highlands Coffee, Unilever Vietnam)
--   - V8, V14 (KOL slugs)
--
-- KHÔNG tạo brand user/profile mới — reuse cả 6 brands hiện có.
-- Campaign titles hoàn toàn mới, không trùng V15/V20/V21.
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1) Bookings COMPLETED — 59 bookings mới cho 20 KOL
-- ---------------------------------------------------------------------
INSERT INTO booking (
    brand_profile_id, kol_profile_id, campaign_title, campaign_brief,
    deliverables, budget, start_date, end_date, status
)
SELECT
    bp.id, kp.id, m.campaign_title, m.campaign_brief,
    m.deliverables, m.budget, m.start_date, m.end_date, 'COMPLETED'
FROM (VALUES
    -- ── V8 KOLs ──────────────────────────────────────────────────────
    -- nguyen-thi-thanh-nha (+3)
    ('Shopee Vietnam',   'nguyen-thi-thanh-nha', 'Shopee Athlete Collection',
     'Campaign thu hút cộng đồng thể thao với sản phẩm sport từ Shopee Mall.',
     '2 TikTok sport shorts + 1 IG carousel', 16000000.00::NUMERIC(15,2),
     DATE '2026-04-01', DATE '2026-04-20'),
    ('The Coffee House', 'nguyen-thi-thanh-nha', 'TCH Morning Training',
     'Khởi đầu buổi sáng tập luyện cùng cà phê TCH take-away.',
     '2 TikTok lifestyle clips', 14000000.00::NUMERIC(15,2),
     DATE '2026-05-01', DATE '2026-05-15'),
    ('Highlands Coffee', 'nguyen-thi-thanh-nha', 'Highlands Sporty Start',
     'Tinh thần thể thao và cà phê Highlands — năng lượng cho ngày mới.',
     '1 TikTok vlog + 1 IG reel', 13000000.00::NUMERIC(15,2),
     DATE '2026-05-10', DATE '2026-05-25'),

    -- lam-bao-ngoc (+3)
    ('Biti''s',          'lam-bao-ngoc', 'Biti''s Melody Walk',
     'Kết hợp lookbook âm nhạc với giày Biti''s theo phong cách năng động.',
     '2 TikTok lookbook videos', 22000000.00::NUMERIC(15,2),
     DATE '2026-04-05', DATE '2026-04-22'),
    ('The Coffee House', 'lam-bao-ngoc', 'TCH Music & Coffee',
     'Acoustic session tại không gian TCH — âm nhạc và cà phê hoà quyện.',
     '2 TikTok acoustic sessions', 18000000.00::NUMERIC(15,2),
     DATE '2026-04-15', DATE '2026-04-30'),
    ('Unilever Vietnam', 'lam-bao-ngoc', 'Sunsilk Lam Bao Ngoc',
     'Collab chăm sóc tóc đẹp mỗi ngày với Sunsilk — giọng hát và mái tóc.',
     '1 TikTok music collab + 2 IG posts', 26000000.00::NUMERIC(15,2),
     DATE '2026-05-05', DATE '2026-05-20'),

    -- phuong-di-dau (+3)
    ('Shopee Vietnam',   'phuong-di-dau', 'Shopee Explore Vietnam',
     'Khám phá Việt Nam với đặc sản từ Shopee Mall trên từng hành trình.',
     '3 TikTok travel videos', 20000000.00::NUMERIC(15,2),
     DATE '2026-03-20', DATE '2026-04-10'),
    ('Highlands Coffee', 'phuong-di-dau', 'Highlands Northern Trails',
     'Hành trình miền Bắc dừng chân tại Highlands Coffee — cà phê và cung đường đẹp.',
     '2 TikTok travel vlogs + 1 IG story', 19000000.00::NUMERIC(15,2),
     DATE '2026-04-10', DATE '2026-04-28'),
    ('Biti''s',          'phuong-di-dau', 'Biti''s Travel Companion',
     'Test độ bền giày Biti''s trên những cung đường phượt dài ngày.',
     '3 TikTok travel shorts', 21000000.00::NUMERIC(15,2),
     DATE '2026-05-01', DATE '2026-05-18'),

    -- tien-tien (+3)
    ('Biti''s',          'tien-tien', 'Biti''s Rhythm Moves',
     'Vũ đạo năng động kết hợp với BST giày Biti''s mới nhất.',
     '3 TikTok dance videos', 24000000.00::NUMERIC(15,2),
     DATE '2026-04-01', DATE '2026-04-18'),
    ('Highlands Coffee', 'tien-tien', 'Highlands Music Morning',
     'Acoustic buổi sáng tại Highlands — truyền cảm hứng qua âm nhạc.',
     '2 TikTok acoustic clips', 17000000.00::NUMERIC(15,2),
     DATE '2026-04-20', DATE '2026-05-05'),
    ('Unilever Vietnam', 'tien-tien', 'Dove x Tien Tien Collab',
     'Chăm sóc bản thân mỗi ngày với Dove — thông điệp yêu thương bản thân qua âm nhạc.',
     '2 TikTok + 1 IG reel', 30000000.00::NUMERIC(15,2),
     DATE '2026-05-01', DATE '2026-05-20'),

    -- hoang-duyen (+3)
    ('The Coffee House', 'hoang-duyen', 'TCH Beauty & Ritual',
     'Beauty ritual buổi sáng tại TCH — không gian chill và menu mới.',
     '2 TikTok lifestyle clips', 20000000.00::NUMERIC(15,2),
     DATE '2026-04-05', DATE '2026-04-20'),
    ('Highlands Coffee', 'hoang-duyen', 'Highlands Morning Glow',
     'Bắt đầu ngày mới tươi sáng cùng Highlands Coffee — beauty meets lifestyle.',
     '2 TikTok + 1 IG carousel', 18000000.00::NUMERIC(15,2),
     DATE '2026-04-22', DATE '2026-05-08'),
    ('Vinamilk',         'hoang-duyen', 'Vinamilk Beauty Inside Out',
     'Sắc đẹp từ bên trong với Vinamilk — healthy và radiant mỗi ngày.',
     '2 TikTok + 1 IG reel', 22000000.00::NUMERIC(15,2),
     DATE '2026-05-05', DATE '2026-05-22'),

    -- hoang-soi (+2)
    ('Vinamilk',         'hoang-soi', 'Vinamilk Strong & Fit',
     'Nội dung thể lực kết hợp dinh dưỡng từ sữa Vinamilk — strong body, strong mind.',
     '3 TikTok fitness shorts', 16000000.00::NUMERIC(15,2),
     DATE '2026-04-01', DATE '2026-04-18'),
    ('Unilever Vietnam', 'hoang-soi', 'Clear Fresh Day',
     'Review dầu gội Clear trong sinh hoạt thể thao hàng ngày.',
     '2 TikTok review clips', 14000000.00::NUMERIC(15,2),
     DATE '2026-04-20', DATE '2026-05-05'),

    -- tebefood (+3)
    ('Vinamilk',         'tebefood', 'Vinamilk Chef Secrets',
     'Bí quyết bếp núc kết hợp nguyên liệu từ sản phẩm Vinamilk tươi ngon.',
     '3 TikTok cooking videos', 20000000.00::NUMERIC(15,2),
     DATE '2026-04-08', DATE '2026-04-25'),
    ('Highlands Coffee', 'tebefood', 'Highlands Kitchen Stories',
     'Câu chuyện bếp núc buổi sáng bắt đầu bằng cà phê Highlands.',
     '2 TikTok lifestyle clips', 15000000.00::NUMERIC(15,2),
     DATE '2026-04-28', DATE '2026-05-12'),
    ('Unilever Vietnam', 'tebefood', 'Knorr Cooking Class',
     'Lớp học nấu ăn online với bí quyết gia vị Knorr từ bếp chuyên nghiệp.',
     '3 TikTok recipe videos', 18000000.00::NUMERIC(15,2),
     DATE '2026-05-01', DATE '2026-05-18'),

    -- tuan-di-dau (+3)
    ('Vinamilk',         'tuan-di-dau', 'Vinamilk Da Nang Flavor',
     'Khám phá ẩm thực Đà Nẵng kết hợp sữa Vinamilk tươi nguyên chất.',
     '2 TikTok food travel vlogs', 14000000.00::NUMERIC(15,2),
     DATE '2026-04-05', DATE '2026-04-20'),
    ('Shopee Vietnam',   'tuan-di-dau', 'Shopee Street Food Trail',
     'Hành trình ẩm thực đường phố Đà Nẵng với đặc sản từ Shopee Mall.',
     '3 TikTok food shorts', 17000000.00::NUMERIC(15,2),
     DATE '2026-04-22', DATE '2026-05-08'),
    ('Highlands Coffee', 'tuan-di-dau', 'Highlands Coast Route',
     'Cung đường ven biển miền Trung — dừng chân tại Highlands trên mỗi chặng.',
     '2 TikTok travel videos', 13000000.00::NUMERIC(15,2),
     DATE '2026-05-01', DATE '2026-05-16'),

    -- ── V14 KOLs ──────────────────────────────────────────────────────
    -- nhan-phuong-chi-xu (+3)
    ('Biti''s',          'nhan-phuong-chi-xu', 'Biti''s Family Journey',
     'Hành trình gia đình ấm áp với giày Biti''s — mỗi bước đều là kỷ niệm.',
     '2 TikTok family vlogs + 1 IG carousel', 20000000.00::NUMERIC(15,2),
     DATE '2026-04-01', DATE '2026-04-18'),
    ('Shopee Vietnam',   'nhan-phuong-chi-xu', 'Shopee Mom & Baby Box',
     'Unbox sản phẩm mẹ và bé từ Shopee Mall — chọn lựa thông minh cho gia đình.',
     '1 Shopee Live + 1 TikTok recap', 18000000.00::NUMERIC(15,2),
     DATE '2026-04-20', DATE '2026-05-05'),
    ('Vinamilk',         'nhan-phuong-chi-xu', 'Vinamilk Growing Kids',
     'Dinh dưỡng đúng cho con lớn khoẻ — Vinamilk đồng hành cùng mẹ.',
     '2 TikTok + 1 IG reel', 24000000.00::NUMERIC(15,2),
     DATE '2026-05-05', DATE '2026-05-20'),

    -- duc-va-ly (+3)
    ('Vinamilk',         'duc-va-ly', 'Vinamilk Couple Goals',
     'Couple lifestyle với Vinamilk — cùng nhau healthy mỗi ngày.',
     '2 TikTok couple videos', 20000000.00::NUMERIC(15,2),
     DATE '2026-04-05', DATE '2026-04-20'),
    ('The Coffee House', 'duc-va-ly', 'TCH Date Spot',
     'Địa điểm hẹn hò lý tưởng tại TCH — romantic moments cùng cà phê.',
     '2 TikTok couple vlogs', 16000000.00::NUMERIC(15,2),
     DATE '2026-04-22', DATE '2026-05-06'),
    ('Biti''s',          'duc-va-ly', 'Biti''s His & Hers',
     'Mix and match giày đôi Biti''s cho couple — phong cách hoà hợp.',
     '2 TikTok couple looks', 22000000.00::NUMERIC(15,2),
     DATE '2026-05-08', DATE '2026-05-22'),

    -- dumi (+3)
    ('Shopee Vietnam',   'dumi', 'Shopee Creator Finds',
     'Review thiết bị sáng tạo nội dung từ Shopee Mall — gear cho creator Gen Z.',
     '2 TikTok review shorts', 14000000.00::NUMERIC(15,2),
     DATE '2026-04-01', DATE '2026-04-16'),
    ('Biti''s',          'dumi', 'Biti''s Gen Z Street',
     'Streetwear Gen Z kết hợp giày Biti''s — phong cách đường phố bùng nổ.',
     '3 TikTok streetwear clips', 16000000.00::NUMERIC(15,2),
     DATE '2026-04-18', DATE '2026-05-02'),
    ('Vinamilk',         'dumi', 'Vinamilk Youth Boost',
     'Năng lượng tuổi trẻ với Vinamilk — sáng tạo không ngừng nghỉ.',
     '2 TikTok lifestyle videos', 18000000.00::NUMERIC(15,2),
     DATE '2026-05-05', DATE '2026-05-20'),

    -- nha-hieu-review (+3)
    ('Biti''s',          'nha-hieu-review', 'Biti''s Honest Walk Test',
     'Test thực chiến giày Biti''s trên nhiều địa hình — honest review đáng tin.',
     '2 TikTok test videos + 1 IG review', 16000000.00::NUMERIC(15,2),
     DATE '2026-04-05', DATE '2026-04-20'),
    ('Unilever Vietnam', 'nha-hieu-review', 'Comfort Fabric Review',
     'Review nước xả vải Comfort trong cuộc sống hàng ngày — cảm nhận thật.',
     '1 TikTok review + 1 IG carousel', 14000000.00::NUMERIC(15,2),
     DATE '2026-04-22', DATE '2026-05-06'),
    ('The Coffee House', 'nha-hieu-review', 'TCH Honest Taste Test',
     'Blind taste test menu mới TCH — đánh giá hoàn toàn trung thực.',
     '1 TikTok blind test video', 15000000.00::NUMERIC(15,2),
     DATE '2026-05-05', DATE '2026-05-20'),

    -- linh-baeli (+3)
    ('The Coffee House', 'linh-baeli', 'TCH Skincare & Chill',
     'Skincare routine sáng tạo trong không gian chill của TCH.',
     '2 TikTok beauty clips', 20000000.00::NUMERIC(15,2),
     DATE '2026-04-01', DATE '2026-04-18'),
    ('Shopee Vietnam',   'linh-baeli', 'Shopee Beauty Haul',
     'Haul beauty tháng mới từ Shopee Mall — review chi tiết từng sản phẩm.',
     '2 TikTok haul videos', 18000000.00::NUMERIC(15,2),
     DATE '2026-04-20', DATE '2026-05-05'),
    ('Biti''s',          'linh-baeli', 'Biti''s Street Glam',
     'Street style kết hợp làm đẹp trendy với giày Biti''s — Gen Z nữ phong cách.',
     '2 TikTok + 1 IG reel', 22000000.00::NUMERIC(15,2),
     DATE '2026-05-05', DATE '2026-05-20'),

    -- ngo-thi-kim-yen (+3)
    ('The Coffee House', 'ngo-thi-kim-yen', 'TCH Hair & Latte',
     'Hair care routine buổi sáng kết hợp latte tại TCH — vẻ đẹp tự nhiên.',
     '2 TikTok beauty vlogs', 16000000.00::NUMERIC(15,2),
     DATE '2026-04-05', DATE '2026-04-20'),
    ('Shopee Vietnam',   'ngo-thi-kim-yen', 'Shopee Beauty Week',
     'Series 7 ngày review sản phẩm làm đẹp từ Shopee Mall — daily beauty picks.',
     '7 daily TikTok clips', 18000000.00::NUMERIC(15,2),
     DATE '2026-04-22', DATE '2026-05-06'),
    ('Biti''s',          'ngo-thi-kim-yen', 'Biti''s Femme Run',
     'Chạy bộ năng động cùng BST giày nữ Biti''s — active lifestyle cho phụ nữ hiện đại.',
     '2 TikTok + 1 IG reel', 20000000.00::NUMERIC(15,2),
     DATE '2026-05-05', DATE '2026-05-20'),

    -- hong-ngoc-van (+3)
    ('Shopee Vietnam',   'hong-ngoc-van', 'Shopee Flash Picks',
     'Series daily flash deal picks — săn deal Shopee cùng KOL mỗi ngày.',
     '3 TikTok daily deal shorts', 14000000.00::NUMERIC(15,2),
     DATE '2026-04-01', DATE '2026-04-16'),
    ('Vinamilk',         'hong-ngoc-van', 'Vinamilk Glow Inside',
     'Sắc đẹp từ bên trong với Vinamilk — chăm sóc từ sâu bên trong.',
     '2 TikTok + 1 IG carousel', 16000000.00::NUMERIC(15,2),
     DATE '2026-04-18', DATE '2026-05-02'),
    ('The Coffee House', 'hong-ngoc-van', 'TCH Refresh & Renew',
     'Làm mới bản thân với không gian TCH — cà phê và cảm hứng mới mỗi ngày.',
     '2 TikTok lifestyle clips', 14000000.00::NUMERIC(15,2),
     DATE '2026-05-05', DATE '2026-05-20'),

    -- bu-ne (+3)
    ('Shopee Vietnam',   'bu-ne', 'Shopee Street Eats Live',
     'Livestream khám phá ẩm thực đường phố với nguyên liệu từ Shopee Fresh.',
     '1 Shopee Live + 1 TikTok recap', 16000000.00::NUMERIC(15,2),
     DATE '2026-04-05', DATE '2026-04-20'),
    ('The Coffee House', 'bu-ne', 'TCH Food Crawl',
     'Hành trình foodie quận 1 kết hợp dừng chân tại TCH — cà phê và ẩm thực.',
     '3 TikTok food vlog episodes', 14000000.00::NUMERIC(15,2),
     DATE '2026-04-22', DATE '2026-05-06'),
    ('Unilever Vietnam', 'bu-ne', 'Knorr Vỉa Hè Ký Sự',
     'Ký sự ẩm thực vỉa hè Sài Gòn kết hợp gia vị Knorr — đậm đà từng góc phố.',
     '2 TikTok food street episodes', 15000000.00::NUMERIC(15,2),
     DATE '2026-05-05', DATE '2026-05-20'),

    -- hat-tieu-foodie (+3)
    ('Biti''s',          'hat-tieu-foodie', 'Biti''s Food Wanderer',
     'Hành trình foodie cùng giày Biti''s — khám phá ẩm thực và bước đi thoải mái.',
     '3 TikTok travel food clips', 18000000.00::NUMERIC(15,2),
     DATE '2026-04-05', DATE '2026-04-20'),
    ('Shopee Vietnam',   'hat-tieu-foodie', 'Shopee Foodie Finds 2026',
     'Khám phá đặc sản và nguyên liệu ngon từ Shopee Mall — foodie paradise.',
     '3 TikTok food discovery episodes', 20000000.00::NUMERIC(15,2),
     DATE '2026-04-22', DATE '2026-05-06'),
    ('Vinamilk',         'hat-tieu-foodie', 'Vinamilk 3 Miền Ngon',
     'Ẩm thực 3 miền kết hợp sữa Vinamilk tươi nguyên chất — hành trình vị ngon.',
     '3 TikTok food travel videos', 22000000.00::NUMERIC(15,2),
     DATE '2026-05-05', DATE '2026-05-20'),

    -- quynh-tran-ne (+3)
    ('The Coffee House', 'quynh-tran-ne', 'TCH Cooking Marathon',
     'Marathon nấu ăn livestream dài nhiều tiếng cùng cà phê TCH đồng hành.',
     '1 TCH Live marathon + 2 TikTok recaps', 18000000.00::NUMERIC(15,2),
     DATE '2026-04-01', DATE '2026-04-18'),
    ('Unilever Vietnam', 'quynh-tran-ne', 'Knorr Healthy Feast',
     'Bữa ăn lành mạnh và ngon miệng với gia vị Knorr — healthy cooking series.',
     '3 TikTok healthy recipe videos', 20000000.00::NUMERIC(15,2),
     DATE '2026-04-20', DATE '2026-05-05'),
    ('Biti''s',          'quynh-tran-ne', 'Biti''s Home Chef Style',
     'Phong cách bếp nhà kết hợp giày Biti''s thoải mái — thời trang ngay tại bếp.',
     '2 TikTok lifestyle clips', 16000000.00::NUMERIC(15,2),
     DATE '2026-05-05', DATE '2026-05-20'),

    -- eatwhning (+3)
    ('The Coffee House', 'eatwhning', 'TCH Regional Feast',
     'Bữa tiệc ẩm thực vùng miền trong không gian TCH — storytelling đẳng cấp.',
     '3 TikTok regional food episodes', 22000000.00::NUMERIC(15,2),
     DATE '2026-04-05', DATE '2026-04-20'),
    ('Vinamilk',         'eatwhning', 'Vinamilk Vietnam Travels',
     'Hành trình ẩm thực khắp Việt Nam kết hợp sữa Vinamilk — vị ngon muôn nơi.',
     '3 TikTok food travel vlogs', 20000000.00::NUMERIC(15,2),
     DATE '2026-04-22', DATE '2026-05-06'),
    ('Unilever Vietnam', 'eatwhning', 'Knorr Food Explorer',
     'Khám phá nguyên liệu địa phương và gia vị Knorr — hành trình ẩm thực học thuật.',
     '3 TikTok ingredient discovery videos', 18000000.00::NUMERIC(15,2),
     DATE '2026-05-05', DATE '2026-05-20'),

    -- simple-man (+3)
    ('Vinamilk',         'simple-man', 'Vinamilk Minimalist Life',
     'Phong cách sống tối giản kết hợp Vinamilk — đơn giản mà khoẻ mạnh mỗi ngày.',
     '2 TikTok minimalist lifestyle', 16000000.00::NUMERIC(15,2),
     DATE '2026-04-01', DATE '2026-04-16'),
    ('The Coffee House', 'simple-man', 'TCH Work Anywhere',
     'Làm việc hiệu quả tại bất kỳ đâu với cà phê TCH đồng hành.',
     '2 TikTok work + coffee shorts', 14000000.00::NUMERIC(15,2),
     DATE '2026-04-18', DATE '2026-05-02'),
    ('Unilever Vietnam', 'simple-man', 'Clear Daily Routine',
     'Routine chăm sóc tóc hàng ngày với Clear theo phong cách tối giản.',
     '2 TikTok grooming routine clips', 15000000.00::NUMERIC(15,2),
     DATE '2026-05-05', DATE '2026-05-20')

) AS m(brand_name, kol_slug, campaign_title, campaign_brief, deliverables, budget, start_date, end_date)
JOIN brand_profile bp ON bp.company_name = m.brand_name
JOIN kol_profile   kp ON kp.slug         = m.kol_slug;

-- ---------------------------------------------------------------------
-- 2) Reviews — BRAND_TO_KOL (59 reviews, 1 per booking)
--    Kết quả avg_rating sau V22:
--      nguyen-thi-thanh-nha  4.75  lam-bao-ngoc   4.75  phuong-di-dau  4.40
--      tien-tien             4.75  hoang-duyen    4.60  hoang-soi      3.50
--      tebefood              4.80  tuan-di-dau    4.25  nhan-p-c-x     4.75
--      duc-va-ly             4.75  dumi           4.00  nha-hieu       4.75
--      linh-baeli            4.75  ngo-thi-kim    4.25  hong-ngoc-van  3.75
--      bu-ne                 4.50  hat-tieu       4.75  quynh-tran-ne  4.00
--      eatwhning             4.75  simple-man     3.75
-- ---------------------------------------------------------------------
INSERT INTO review (booking_id, author_id, target_id, direction, rating, comment)
SELECT b.id, brand_u.id, kol_u.id, 'BRAND_TO_KOL', r.rating, r.comment
FROM (VALUES
    -- nguyen-thi-thanh-nha
    ('Shopee Athlete Collection',   4::SMALLINT, 'Hình ảnh năng động, phù hợp concept sport. Video cần đa dạng format hơn để giữ attention dài hơn.'),
    ('TCH Morning Training',        5::SMALLINT, 'Content buổi sáng tươi tắn, mạch video được giữ rất tốt. Brand mention tự nhiên, không gượng ép.'),
    ('Highlands Sporty Start',      5::SMALLINT, 'Tinh thần thể thao kết hợp cà phê khởi đầu ngày mới rất cuốn hút. KOL chuyên nghiệp, đúng deadline.'),
    -- lam-bao-ngoc
    ('Biti''s Melody Walk',         4::SMALLINT, 'Lookbook âm nhạc đẹp, phối đồ ấn tượng. Nên thêm 1-2 shot close-up giày để showcase sản phẩm tốt hơn.'),
    ('TCH Music & Coffee',          5::SMALLINT, 'Kết hợp âm nhạc và cà phê cực kỳ tự nhiên, đúng lifestyle mà TCH muốn truyền tải. Rất ấn tượng.'),
    ('Sunsilk Lam Bao Ngoc',        5::SMALLINT, 'Giọng hát và ngoại hình đều phù hợp thương hiệu, reach Gen Z female cực kỳ tốt. Rất ấn tượng.'),
    -- phuong-di-dau
    ('Shopee Explore Vietnam',      4::SMALLINT, 'Hành trình khám phá hấp dẫn, sản phẩm được giới thiệu tự nhiên. Video cần trim ngắn hơn để tăng retention.'),
    ('Highlands Northern Trails',   5::SMALLINT, 'Cảnh đẹp miền Bắc, nội dung travel chất lượng, cà phê Highlands xuất hiện đúng lúc đúng chỗ. Tuyệt vời!'),
    ('Biti''s Travel Companion',    4::SMALLINT, 'Giày được test thực tế trong hành trình dài, nội dung chân thực. Phần review cuối cần thêm CTA rõ hơn.'),
    -- tien-tien
    ('Biti''s Rhythm Moves',        4::SMALLINT, 'Phong cách nhảy kết hợp giày Biti''s trẻ trung, năng động. Nên thêm 1 cut stitch để nội dung dễ viral hơn.'),
    ('Highlands Music Morning',     5::SMALLINT, 'Nhạc acoustic với không gian Highlands cực kỳ chill. Reach target trẻ vượt mong đợi, engagement cao.'),
    ('Dove x Tien Tien Collab',     5::SMALLINT, 'Thông điệp chăm sóc bản thân được truyền tải nhẹ nhàng, sản phẩm được dùng tự nhiên. Conversion tốt.'),
    -- hoang-duyen
    ('TCH Beauty & Ritual',         4::SMALLINT, 'Góc quay đẹp, không gian TCH được tôn lên rất tốt. Cần thêm nhiều cảnh menu mới để kích thích order.'),
    ('Highlands Morning Glow',      5::SMALLINT, 'Lối sống buổi sáng tích cực kết hợp cà phê Highlands rất tự nhiên. View tốt, engagement cao hơn kỳ vọng.'),
    ('Vinamilk Beauty Inside Out',  4::SMALLINT, 'Thông điệp sắc đẹp từ bên trong lan toả tốt, sản phẩm Vinamilk được đưa vào hợp lý. Sẽ collab thêm.'),
    -- hoang-soi
    ('Vinamilk Strong & Fit',       4::SMALLINT, 'Nội dung thể lực phù hợp thông điệp "sữa khoẻ mạnh". Cần đa dạng setting quay hơn để tránh đơn điệu.'),
    ('Clear Fresh Day',             3::SMALLINT, 'Review chân thực nhưng phần giới thiệu sản phẩm còn rõ dấu quảng cáo. Cần tự nhiên hơn lần sau.'),
    -- tebefood
    ('Vinamilk Chef Secrets',       5::SMALLINT, 'Công thức nấu ăn kết hợp sản phẩm Vinamilk sáng tạo, visual food cực đẹp. Tỷ lệ save video cao bất thường.'),
    ('Highlands Kitchen Stories',   4::SMALLINT, 'Câu chuyện bếp núc kết hợp cà phê tốt. Brand mention Highlands cần rõ hơn trong phần caption.'),
    ('Knorr Cooking Class',         5::SMALLINT, 'Hướng dẫn nấu ăn chi tiết, sản phẩm Knorr được tích hợp hoàn hảo. Một trong những collab chất lượng nhất Q2.'),
    -- tuan-di-dau
    ('Vinamilk Da Nang Flavor',     4::SMALLINT, 'Ẩm thực Đà Nẵng kết hợp sữa Vinamilk tươi ngon, nội dung đậm bản sắc địa phương. Cần thêm CTA mạnh hơn.'),
    ('Shopee Street Food Trail',    5::SMALLINT, 'Series ẩm thực đường phố Đà Nẵng rất sống động, sản phẩm Shopee được link tự nhiên. Conversion ổn.'),
    ('Highlands Coast Route',       4::SMALLINT, 'Hành trình ven biển miền Trung kết hợp cà phê Highlands cuốn hút, reach khách du lịch tốt.'),
    -- nhan-phuong-chi-xu
    ('Biti''s Family Journey',      5::SMALLINT, 'Nội dung gia đình ấm áp, đúng tone mẫu và con. Giày Biti''s xuất hiện tự nhiên trong từng khoảnh khắc.'),
    ('Shopee Mom & Baby Box',       4::SMALLINT, 'Unbox mẹ và bé tương tác tốt, conversion vào Shopee ổn. Nên cải thiện âm thanh một chút cho rõ hơn.'),
    ('Vinamilk Growing Kids',       5::SMALLINT, 'Nội dung dinh dưỡng cho trẻ được thể hiện thuyết phục. Phụ huynh bình luận cực kỳ tích cực dưới video.'),
    -- duc-va-ly
    ('Vinamilk Couple Goals',       4::SMALLINT, 'Nội dung couple dễ thương, sữa Vinamilk được đưa vào hợp lý. Một số cảnh cần chú ý ánh sáng hơn.'),
    ('TCH Date Spot',               5::SMALLINT, 'Không gian hẹn hò tại TCH được thể hiện romantic và chill, đúng target couple 25-35 tuổi. Sẽ collab tiếp.'),
    ('Biti''s His & Hers',          5::SMALLINT, 'Couple mix-and-match giày đôi rất creative. Engagement tốt, nhiều bạn tag nhau trong phần bình luận.'),
    -- dumi
    ('Shopee Creator Finds',        4::SMALLINT, 'Review thiết bị sáng tạo chuẩn Gen Z, sản phẩm được highlight rõ. Cần thêm link mua hàng nổi bật hơn.'),
    ('Biti''s Gen Z Street',        3::SMALLINT, 'Phong cách streetwear thú vị nhưng brand message hơi mờ. Cần brief rõ về key message hơn lần tới.'),
    ('Vinamilk Youth Boost',        5::SMALLINT, 'Năng lượng trẻ trung bùng nổ, sản phẩm Vinamilk được đưa vào hành trình sáng tạo rất khéo. Viral tốt.'),
    -- nha-hieu-review
    ('Biti''s Honest Walk Test',    5::SMALLINT, 'Test giày Biti''s trong nhiều terrain rất thuyết phục, honest review được khán giả tin tưởng cao.'),
    ('Comfort Fabric Review',       4::SMALLINT, 'Review nước xả vải khách quan, cảm nhận thật. Cần mở rộng testing scenario để tăng credibility.'),
    ('TCH Honest Taste Test',       5::SMALLINT, 'Blind taste test cà phê sáng tạo, không gian TCH được giới thiệu rất khéo. View vượt mong đợi.'),
    -- linh-baeli
    ('TCH Skincare & Chill',        5::SMALLINT, 'Skincare routine kết hợp không gian TCH chill — concept độc đáo, thu hút đúng target beauty niche.'),
    ('Shopee Beauty Haul',          4::SMALLINT, 'Haul beauty từ Shopee Mall phong phú, sản phẩm review chi tiết. Cần thumbnail hấp dẫn hơn để tăng CTR.'),
    ('Biti''s Street Glam',         5::SMALLINT, 'Street style kết hợp làm đẹp trendy, đúng phong cách Gen Z nữ. Engagement và reach đều vượt kỳ vọng.'),
    -- ngo-thi-kim-yen
    ('TCH Hair & Latte',            4::SMALLINT, 'Hair care routine kết hợp café time tại TCH sáng tạo. Cần cải thiện lighting để showcase sản phẩm đẹp hơn.'),
    ('Shopee Beauty Week',          5::SMALLINT, 'Series beauty week 7 ngày với sản phẩm Shopee Mall đa dạng, engagement ổn định trong suốt campaign.'),
    ('Biti''s Femme Run',           4::SMALLINT, 'Nội dung chạy bộ năng động, phù hợp thông điệp active lifestyle của Biti''s. Một số cảnh quay hơi rung.'),
    -- hong-ngoc-van
    ('Shopee Flash Picks',          4::SMALLINT, 'Flash deal hàng ngày tương tác tốt, conversion vào Shopee đạt 75% KPI. Cần đều hơn về tần suất đăng bài.'),
    ('Vinamilk Glow Inside',        3::SMALLINT, 'Thông điệp đẹp từ bên trong chưa được thể hiện rõ trong nội dung. Cần brief sáng tạo kỹ hơn lần sau.'),
    ('TCH Refresh & Renew',         4::SMALLINT, 'Concept làm mới bản thân cùng TCH thú vị, execution ổn. Reach đúng target female 20-30.'),
    -- bu-ne
    ('Shopee Street Eats Live',     4::SMALLINT, 'Livestream ẩm thực đường phố vui nhộn, tương tác cao. Nên cải thiện setup mic để âm thanh tốt hơn.'),
    ('TCH Food Crawl',              5::SMALLINT, 'Hành trình foodie kết hợp cà phê TCH rất tự nhiên và thú vị. Nhiều bạn trẻ tag nhau dưới video.'),
    ('Knorr Vỉa Hè Ký Sự',         4::SMALLINT, 'Series ẩm thực vỉa hè kết hợp gia vị Knorr chân thực, gần gũi. Cần thêm 1 recipe video để convert tốt hơn.'),
    -- hat-tieu-foodie
    ('Biti''s Food Wanderer',       4::SMALLINT, 'Hành trình foodie kết hợp giày Biti''s travel-friendly sáng tạo. Cần showcase thêm tính năng giày thực chiến.'),
    ('Shopee Foodie Finds 2026',    5::SMALLINT, 'Khám phá đặc sản qua Shopee Mall chất lượng, mỗi tập đều có điểm nhấn riêng. Tỷ lệ mua hàng sau video tốt.'),
    ('Vinamilk 3 Miền Ngon',        5::SMALLINT, 'Visual ẩm thực 3 miền cực đẹp, sữa Vinamilk tích hợp tự nhiên trong từng bữa ăn. Ấn tượng nhất Q2!'),
    -- quynh-tran-ne
    ('TCH Cooking Marathon',        5::SMALLINT, 'Marathon nấu ăn livestream kết hợp cà phê TCH cực kỳ sáng tạo, giữ view suốt nhiều tiếng đồng hồ.'),
    ('Knorr Healthy Feast',         4::SMALLINT, 'Bữa ăn lành mạnh với gia vị Knorr được thực hiện bài bản, healthy lifestyle message rõ ràng và tích cực.'),
    ('Biti''s Home Chef Style',     3::SMALLINT, 'Concept home chef với giày Biti''s hơi gượng — kết hợp chưa đủ tự nhiên. Brief cần rõ hơn lần sau.'),
    -- eatwhning
    ('TCH Regional Feast',          5::SMALLINT, 'Bữa tiệc ẩm thực vùng miền kết hợp không gian TCH sang trọng — storytelling đẳng cấp, được chia sẻ rộng rãi.'),
    ('Vinamilk Vietnam Travels',    4::SMALLINT, 'Hành trình ẩm thực Việt kết hợp Vinamilk tươi ngon. Đôi chỗ caption cần rõ hơn về brand message.'),
    ('Knorr Food Explorer',         5::SMALLINT, 'Khám phá nguyên liệu địa phương kết hợp gia vị Knorr rất sáng tạo và educational. Tỷ lệ save cao.'),
    -- simple-man
    ('Vinamilk Minimalist Life',    4::SMALLINT, 'Phong cách sống tối giản kết hợp sữa Vinamilk hợp lý. Aesthetic rõ ràng, đúng tone minimalist content.'),
    ('TCH Work Anywhere',           3::SMALLINT, 'Concept làm việc mọi nơi tại TCH ổn nhưng nội dung hơi đơn điệu. Cần thêm góc nhìn đa dạng hơn lần sau.'),
    ('Clear Daily Routine',         4::SMALLINT, 'Routine hàng ngày với Clear được thực hiện clean, đúng aesthetic. Reach male target audience tốt.')

) AS r(campaign_title, rating, comment)
JOIN booking       b        ON b.campaign_title  = r.campaign_title
JOIN brand_profile bp       ON bp.id             = b.brand_profile_id
JOIN kol_profile   kp       ON kp.id             = b.kol_profile_id
JOIN app_user      brand_u  ON brand_u.id        = bp.user_id
JOIN app_user      kol_u    ON kol_u.id          = kp.user_id;

-- ---------------------------------------------------------------------
-- 3) Recompute kol_profile aggregates cho tất cả 20 KOL bị ảnh hưởng.
--    Dựa trên TOÀN BỘ BRAND_TO_KOL reviews (cũ + mới) theo từng KOL.
-- ---------------------------------------------------------------------
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
          -- V8 KOLs
          'nguyen-thi-thanh-nha', 'lam-bao-ngoc', 'phuong-di-dau', 'tien-tien',
          'hoang-duyen', 'hoang-soi', 'tebefood', 'tuan-di-dau',
          -- V14 KOLs
          'nhan-phuong-chi-xu', 'duc-va-ly', 'dumi', 'nha-hieu-review',
          'linh-baeli', 'ngo-thi-kim-yen', 'hong-ngoc-van', 'bu-ne',
          'hat-tieu-foodie', 'quynh-tran-ne', 'eatwhning', 'simple-man'
      )
    GROUP BY r.target_id
) stats
WHERE k.user_id = stats.kol_user_id;
