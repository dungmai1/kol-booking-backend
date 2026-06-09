-- ======================================================================
-- V25 — Idempotent backfill of denormalized kol_profile aggregates.
--
-- Ensures min_price and max_follower_count are correct for all profiles,
-- including any imported after V19. Safe to run multiple times.
-- ======================================================================

UPDATE kol_profile k
SET
    max_follower_count = COALESCE(
        (SELECT MAX(c.follower_count)
         FROM kol_social_channel c
         WHERE c.kol_profile_id = k.id),
        0
    ),
    min_price = (
        SELECT MIN(p.price)
        FROM kol_pricing_package p
        WHERE p.kol_profile_id = k.id
    );
