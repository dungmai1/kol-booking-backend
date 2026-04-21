-- =====================================================================
-- V8 — Seed KOL profiles (dev/test)
-- Nguồn: blog.vn.revu.net/category/thuc-don-influencer/influencer-data/
-- Crawl ngày: 2026-04-21, 8 hồ sơ influencer công khai
--
-- Mỗi KOL tạo ra bộ bản ghi:
--   app_user          (role=KOL, status=ACTIVE, email_verified=TRUE)
--   kol_profile       (status=APPROVED)
--   kol_social_channel (TikTok)
--   kol_category      (junction theo category.slug)
--
-- Password hash dùng bcrypt placeholder cho "password123" — chỉ phục vụ dev.
-- =====================================================================

-- -------------------- 1. Nguyễn Thị Thanh Nhã --------------------
WITH new_user AS (
    INSERT INTO app_user (email, password_hash, role, status, email_verified)
    VALUES ('thanhnha25091@seed.local',
            '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
            'KOL', 'ACTIVE', TRUE)
    RETURNING id
),
new_profile AS (
    INSERT INTO kol_profile (user_id, display_name, slug, avatar_url, bio, gender, status, avg_rating, review_count)
    SELECT id,
           'Nguyễn Thị Thanh Nhã',
           'nguyen-thi-thanh-nha',
           'https://blog.vn.revu.net/wp-content/uploads/2026/04/Nguyen-Thi-Thanh-Nha.jpg',
           'Nữ tuyển thủ bóng đá Việt Nam với hình ảnh mạnh mẽ trên sân cỏ và nữ tính trong đời sống, xây dựng cộng đồng athlete lifestyle trên TikTok.',
           'FEMALE', 'APPROVED', 0, 0
    FROM new_user
    RETURNING id
),
new_channel AS (
    INSERT INTO kol_social_channel (kol_profile_id, platform, url, username, follower_count, engagement_rate, verified)
    SELECT id, 'TIKTOK', 'https://www.tiktok.com/@thanhnha25091', 'thanhnha25091', 790300, 4.65, TRUE
    FROM new_profile
    RETURNING kol_profile_id
)
INSERT INTO kol_category (kol_profile_id, category_id)
SELECT c.kol_profile_id, cat.id
FROM new_channel c
CROSS JOIN category cat
WHERE cat.slug IN ('fitness', 'lifestyle');

-- -------------------- 2. Lâm Bảo Ngọc --------------------
WITH new_user AS (
    INSERT INTO app_user (email, password_hash, role, status, email_verified)
    VALUES ('lambaongoc@seed.local',
            '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
            'KOL', 'ACTIVE', TRUE)
    RETURNING id
),
new_profile AS (
    INSERT INTO kol_profile (user_id, display_name, slug, avatar_url, bio, gender, status, avg_rating, review_count)
    SELECT id,
           'Lâm Bảo Ngọc',
           'lam-bao-ngoc',
           'https://blog.vn.revu.net/wp-content/uploads/2026/04/Lam-Bao-Ngoc.jpg',
           'Nữ ca sĩ nổi bật với giọng hát nội lực. Được biết đến qua The Voice, The Masked Singer và Em xinh say hi.',
           'FEMALE', 'APPROVED', 0, 0
    FROM new_user
    RETURNING id
),
new_channel AS (
    INSERT INTO kol_social_channel (kol_profile_id, platform, url, username, follower_count, engagement_rate, verified)
    SELECT id, 'TIKTOK', 'https://www.tiktok.com/@lambaongoc_official', 'lambaongoc_official', 636700, 5.22, TRUE
    FROM new_profile
    RETURNING kol_profile_id
)
INSERT INTO kol_category (kol_profile_id, category_id)
SELECT c.kol_profile_id, cat.id
FROM new_channel c
CROSS JOIN category cat
WHERE cat.slug IN ('entertainment', 'lifestyle');

