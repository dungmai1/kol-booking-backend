-- V9 đã thử fix hash bcrypt cho "password123" nhưng chuỗi hash đó không verify được:
-- bcrypt.checkpw(b'password123', V9_hash) == False (kiểm tra bằng python bcrypt).
-- Hậu quả: 8 seed KOL không đăng nhập được, mọi flow phụ thuộc seed (booking, payment,
-- notification, review) đều fail trong QA. Replace bằng hash đã verify đúng.
UPDATE app_user
SET password_hash = '$2a$10$ZJo6Pxi.C7ichgif9MD6DOZCjfn3AxLh2q18qP39T5qHtxF3u9d7C'
WHERE email LIKE '%@seed.local';
