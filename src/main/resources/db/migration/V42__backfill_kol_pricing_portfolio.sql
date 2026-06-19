-- =====================================================================
-- V42 — Backfill giá (pricing packages), portfolio và min_price cho KOL
--
-- Phạm vi: mọi kol_profile status = APPROVED chưa có pricing package.
-- Giá VIDEO/STORY tính theo max follower_count trên kênh chính (follower cao nhất).
-- Portfolio: tối đa 2 kênh MXH cho KOL chưa có portfolio item.
-- Cuối cùng recompute min_price + max_follower_count trên kol_profile.
--
-- Idempotent: NOT EXISTS guards — an toàn khi chạy lại.
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1) Pricing — VIDEO trên kênh chính
-- ---------------------------------------------------------------------
WITH primary_channel AS (
    SELECT DISTINCT ON (c.kol_profile_id)
        c.kol_profile_id,
        c.platform,
        c.username,
        c.follower_count
    FROM kol_social_channel c
    JOIN kol_profile kp ON kp.id = c.kol_profile_id
    WHERE kp.status = 'APPROVED'
    ORDER BY c.kol_profile_id,
             c.follower_count DESC,
             CASE c.platform
                 WHEN 'TIKTOK'    THEN 1
                 WHEN 'INSTAGRAM' THEN 2
                 WHEN 'YOUTUBE'   THEN 3
                 ELSE 4
             END
),
tiered AS (
    SELECT
        pc.kol_profile_id,
        pc.platform,
        pc.username,
        CASE
            WHEN pc.follower_count >= 1000000 THEN 40000000.00
            WHEN pc.follower_count >=  500000 THEN 25000000.00
            WHEN pc.follower_count >=  100000 THEN 15000000.00
            WHEN pc.follower_count >=   50000 THEN  8000000.00
            WHEN pc.follower_count >=   10000 THEN  5000000.00
            ELSE                                  2000000.00
        END AS video_price
    FROM primary_channel pc
)
INSERT INTO kol_pricing_package (kol_profile_id, type, platform, price, description)
SELECT
    t.kol_profile_id,
    'VIDEO',
    t.platform,
    t.video_price,
    '1 video ' || t.platform
        || COALESCE(' trên kênh @' || NULLIF(t.username, ''), '')
        || ' — giá tham khảo theo quy mô follower'
FROM tiered t
WHERE NOT EXISTS (
    SELECT 1 FROM kol_pricing_package p WHERE p.kol_profile_id = t.kol_profile_id
);

-- ---------------------------------------------------------------------
-- 2) Pricing — STORY (TikTok / Instagram, ~40% giá VIDEO)
-- ---------------------------------------------------------------------
WITH primary_channel AS (
    SELECT DISTINCT ON (c.kol_profile_id)
        c.kol_profile_id,
        c.platform,
        c.username,
        c.follower_count
    FROM kol_social_channel c
    JOIN kol_profile kp ON kp.id = c.kol_profile_id
    WHERE kp.status = 'APPROVED'
    ORDER BY c.kol_profile_id,
             c.follower_count DESC,
             CASE c.platform
                 WHEN 'TIKTOK'    THEN 1
                 WHEN 'INSTAGRAM' THEN 2
                 WHEN 'YOUTUBE'   THEN 3
                 ELSE 4
             END
),
tiered AS (
    SELECT
        pc.kol_profile_id,
        pc.platform,
        pc.username,
        CASE
            WHEN pc.follower_count >= 1000000 THEN 12000000.00
            WHEN pc.follower_count >=  500000 THEN  8000000.00
            WHEN pc.follower_count >=  100000 THEN  5000000.00
            WHEN pc.follower_count >=   50000 THEN  3000000.00
            WHEN pc.follower_count >=   10000 THEN  2000000.00
            ELSE                                  800000.00
        END AS story_price
    FROM primary_channel pc
    WHERE pc.platform IN ('TIKTOK', 'INSTAGRAM')
)
INSERT INTO kol_pricing_package (kol_profile_id, type, platform, price, description)
SELECT
    t.kol_profile_id,
    'STORY',
    t.platform,
    t.story_price,
    '1 story / reel ngắn trên ' || t.platform
        || COALESCE(' @' || NULLIF(t.username, ''), '')
FROM tiered t
WHERE NOT EXISTS (
    SELECT 1 FROM kol_pricing_package p
    WHERE p.kol_profile_id = t.kol_profile_id
      AND p.type = 'STORY'
      AND p.platform = t.platform
);

-- ---------------------------------------------------------------------
-- 3) Portfolio — tối đa 2 kênh MXH cho KOL chưa có portfolio
-- ---------------------------------------------------------------------
WITH ranked_channels AS (
    SELECT
        c.kol_profile_id,
        c.platform,
        c.url,
        kp.display_name,
        ROW_NUMBER() OVER (
            PARTITION BY c.kol_profile_id
            ORDER BY c.follower_count DESC,
                     CASE c.platform
                         WHEN 'TIKTOK'    THEN 1
                         WHEN 'INSTAGRAM' THEN 2
                         WHEN 'YOUTUBE'   THEN 3
                         ELSE 4
                     END
        ) AS rn
    FROM kol_social_channel c
    JOIN kol_profile kp ON kp.id = c.kol_profile_id
    WHERE kp.status = 'APPROVED'
      AND c.url IS NOT NULL
      AND c.url <> ''
      AND NOT EXISTS (
          SELECT 1 FROM kol_portfolio_item pi WHERE pi.kol_profile_id = kp.id
      )
)
INSERT INTO kol_portfolio_item (kol_profile_id, title, media_url, media_type, campaign_name)
SELECT
    rc.kol_profile_id,
    LEFT('Kênh ' || rc.platform || ' — ' || rc.display_name, 200),
    rc.url,
    CASE WHEN rc.platform IN ('TIKTOK', 'YOUTUBE') THEN 'VIDEO' ELSE 'IMAGE' END,
    'Portfolio seed V42'
FROM ranked_channels rc
WHERE rc.rn <= 2
  AND NOT EXISTS (
      SELECT 1 FROM kol_portfolio_item pi
      WHERE pi.kol_profile_id = rc.kol_profile_id
        AND pi.media_url = rc.url
  );

-- ---------------------------------------------------------------------
-- 4) Recompute min_price + max_follower_count
-- ---------------------------------------------------------------------
UPDATE kol_profile k
SET
    min_price = (
        SELECT MIN(p.price)
        FROM kol_pricing_package p
        WHERE p.kol_profile_id = k.id
    ),
    max_follower_count = COALESCE(
        (SELECT MAX(c.follower_count)
         FROM kol_social_channel c
         WHERE c.kol_profile_id = k.id),
        0
    ),
    updated_at = NOW()
WHERE k.status = 'APPROVED'
  AND EXISTS (
      SELECT 1 FROM kol_pricing_package p WHERE p.kol_profile_id = k.id
  );