-- -------------------- 3. Tiên Tiên --------------------
WITH new_user AS (
    INSERT INTO app_user (email, password_hash, role, status, email_verified)
    VALUES ('tien.tien@seed.local',
            '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
            'KOL', 'ACTIVE', TRUE)
    RETURNING id
),
new_profile AS (
    INSERT INTO kol_profile (user_id, display_name, slug, avatar_url, bio, gender, status, avg_rating, review_count)
    SELECT id,
           'Tiên Tiên',
           'tien-tien',
           'https://blog.vn.revu.net/wp-content/uploads/2026/04/Tien-Tien.jpg',
           'Hit-maker của Vpop với khả năng sáng tác mạnh mẽ. Trên TikTok xây dựng hình ảnh ngẫu hứng nhưng giữ cá tính riêng, vừa gia trưởng vừa đáng yêu.',
           'FEMALE', 'APPROVED', 0, 0
    FROM new_user
    RETURNING id
),
new_channel AS (
    INSERT INTO kol_social_channel (kol_profile_id, platform, url, username, follower_count, engagement_rate, verified)
    SELECT id, 'TIKTOK', 'https://www.tiktok.com/@tien.tientien.tien', 'tien.tientien.tien', 426600, 7.52, TRUE
    FROM new_profile
    RETURNING kol_profile_id
)
INSERT INTO kol_category (kol_profile_id, category_id)
SELECT c.kol_profile_id, cat.id
FROM new_channel c
CROSS JOIN category cat
WHERE cat.slug IN ('entertainment', 'lifestyle');

-- -------------------- 4. Hoàng Sỏi (Vương Hoàng) --------------------
WITH new_user AS (
    INSERT INTO app_user (email, password_hash, role, status, email_verified)
    VALUES ('hoangsoi2809@seed.local',
            '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
            'KOL', 'ACTIVE', TRUE)
    RETURNING id
),
new_profile AS (
    INSERT INTO kol_profile (user_id, display_name, slug, avatar_url, bio, status, avg_rating, review_count)
    SELECT id,
           'Hoàng Sỏi',
           'hoang-soi',
           'https://blog.vn.revu.net/wp-content/uploads/2026/04/Hoang-Soi.jpg',
           'Creator nổi bật với phong cách review thẳng thắn, chân thực và đáng tin cậy trên TikTok, chuyên mảng FMCG, thời trang và sản phẩm tiêu dùng.',
           'APPROVED', 0, 0
    FROM new_user
    RETURNING id
),
new_channel AS (
    INSERT INTO kol_social_channel (kol_profile_id, platform, url, username, follower_count, engagement_rate, verified)
    SELECT id, 'TIKTOK', 'https://www.tiktok.com/@hoangsoi2809', 'hoangsoi2809', 82800, 1.41, TRUE
    FROM new_profile
    RETURNING kol_profile_id
)
INSERT INTO kol_category (kol_profile_id, category_id)
SELECT c.kol_profile_id, cat.id
FROM new_channel c
CROSS JOIN category cat
WHERE cat.slug IN ('lifestyle', 'fashion');

-- -------------------- 5. Phượng Đi Đâu --------------------
WITH new_user AS (
    INSERT INTO app_user (email, password_hash, role, status, email_verified)
    VALUES ('phuongdidau@seed.local',
            '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
            'KOL', 'ACTIVE', TRUE)
    RETURNING id
),
new_profile AS (
    INSERT INTO kol_profile (user_id, display_name, slug, avatar_url, bio, gender, status, avg_rating, review_count)
    SELECT id,
           'Phượng Đi Đâu',
           'phuong-di-dau',
           'https://blog.vn.revu.net/wp-content/uploads/2026/04/Phuong-Di-Dau.jpg',
           'Travel creator và lifestyle influencer chuyển mình từ những chuyến phượt mô-tô tự thân thành cộng đồng truyền cảm hứng sống và khởi nghiệp.',
           'FEMALE', 'APPROVED', 0, 0
    FROM new_user
    RETURNING id
),
new_channel AS (
    INSERT INTO kol_social_channel (kol_profile_id, platform, url, username, follower_count, engagement_rate, verified)
    SELECT id, 'TIKTOK', 'https://www.tiktok.com/@phuongdidau', 'phuongdidau', 536100, 4.25, TRUE
    FROM new_profile
    RETURNING kol_profile_id
)
INSERT INTO kol_category (kol_profile_id, category_id)
SELECT c.kol_profile_id, cat.id
FROM new_channel c
CROSS JOIN category cat
WHERE cat.slug IN ('travel', 'lifestyle');

