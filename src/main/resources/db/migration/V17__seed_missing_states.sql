-- =====================================================================
-- V17 — Seed bổ sung các trạng thái còn thiếu cho frontend test
--
-- Mục tiêu:
--   1. KOL SUBMITTED (2 hồ sơ chờ duyệt) — kiểm thử admin approve/reject
--   2. Booking ở các trạng thái non-COMPLETED còn thiếu:
--      PENDING, ACCEPTED, REJECTED, CANCELLED, IN_PROGRESS, DELIVERED
--   3. Wallet + wallet_transaction cho cả BRAND lẫn KOL — kiểm thử
--      payment flow (DEPOSIT / HOLD / RELEASE / FEE)
--   4. Notification cho nhiều NotificationType khác nhau
--   5. 1 subscription ACTIVE (gói FREE) cho mỗi BRAND
--
-- Phụ thuộc:
--   - V2 (app_user, role enum)
--   - V3 (brand_profile, kol_profile)
--   - V5 (booking, booking_status_history)
--   - V6 (wallet, wallet_transaction)
--   - V7 (notification)
--   - V8 (KOL seed — tra qua slug)
--   - V15 (Brand seed + COMPLETED bookings)
--   - V16 (plan FREE)
--
-- Quy ước: tra cứu qua email / slug / company_name / campaign_title,
-- KHÔNG hardcode primary key.
-- Password: "password123" — bcrypt đã được V11.2 thống nhất.
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1) KOL SUBMITTED — 2 hồ sơ chờ duyệt
-- ---------------------------------------------------------------------
INSERT INTO app_user (email, password_hash, role, status, email_verified) VALUES
    ('kol.submitted1@seed.local', '$2a$10$ZJo6Pxi.C7ichgif9MD6DOZCjfn3AxLh2q18qP39T5qHtxF3u9d7C', 'KOL', 'ACTIVE', TRUE),
    ('kol.submitted2@seed.local', '$2a$10$ZJo6Pxi.C7ichgif9MD6DOZCjfn3AxLh2q18qP39T5qHtxF3u9d7C', 'KOL', 'ACTIVE', TRUE);

INSERT INTO kol_profile (user_id, display_name, slug, avatar_url, bio, gender, status, avg_rating, review_count)
SELECT u.id, m.display_name, m.slug, m.avatar_url, m.bio, m.gender, 'PENDING_REVIEW', 0, 0
FROM (VALUES
    ('kol.submitted1@seed.local',
     'Nguyễn Đăng Khoa',
     'nguyen-dang-khoa-submitted',
     'https://blog.vn.revu.net/wp-content/uploads/2026/04/placeholder-male.jpg',
     'Reviewer công nghệ trẻ chuyên unbox gadget Android, đang chờ admin duyệt hồ sơ.',
     'MALE'),
    ('kol.submitted2@seed.local',
     'Trần Mỹ Anh',
     'tran-my-anh-submitted',
     'https://blog.vn.revu.net/wp-content/uploads/2026/04/placeholder-female.jpg',
     'Beauty creator tập trung skincare cho da nhạy cảm — hồ sơ đã submit lần đầu.',
     'FEMALE')
) AS m(email, display_name, slug, avatar_url, bio, gender)
JOIN app_user u ON u.email = m.email;

INSERT INTO kol_social_channel (kol_profile_id, platform, url, username, follower_count, engagement_rate, verified)
SELECT kp.id, m.platform, m.url, m.username, m.follower_count, m.engagement_rate, m.verified
FROM (VALUES
    ('nguyen-dang-khoa-submitted', 'TIKTOK', 'https://www.tiktok.com/@khoa.tech',  'khoa.tech', 125000, 3.85, FALSE),
    ('tran-my-anh-submitted',      'TIKTOK', 'https://www.tiktok.com/@myanh.skin', 'myanh.skin', 88000, 4.42, FALSE)
) AS m(slug, platform, url, username, follower_count, engagement_rate, verified)
JOIN kol_profile kp ON kp.slug = m.slug;

