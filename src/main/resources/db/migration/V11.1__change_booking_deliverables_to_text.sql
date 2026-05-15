-- Booking.deliverables được dùng như free-form text (vd "1 TikTok video, 1 IG post"),
-- không phải JSON document. Cột JSONB cũ làm Postgres reject mọi input không phải JSON
-- (POST /bookings → 500 với "Token TikTok is invalid"). Đổi sang TEXT cho đúng nghĩa.
ALTER TABLE booking
    ALTER COLUMN deliverables TYPE text USING deliverables::text;
