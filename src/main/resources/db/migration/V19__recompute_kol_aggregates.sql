-- ======================================================================
-- V19 — Recompute denormalized aggregates for all KOL profiles.
--
-- Context:
--   V12 added max_follower_count / min_price columns and ran a one-time
--   backfill at migration time. V14 then bulk-imported 249 KOLs together
--   with their social channels and pricing packages. Because V12's backfill
--   had already run by then, those V14 profiles were left with
--   max_follower_count = 0 (DEFAULT) even though their channels exist.
--
-- Fix:
--   Re-run the same correlated-subquery UPDATE for every KOL profile.
--   KOLs without channels keep 0; KOLs with channels get the correct max.
--   This is idempotent — safe to run multiple times.
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