INSERT INTO kol_category (kol_profile_id, category_id)
SELECT kp.id, cat.id
FROM kol_profile kp
JOIN category cat ON cat.slug IN ('tech', 'lifestyle')
WHERE kp.slug = 'nguyen-dang-khoa-submitted';

INSERT INTO kol_category (kol_profile_id, category_id)
SELECT kp.id, cat.id
FROM kol_profile kp
JOIN category cat ON cat.slug IN ('beauty', 'lifestyle')
WHERE kp.slug = 'tran-my-anh-submitted';

-- ---------------------------------------------------------------------
-- 2) Bookings ở các trạng thái còn thiếu
--    Mỗi campaign_title duy nhất → dễ tra cứu cho test/QA.
-- ---------------------------------------------------------------------
INSERT INTO booking (
    brand_profile_id, kol_profile_id, campaign_title, campaign_brief,
    deliverables, budget, start_date, end_date, status,
    reject_reason, cancel_reason
)
SELECT
    bp.id, kp.id, m.campaign_title, m.campaign_brief,
    m.deliverables, m.budget, m.start_date, m.end_date, m.status,
    m.reject_reason, m.cancel_reason
FROM (VALUES
    -- PENDING — brand mới gửi yêu cầu, KOL chưa phản hồi
    ('Vinamilk', 'nguyen-thi-thanh-nha', 'Vinamilk Summer Sport 2026',
     'Đề xuất series TikTok mùa hè kết hợp thể thao và sữa Vinamilk.',
     '2 TikTok videos', 20000000.00::NUMERIC(15,2),
     DATE '2026-06-15', DATE '2026-07-05', 'PENDING', NULL, NULL),

    -- ACCEPTED — KOL đã đồng ý, brand chuẩn bị thanh toán
    ('Shopee Vietnam', 'lam-bao-ngoc', 'Shopee 5.5 Music Theme',
     'Sáng tác music theme cho campaign sale 5/5.',
     '1 TikTok music video', 38000000.00::NUMERIC(15,2),
     DATE '2026-04-20', DATE '2026-05-05', 'ACCEPTED', NULL, NULL),

    -- REJECTED — KOL từ chối vì conflict lịch
    ('The Coffee House', 'tien-tien', 'TCH Acoustic Night',
     'Mời tham gia live acoustic tại TCH chi nhánh Q1.',
     '1 live performance + 1 TikTok recap', 22000000.00::NUMERIC(15,2),
     DATE '2026-05-10', DATE '2026-05-25', 'REJECTED',
     'Lịch tour cá nhân không khớp với mốc go-live, đành từ chối lần này.', NULL),

    -- CANCELLED — brand huỷ trước khi KOL phản hồi
    ('Biti''s', 'phuong-di-dau', 'Bitis Trekking Series',
     'Travel + thử giày trekking Biti''s tại Tây Bắc.',
     '3 TikTok travel videos', 30000000.00::NUMERIC(15,2),
     DATE '2026-05-01', DATE '2026-05-30', 'CANCELLED',
     NULL, 'Brand điều chỉnh ngân sách Q2, tạm hoãn chiến dịch.'),

    -- IN_PROGRESS — đã accept + thanh toán, đang sản xuất content
    ('Vinamilk', 'hoang-soi', 'Vinamilk Probi Daily',
     'Daily check-in kèm Probi mỗi sáng.',
     '5 TikTok shorts', 16000000.00::NUMERIC(15,2),
     DATE '2026-05-15', DATE '2026-06-15', 'IN_PROGRESS', NULL, NULL),

    -- IN_PROGRESS — chiến dịch thứ 2 để kiểm thử list paginated
    ('Shopee Vietnam', 'tebefood', 'Shopee Kitchen Reno',
     'Renovate kitchen với items Shopee Home & Living.',
     '2 TikTok long videos', 26000000.00::NUMERIC(15,2),
     DATE '2026-05-10', DATE '2026-06-05', 'IN_PROGRESS', NULL, NULL),

    -- DELIVERED — KOL đã nộp deliverable, chờ brand approve để complete
    ('The Coffee House', 'tuan-di-dau', 'TCH Hanoi Weekend',
     'Trải nghiệm cuối tuần tại TCH Hà Nội — vlog.',
     '1 TikTok vlog', 13000000.00::NUMERIC(15,2),
     DATE '2026-04-25', DATE '2026-05-15', 'DELIVERED', NULL, NULL)
) AS m(brand_name, kol_slug, campaign_title, campaign_brief, deliverables, budget, start_date, end_date, status, reject_reason, cancel_reason)
JOIN brand_profile bp ON bp.company_name = m.brand_name
JOIN kol_profile kp   ON kp.slug         = m.kol_slug;