-- -------------------- 6. Tebefood --------------------
WITH new_user AS (
    INSERT INTO app_user (email, password_hash, role, status, email_verified)
    VALUES ('tebefood@seed.local',
            '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
            'KOL', 'ACTIVE', TRUE)
    RETURNING id
),
new_profile AS (
    INSERT INTO kol_profile (user_id, display_name, slug, avatar_url, bio, status, avg_rating, review_count)
    SELECT id,
           'Tebefood',
           'tebefood',
           'https://blog.vn.revu.net/wp-content/uploads/2026/04/Tebefood.jpg',
           'Chuyên review món ăn quốc tế tại Việt Nam với nội dung được đầu tư kỹ về hình ảnh và đồ họa.',
           'APPROVED', 0, 0
    FROM new_user
    RETURNING id
),
new_channel AS (
    INSERT INTO kol_social_channel (kol_profile_id, platform, url, username, follower_count, engagement_rate, verified)
    SELECT id, 'TIKTOK', 'https://www.tiktok.com/@tebefood', 'tebefood', 551300, 4.90, TRUE
    FROM new_profile
    RETURNING kol_profile_id
)
INSERT INTO kol_category (kol_profile_id, category_id)
SELECT c.kol_profile_id, cat.id
FROM new_channel c
CROSS JOIN category cat
WHERE cat.slug IN ('food');

-- -------------------- 7. Tuấn Đi Đâu --------------------
WITH new_user AS (
    INSERT INTO app_user (email, password_hash, role, status, email_verified)
    VALUES ('tuandidau@seed.local',
            '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
            'KOL', 'ACTIVE', TRUE)
    RETURNING id
),
new_profile AS (
    INSERT INTO kol_profile (user_id, display_name, slug, avatar_url, bio, city, status, avg_rating, review_count)
    SELECT id,
           'Tuấn Đi Đâu',
           'tuan-di-dau',
           'https://blog.vn.revu.net/wp-content/uploads/2026/04/Tuan-Di-Dau.jpg',
           'Creator nổi bật trong lĩnh vực review ăn uống và du lịch tại Đà Nẵng, chuyên nội dung trải nghiệm ngoài trời.',
           'Đà Nẵng', 'APPROVED', 0, 0
    FROM new_user
    RETURNING id
),
new_channel AS (
    INSERT INTO kol_social_channel (kol_profile_id, platform, url, username, follower_count, engagement_rate, verified)
    SELECT id, 'TIKTOK', 'https://www.tiktok.com/@tuandidau', 'tuandidau', 293100, 2.64, TRUE
    FROM new_profile
    RETURNING kol_profile_id
)
INSERT INTO kol_category (kol_profile_id, category_id)
SELECT c.kol_profile_id, cat.id
FROM new_channel c
CROSS JOIN category cat
WHERE cat.slug IN ('food', 'travel');

-- -------------------- 8. Hoàng Duyên --------------------
WITH new_user AS (
    INSERT INTO app_user (email, password_hash, role, status, email_verified)
    VALUES ('hoangduyen.dreams@seed.local',
            '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
            'KOL', 'ACTIVE', TRUE)
    RETURNING id
),
new_profile AS (
    INSERT INTO kol_profile (user_id, display_name, slug, avatar_url, bio, gender, status, avg_rating, review_count)
    SELECT id,
           'Hoàng Duyên',
           'hoang-duyen',
           'https://blog.vn.revu.net/wp-content/uploads/2026/04/Hoang-Duyen-1.jpg',
           'Nữ ca sĩ Gen Z với hình tượng dịu dàng, nổi tiếng qua các hit như "Chàng trai sơ mi hồng" và chương trình "Em xinh say hi".',
           'FEMALE', 'APPROVED', 0, 0
    FROM new_user
    RETURNING id
),
new_channel AS (
    INSERT INTO kol_social_channel (kol_profile_id, platform, url, username, follower_count, engagement_rate, verified)
    SELECT id, 'TIKTOK', 'https://www.tiktok.com/@hoangduyen.dreams', 'hoangduyen.dreams', 592500, 10.44, TRUE
    FROM new_profile
    RETURNING kol_profile_id
)
INSERT INTO kol_category (kol_profile_id, category_id)
SELECT c.kol_profile_id, cat.id
FROM new_channel c
CROSS JOIN category cat
WHERE cat.slug IN ('entertainment', 'beauty', 'fashion');
