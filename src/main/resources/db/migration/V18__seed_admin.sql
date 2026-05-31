-- =====================================================================
-- V18 — Seed tài khoản ADMIN cho phát triển & kiểm thử
--
-- Mục tiêu:
--   Tạo 1 tài khoản ADMIN để kiểm thử toàn bộ module Admin:
--     - GET  /admin/kols?status=PENDING_REVIEW
--     - POST /admin/kols/{id}/approve|reject
--     - GET  /admin/brands
--     - GET  /admin/bookings
--     - GET  /admin/users
--     - GET  /admin/stats/overview
--     - GET  /admin/audit-logs
--     - GET  /withdraws/admin
--
-- Credentials:
--   Email   : admin@dev.local
--   Password: password123
--   Hash    : BCrypt $2a$10$ (cùng hash dùng cho toàn bộ tài khoản @seed.local)
--
-- Phụ thuộc:
--   - V2 (app_user — bảng, enum role, enum status)
-- =====================================================================

INSERT INTO app_user (email, password_hash, role, status, email_verified)
VALUES (
    'admin@dev.local',
    '$2a$10$ZJo6Pxi.C7ichgif9MD6DOZCjfn3AxLh2q18qP39T5qHtxF3u9d7C',
    'ADMIN',
    'ACTIVE',
    TRUE
);