-- Booking deliverable cho các booking IN_PROGRESS / DELIVERED
INSERT INTO booking_deliverable (booking_id, type, platform, submitted_url, submitted_at, status)
SELECT b.id, m.type, m.platform, m.submitted_url, m.submitted_at, m.status
FROM (VALUES
    ('Vinamilk Probi Daily', 'VIDEO', 'TIKTOK', NULL,                                    NULL,                            'PENDING'),
    ('Shopee Kitchen Reno',  'VIDEO', 'TIKTOK', NULL,                                    NULL,                            'PENDING'),
    ('TCH Hanoi Weekend',    'VIDEO', 'TIKTOK', 'https://www.tiktok.com/@tuan/video/1', NOW() - INTERVAL '3 days',       'SUBMITTED')
) AS m(campaign_title, type, platform, submitted_url, submitted_at, status)
JOIN booking b ON b.campaign_title = m.campaign_title;

-- Booking status history — đánh dấu lịch sử chuyển trạng thái cho bookings mới
INSERT INTO booking_status_history (booking_id, from_status, to_status, changed_by_user, note)
SELECT b.id, m.from_status, m.to_status, NULL, m.note
FROM (VALUES
    ('Vinamilk Summer Sport 2026', NULL,         'PENDING',     'Booking khởi tạo bởi brand.'),
    ('Shopee 5.5 Music Theme',     NULL,         'PENDING',     'Booking khởi tạo bởi brand.'),
    ('Shopee 5.5 Music Theme',     'PENDING',    'ACCEPTED',    'KOL chấp nhận chiến dịch.'),
    ('TCH Acoustic Night',         NULL,         'PENDING',     'Booking khởi tạo bởi brand.'),
    ('TCH Acoustic Night',         'PENDING',    'REJECTED',    'KOL từ chối.'),
    ('Bitis Trekking Series',      NULL,         'PENDING',     'Booking khởi tạo bởi brand.'),
    ('Bitis Trekking Series',      'PENDING',    'CANCELLED',   'Brand huỷ.'),
    ('Vinamilk Probi Daily',       NULL,         'PENDING',     'Booking khởi tạo bởi brand.'),
    ('Vinamilk Probi Daily',       'PENDING',    'ACCEPTED',    'KOL chấp nhận.'),
    ('Vinamilk Probi Daily',       'ACCEPTED',   'IN_PROGRESS', 'Brand thanh toán, KOL bắt đầu sản xuất.'),
    ('Shopee Kitchen Reno',        NULL,         'PENDING',     'Booking khởi tạo bởi brand.'),
    ('Shopee Kitchen Reno',        'PENDING',    'ACCEPTED',    'KOL chấp nhận.'),
    ('Shopee Kitchen Reno',        'ACCEPTED',   'IN_PROGRESS', 'Brand thanh toán, KOL bắt đầu sản xuất.'),
    ('TCH Hanoi Weekend',          NULL,         'PENDING',     'Booking khởi tạo bởi brand.'),
    ('TCH Hanoi Weekend',          'PENDING',    'ACCEPTED',    'KOL chấp nhận.'),
    ('TCH Hanoi Weekend',          'ACCEPTED',   'IN_PROGRESS', 'Brand thanh toán, KOL bắt đầu sản xuất.'),
    ('TCH Hanoi Weekend',          'IN_PROGRESS','DELIVERED',   'KOL submit deliverable, chờ brand duyệt.')
) AS m(campaign_title, from_status, to_status, note)
JOIN booking b ON b.campaign_title = m.campaign_title;

