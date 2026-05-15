-- Trim category seed to the 8 canonical KOL niches.
-- Removes: gaming, education, parenting, finance (and any non-canonical
-- categories that may have been added since V4).
--
-- kol_category.category_id has ON DELETE CASCADE (see V4), so any KOL
-- associations to the dropped categories disappear automatically.
-- category.parent_id has ON DELETE SET NULL — no risk of orphaning.
--
-- DELETE is naturally idempotent; re-running it is a no-op.

DELETE FROM category
WHERE slug NOT IN (
    'beauty',
    'fashion',
    'food',
    'lifestyle',
    'travel',
    'fitness',
    'tech',
    'entertainment'
);
