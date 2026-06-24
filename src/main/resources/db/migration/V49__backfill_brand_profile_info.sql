-- =====================================================================
-- V49 — Backfill logo_url / website / bio / country cho các brand seed.
--
-- Mục tiêu:
--   Bổ sung thông tin còn thiếu (logo, website, bio, country) cho 6 brand
--   đã được seed ở V15 và V20: Vinamilk, Shopee Vietnam, The Coffee House,
--   Biti's, Highlands Coffee, Unilever Vietnam. Trước đây các brand này
--   chỉ có company_name / industry / contact_name; các cột logo_url,
--   website, bio, country đều NULL → trang chi tiết brand và card hiển
--   thị thiếu nội dung.
--
-- Phụ thuộc:
--   - V3  (brand_profile)
--   - V15 (brand seed: Vinamilk, Shopee Vietnam, The Coffee House, Biti's)
--   - V20 (brand seed: Highlands Coffee, Unilever Vietnam)
--   - V30 (cột bio + country)
--
-- Quy ước:
--   - Tra app_user qua email seed (tránh lệch khi company_name khác môi
--     trường), tra brand_profile qua user_id.
--   - country đặt 'Việt Nam' theo yêu cầu seed (cột VARCHAR(100) đủ chứa).
--   - Idempotent: chạy lại sẽ overwrite cùng giá trị canonical, không
--     gây side-effect khác ngoài updated_at.
-- =====================================================================

WITH brand_data AS (
    SELECT u.id        AS user_id,
           m.logo_url,
           m.website,
           m.bio
    FROM (VALUES
        (
            'brand.vinamilk@seed.local',
            'https://ibrand.vn/wp-content/uploads/2024/08/vinamilk-logo_brandlogos.net_quayf.png',
            'https://www.vinamilk.com.vn',
            'Vinamilk là thương hiệu sữa hàng đầu Việt Nam với hơn 45 năm phát triển, sở hữu danh mục sản phẩm sữa tươi, sữa bột, sữa chua và dinh dưỡng đa dạng. Tìm KOL phù hợp với các chiến dịch dinh dưỡng gia đình, mẹ và bé, lifestyle khoẻ mạnh.'
        ),
        (
            'brand.shopee@seed.local',
            'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRO6mQ4kh8Ms7cBHu9ViJWVSBGpAinimxpxZMkVW6FnlFCvcS7-pWAU-ts2&s=10',
            'https://shopee.vn',
            'Shopee Vietnam là nền tảng thương mại điện tử dẫn đầu khu vực Đông Nam Á, kết nối hàng triệu người mua và người bán mỗi ngày. Hợp tác KOL cho các campaign mega sale, livestream bán hàng, review sản phẩm và xu hướng tiêu dùng.'
        ),
        (
            'brand.tch@seed.local',
            'https://cdn-new.topcv.vn/unsafe/https://static.topcv.vn/company_logos/4sMGPpxqw9w5t6ZFWiIyk9cmSIT07e1r_1680678175____3d79ba72f9185aa5b558d462baa8fd31.jpg',
            'https://www.thecoffeehouse.com',
            'The Coffee House là chuỗi cà phê thuần Việt với không gian trẻ trung, hiện đại và thực đơn signature đậm chất bản địa. Tìm kiếm KOL trong lĩnh vực F&B, lifestyle, văn hoá cà phê và sáng tạo nội dung thường ngày.'
        ),
        (
            'brand.bitis@seed.local',
            'https://gozic.vn/uploads/stores/184/2025/11/logo-bitis--hanh-trinh-nhan-dien-thuong-hieu-qua-thoi-gian.jpg',
            'https://bitis.com.vn',
            'Biti''s là thương hiệu giày dép Việt Nam với hơn 40 năm lịch sử, nổi tiếng với dòng Biti''s Hunter và các bộ sưu tập tôn vinh văn hoá Việt. Hợp tác KOL trong lĩnh vực fashion, streetwear, thể thao và các chiến dịch tự hào dân tộc.'
        ),
        (
            'brand.highlands@seed.local',
            'https://i.pinimg.com/736x/49/33/96/49339665366f51689443db2b6832f2cb.jpg',
            'https://www.highlandscoffee.com.vn',
            'Highlands Coffee là chuỗi cà phê Việt với hơn 700 cửa hàng trên toàn quốc, định vị "Tự hào sinh ra từ đất Việt". Tìm KOL phù hợp cho các campaign sản phẩm mới, văn phòng, gặp gỡ bạn bè và các hoạt động cộng đồng.'
        ),
        (
            'brand.unilever@seed.local',
            'https://goldidea.vn/upload/logo-unilever.jpg',
            'https://www.unilever.com.vn',
            'Unilever Vietnam là tập đoàn FMCG đa quốc gia, sở hữu các nhãn hàng quen thuộc như OMO, Sunsilk, Dove, Lifebuoy, Knorr. Hợp tác KOL trong nhiều mảng: chăm sóc cá nhân, gia đình, làm đẹp, ẩm thực và các chiến dịch sustainability.'
        )
    ) AS m(email, logo_url, website, bio)
    JOIN app_user u ON u.email = m.email
)
UPDATE brand_profile bp
SET logo_url   = bd.logo_url,
    website    = bd.website,
    bio        = bd.bio,
    country    = 'Việt Nam',
    updated_at = NOW()
FROM brand_data bd
WHERE bp.user_id = bd.user_id;