-- ---------------------------------------------------------------------
-- 3) Wallet — tạo cho tất cả seed user (BRAND + KOL)
--    Brand được nạp sẵn để test deposit/booking flow,
--    KOL có balance_held để test release sau khi complete booking.
-- ---------------------------------------------------------------------
INSERT INTO wallet (user_id, balance_available, balance_held, currency)
SELECT u.id, m.balance_available, m.balance_held, 'VND'
FROM (VALUES
    -- BRAND wallets — nạp sẵn 200tr để chạy nhiều booking
    ('brand.vinamilk@seed.local',     200000000.00::NUMERIC(18,2), 0.00::NUMERIC(18,2)),
    ('brand.shopee@seed.local',       200000000.00::NUMERIC(18,2), 0.00::NUMERIC(18,2)),
    ('brand.tch@seed.local',          150000000.00::NUMERIC(18,2), 0.00::NUMERIC(18,2)),
    ('brand.bitis@seed.local',        150000000.00::NUMERIC(18,2), 0.00::NUMERIC(18,2)),

    -- KOL wallets — đã có 1 phần balance_available từ các COMPLETED booking
    -- (giả lập sau khi RELEASE trừ FEE 10%)
    ('thanhnha25091@seed.local', 16200000.00::NUMERIC(18,2), 0.00::NUMERIC(18,2)),
    ('lambaongoc@seed.local',    28800000.00::NUMERIC(18,2), 0.00::NUMERIC(18,2)),
    ('tien.tien@seed.local',     40500000.00::NUMERIC(18,2), 0.00::NUMERIC(18,2)),
    ('hoangsoi2809@seed.local',  27900000.00::NUMERIC(18,2), 14400000.00::NUMERIC(18,2)),
    ('phuongdidau@seed.local',   37800000.00::NUMERIC(18,2), 0.00::NUMERIC(18,2)),
    ('tebefood@seed.local',      30600000.00::NUMERIC(18,2), 23400000.00::NUMERIC(18,2)),
    ('tuandidau@seed.local',     10800000.00::NUMERIC(18,2), 11700000.00::NUMERIC(18,2)),
    ('hoangduyen.dreams@seed.local', 45000000.00::NUMERIC(18,2), 0.00::NUMERIC(18,2)),

    -- SUBMITTED KOL wallets — chưa có giao dịch nào
    ('kol.submitted1@seed.local', 0.00::NUMERIC(18,2), 0.00::NUMERIC(18,2)),
    ('kol.submitted2@seed.local', 0.00::NUMERIC(18,2), 0.00::NUMERIC(18,2))
) AS m(email, balance_available, balance_held)
JOIN app_user u ON u.email = m.email;

-- ---------------------------------------------------------------------
-- 4) Wallet transactions — minh hoạ DEPOSIT / HOLD / RELEASE / FEE
-- ---------------------------------------------------------------------

-- 4a) Brand DEPOSIT — nạp tiền vào ví
INSERT INTO wallet_transaction (wallet_id, type, amount, balance_after, booking_id, external_ref, status, note)
SELECT w.id, 'DEPOSIT', m.amount, m.amount, NULL, m.external_ref, 'SUCCESS', m.note
FROM (VALUES
    ('brand.vinamilk@seed.local',     200000000.00::NUMERIC(18,2), 'DEP-VNM-001',   'Brand Vinamilk nạp ví đầu kỳ.'),
    ('brand.shopee@seed.local',       200000000.00::NUMERIC(18,2), 'DEP-SHOPEE-001','Brand Shopee nạp ví đầu kỳ.'),
    ('brand.tch@seed.local',          150000000.00::NUMERIC(18,2), 'DEP-TCH-001',   'Brand TCH nạp ví đầu kỳ.'),
    ('brand.bitis@seed.local',        150000000.00::NUMERIC(18,2), 'DEP-BITIS-001', 'Brand Biti''s nạp ví đầu kỳ.')
) AS m(email, amount, external_ref, note)
JOIN app_user u ON u.email = m.email
JOIN wallet w   ON w.user_id = u.id;

