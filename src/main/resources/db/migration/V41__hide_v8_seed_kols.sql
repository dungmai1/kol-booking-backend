-- =====================================================================
-- V41 — Ẩn 8 KOL seed V8 khỏi marketplace
--
-- Đặt status = REJECTED để không còn xuất hiện ở:
--   GET /kols/search, /kols/featured, /kols/{slug} (public)
-- Dữ liệu profile / booking / review vẫn giữ nguyên trong DB.
--
-- Idempotent: chỉ update các slug đang APPROVED.
-- =====================================================================

UPDATE kol_profile
SET status        = 'REJECTED',
    reject_reason = 'Ẩn khỏi marketplace (seed V8 cleanup)',
    updated_at    = NOW()
WHERE slug IN (
    'nguyen-thi-thanh-nha',
    'lam-bao-ngoc',
    'tien-tien',
    'hoang-soi',
    'phuong-di-dau',
    'tebefood',
    'tuan-di-dau',
    'hoang-duyen'
)
AND status = 'APPROVED';