-- 4b) Brand HOLD — booking IN_PROGRESS giữ tiền của brand
INSERT INTO wallet_transaction (wallet_id, type, amount, balance_after, booking_id, external_ref, status, note)
SELECT w.id, 'HOLD', m.amount, m.balance_after, b.id, m.external_ref, 'SUCCESS', m.note
FROM (VALUES
    ('brand.vinamilk@seed.local',   'Vinamilk Probi Daily', 16000000.00::NUMERIC(18,2), 184000000.00::NUMERIC(18,2), 'HOLD-VNM-PROBI',   'Giữ tiền cho booking Vinamilk Probi Daily.'),
    ('brand.shopee@seed.local',     'Shopee Kitchen Reno',  26000000.00::NUMERIC(18,2), 174000000.00::NUMERIC(18,2), 'HOLD-SHOPEE-KIT',  'Giữ tiền cho booking Shopee Kitchen Reno.'),
    ('brand.tch@seed.local',        'TCH Hanoi Weekend',    13000000.00::NUMERIC(18,2), 137000000.00::NUMERIC(18,2), 'HOLD-TCH-HN',      'Giữ tiền cho booking TCH Hanoi Weekend.')
) AS m(email, campaign_title, amount, balance_after, external_ref, note)
JOIN app_user u ON u.email = m.email
JOIN wallet w   ON w.user_id = u.id
JOIN booking b  ON b.campaign_title = m.campaign_title;

-- 4c) KOL HOLD — phần đối ứng: ví KOL ghi nhận khoản đang chờ release
INSERT INTO wallet_transaction (wallet_id, type, amount, balance_after, booking_id, external_ref, status, note)
SELECT w.id, 'HOLD', m.amount, m.balance_after, b.id, m.external_ref, 'SUCCESS', m.note
FROM (VALUES
    ('hoangsoi2809@seed.local',  'Vinamilk Probi Daily', 14400000.00::NUMERIC(18,2), 14400000.00::NUMERIC(18,2), 'HOLD-KOL-PROBI', 'Tiền chờ release sau fee 10%.'),
    ('tebefood@seed.local',      'Shopee Kitchen Reno',  23400000.00::NUMERIC(18,2), 23400000.00::NUMERIC(18,2), 'HOLD-KOL-KIT',   'Tiền chờ release sau fee 10%.'),
    ('tuandidau@seed.local',     'TCH Hanoi Weekend',    11700000.00::NUMERIC(18,2), 11700000.00::NUMERIC(18,2), 'HOLD-KOL-HN',    'Tiền chờ release sau fee 10%.')
) AS m(email, campaign_title, amount, balance_after, external_ref, note)
JOIN app_user u ON u.email = m.email
JOIN wallet w   ON w.user_id = u.id
JOIN booking b  ON b.campaign_title = m.campaign_title;

-- 4d) RELEASE (mẫu) — một booking đã COMPLETED của Vinamilk x Thanh Nhã
INSERT INTO wallet_transaction (wallet_id, type, amount, balance_after, booking_id, external_ref, status, note)
SELECT w.id, 'RELEASE', 16200000.00::NUMERIC(18,2), 16200000.00::NUMERIC(18,2), b.id, 'REL-VNM-ATHLETE', 'SUCCESS',
       'Release 90% sau khi brand approve deliverable.'
FROM app_user u
JOIN wallet w  ON w.user_id = u.id
JOIN booking b ON b.campaign_title = 'Vinamilk Athlete Drive 2026'
WHERE u.email = 'thanhnha25091@seed.local';

-- 4e) FEE (mẫu) — 10% phí nền tảng tương ứng booking trên
INSERT INTO wallet_transaction (wallet_id, type, amount, balance_after, booking_id, external_ref, status, note)
SELECT w.id, 'FEE', 1800000.00::NUMERIC(18,2), 16200000.00::NUMERIC(18,2), b.id, 'FEE-VNM-ATHLETE', 'SUCCESS',
       'Phí nền tảng 10% trên budget 18tr.'
FROM app_user u
JOIN wallet w  ON w.user_id = u.id
JOIN booking b ON b.campaign_title = 'Vinamilk Athlete Drive 2026'
WHERE u.email = 'thanhnha25091@seed.local';

-- ---------------------------------------------------------------------
-- 5) Withdraw requests — kiểm thử KOL rút tiền, đủ trạng thái
-- ---------------------------------------------------------------------
INSERT INTO withdraw_request (kol_user_id, amount, bank_name, bank_account, account_name, status, reject_reason, processed_at)
SELECT u.id, m.amount, m.bank_name, m.bank_account, m.account_name, m.status, m.reject_reason, m.processed_at
FROM (VALUES
    -- PENDING
    ('lambaongoc@seed.local',        10000000.00::NUMERIC(18,2), 'Vietcombank', '0123456789', 'LAM BAO NGOC',  'PENDING',  NULL, NULL),
    -- APPROVED (admin đã duyệt, chờ chuyển khoản)
    ('phuongdidau@seed.local',        8000000.00::NUMERIC(18,2), 'Techcombank', '1234567890', 'NGUYEN PHUONG', 'APPROVED', NULL, NOW() - INTERVAL '1 day'),
    -- PAID (đã chuyển xong)
    ('hoangduyen.dreams@seed.local', 15000000.00::NUMERIC(18,2), 'MB Bank',     '9876543210', 'HOANG DUYEN',   'PAID',     NULL, NOW() - INTERVAL '5 day'),
    -- REJECTED (sai thông tin tài khoản)
    ('tien.tien@seed.local',          5000000.00::NUMERIC(18,2), 'ACB',         '5555555555', 'NGUYEN TIEN',   'REJECTED', 'Tên tài khoản không khớp CMND đã đăng ký.', NOW() - INTERVAL '2 day')
) AS m(email, amount, bank_name, bank_account, account_name, status, reject_reason, processed_at)
JOIN app_user u ON u.email = m.email;

-- ---------------------------------------------------------------------
-- 6) Notifications — phủ nhiều NotificationType cho test danh sách + unread
-- ---------------------------------------------------------------------
INSERT INTO notification (user_id, type, title, message, link, read_at)
SELECT u.id, m.type, m.title, m.message, m.link, m.read_at
FROM (VALUES
    -- KOL Thanh Nhã: nhận booking + review
    ('thanhnha25091@seed.local', 'BOOKING_CREATED',  'Yêu cầu booking mới', 'Vinamilk đã gửi yêu cầu booking "Vinamilk Athlete Drive 2026".', '/kol/bookings', NOW() - INTERVAL '30 day'),
    ('thanhnha25091@seed.local', 'BOOKING_COMPLETED','Booking hoàn tất',    'Booking "Vinamilk Athlete Drive 2026" đã hoàn tất, tiền sẽ về ví.',   '/kol/bookings', NOW() - INTERVAL '20 day'),
    ('thanhnha25091@seed.local', 'REVIEW_RECEIVED',  'Đánh giá mới',        'Vinamilk đã đánh giá bạn 5 sao.',                                     '/kol/reviews',  NULL),
    ('thanhnha25091@seed.local', 'PAYMENT_SUCCESS',  'Nhận thanh toán',     'Bạn vừa nhận 16,200,000đ từ booking đã hoàn tất.',                    '/wallet',       NULL),

    -- KOL Hoàng Sói: đang in-progress
    ('hoangsoi2809@seed.local', 'BOOKING_ACCEPTED', 'Đã nhận booking',     'Bạn đã chấp nhận booking "Vinamilk Probi Daily".',                    '/kol/bookings', NOW() - INTERVAL '5 day'),
    ('hoangsoi2809@seed.local', 'PAYMENT_SUCCESS',  'Brand đã thanh toán', 'Brand đã thanh toán, tiền sẽ release sau khi bạn nộp deliverable.',   '/wallet',       NULL),

    -- KOL Tuấn: đã deliver, chờ duyệt
    ('tuandidau@seed.local',     'DELIVERABLE_SUBMITTED', 'Đã nộp deliverable', 'Deliverable cho "TCH Hanoi Weekend" đã gửi, chờ brand duyệt.',    '/kol/bookings', NULL),

    -- KOL Tiên Tiên: bị từ chối withdraw
    ('tien.tien@seed.local',     'WITHDRAW_REJECTED', 'Yêu cầu rút tiền bị từ chối', 'Yêu cầu rút 5,000,000đ bị từ chối: tên TK không khớp CMND.', '/wallet/withdrawals', NULL),

    -- KOL SUBMITTED đang chờ admin duyệt (dùng NEW_MESSAGE — chưa có sự kiện
    -- PROFILE_APPROVED/REJECTED nên tạm dùng NEW_MESSAGE cho onboarding)
    ('kol.submitted1@seed.local', 'NEW_MESSAGE', 'Hồ sơ đã gửi duyệt', 'Hồ sơ KOL của bạn đã được gửi tới admin, kết quả trong 24-48h.',     '/kol/profile', NOW() - INTERVAL '2 day'),
    ('kol.submitted2@seed.local', 'NEW_MESSAGE', 'Hồ sơ đã gửi duyệt', 'Hồ sơ KOL của bạn đã được gửi tới admin, kết quả trong 24-48h.',     '/kol/profile', NULL),

    -- BRAND Vinamilk
    ('brand.vinamilk@seed.local', 'BOOKING_ACCEPTED', 'KOL chấp nhận',     'Thanh Nhã đã chấp nhận booking "Vinamilk Athlete Drive 2026".',       '/brand/bookings', NOW() - INTERVAL '25 day'),
    ('brand.vinamilk@seed.local', 'DELIVERABLE_SUBMITTED', 'Deliverable mới', 'Hoàng Sói vừa nộp deliverable cho "Vinamilk Probi Daily".',         '/brand/bookings', NULL),

    -- BRAND Shopee
    ('brand.shopee@seed.local',   'BOOKING_ACCEPTED', 'KOL chấp nhận',     'Lâm Bảo Ngọc đã chấp nhận booking "Shopee 5.5 Music Theme".',         '/brand/bookings', NOW() - INTERVAL '7 day'),
    ('brand.shopee@seed.local',   'NEW_MESSAGE',      'Tin nhắn mới',      'KOL vừa gửi tin nhắn về booking "Shopee Kitchen Reno".',              '/brand/bookings', NULL),

    -- BRAND TCH
    ('brand.tch@seed.local',      'BOOKING_REJECTED', 'KOL từ chối',       'Tiên Tiên đã từ chối booking "TCH Acoustic Night".',                  '/brand/bookings', NULL),
    ('brand.tch@seed.local',      'DELIVERABLE_SUBMITTED', 'Deliverable mới', 'Tuấn Đi Đâu vừa nộp deliverable cho "TCH Hanoi Weekend".',          '/brand/bookings', NULL),

    -- BRAND Biti's
    ('brand.bitis@seed.local',    'BOOKING_CANCELLED', 'Booking đã huỷ',   'Booking "Bitis Trekking Series" đã được huỷ.',                        '/brand/bookings', NOW() - INTERVAL '3 day')
) AS m(email, type, title, message, link, read_at)
JOIN app_user u ON u.email = m.email;

-- ---------------------------------------------------------------------
-- 7) Subscriptions ACTIVE (gói FREE) cho mỗi BRAND
--    Để frontend test GET /api/v1/subscriptions/me trả về ACTIVE.
-- ---------------------------------------------------------------------
INSERT INTO subscription (
    user_id, plan_id, status, started_at, expires_at, auto_renew,
    amount_paid, currency, external_ref
)
SELECT u.id, p.id, 'ACTIVE', NOW(), NOW() + INTERVAL '30 day', FALSE,
       0, 'VND', 'SUB-SEED-' || u.id
FROM app_user u
CROSS JOIN plan p
WHERE u.email IN (
    'brand.vinamilk@seed.local',
    'brand.shopee@seed.local',
    'brand.tch@seed.local',
    'brand.bitis@seed.local'
)
  AND p.code = 'FREE';
