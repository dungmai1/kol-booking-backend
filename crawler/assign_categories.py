"""Assign each imported KOL to 1-2 of the 8 canonical categories,
balancing the resulting distribution.

Pipeline
--------
1. Read ``data/kols.json``.
2. Score each KOL against the 8 categories using their kols-koc.com tags
   (``personal_categories`` + ``service_categories``).
3. Pick a primary category for every KOL (the highest-scoring niche;
   ``lifestyle`` is the fallback).
4. Try to add a secondary category — but ONLY if the secondary category is
   currently underweight and the KOL has a real signal for it. This both
   enriches multi-niche KOLs and rebalances the distribution.
5. After step 4, redistribute primary assignments away from the heaviest
   category into the lightest ones, but only for KOLs whose primary
   assignment came from the generic fallback (no specific niche tag).
6. Write ``output/assign_categories.sql`` — DELETE then INSERT, scoped to
   ``%@kols-koc.imported`` so seed data stays untouched.

The 8 categories are fixed by V11.2:
  beauty, fashion, food, lifestyle, travel, fitness, tech, entertainment

Run ``python assign_categories.py --help`` for flags.
"""

from __future__ import annotations

import argparse
import json
import sys
from collections import Counter, defaultdict
from pathlib import Path

if sys.stdout.encoding and sys.stdout.encoding.lower() != "utf-8":
    try:
        sys.stdout.reconfigure(encoding="utf-8")
    except Exception:
        pass

ROOT = Path(__file__).resolve().parent
DEFAULT_INPUT = ROOT / "data" / "kols.json"
DEFAULT_OUTPUT = ROOT / "output" / "assign_categories.sql"

CATEGORIES = ["beauty", "fashion", "food", "lifestyle", "travel", "fitness", "tech", "entertainment"]

# Score map: lowercased Vietnamese tag → {category: weight}.
# Weights of 2+ are strong "niche" signals; weights of 1 are weaker and only
# count when no stronger signal exists. Generic role tags (KOL, KOC,
# Influencer, Reviewer, Tiktoker) have NO weight — they don't tell us
# anything about niche.
TAG_SCORES: dict[str, dict[str, int]] = {
    # ---- personal_categories ("Phân loại") ----
    "beauty blogger": {"beauty": 3},
    "beauty review": {"beauty": 3},
    "makeup artist": {"beauty": 3},
    "fashionista": {"fashion": 3},
    "gia đình": {"lifestyle": 2},
    "mẹ và bé": {"lifestyle": 2},
    "life style": {"lifestyle": 1},
    "streamer": {"entertainment": 3},
    "mc": {"entertainment": 2},
    "youtuber": {"entertainment": 1},  # weak — many youtubers are niche
    # ---- service_categories ("Lĩnh vực") ----
    "mỹ phẩm": {"beauty": 3},
    "thời trang": {"fashion": 3},
    "nhà hàng": {"food": 3},
    "thực phẩm": {"food": 3},
    "phong cách sống": {"lifestyle": 1},  # very common, weak signal
    "du lịch": {"travel": 3},
    "khách sạn": {"travel": 3},
    "gym và thể thao": {"fitness": 3},
    "sức khỏe": {"fitness": 2},
    "công nghệ": {"tech": 3},
    "gamer": {"tech": 2, "entertainment": 1},
    "vloggers": {"entertainment": 2},
    "bloggers": {"entertainment": 1},  # blogger could be many things
    "anime": {"entertainment": 2},
    # Long-tail tags that don't fit the 8 well — bias to lifestyle/entertainment.
    "thú cưng": {"lifestyle": 2},
    "xe cộ": {"lifestyle": 1, "tech": 1},
}

# Tags treated as "format" rather than niche — they carry a small weight
# toward entertainment because vlog/tiktok content is typically broad
# entertainment when no other niche signal is present, but a real niche tag
# (Mỹ phẩm, Thời trang, ...) still wins.
FORMAT_TAGS_WEIGHT: dict[str, int] = {
    "tiktoker": 1,  # extremely common; ties get broken by bin-packer load
    "youtuber": 1,
}
for _t, _w in FORMAT_TAGS_WEIGHT.items():
    TAG_SCORES.setdefault(_t, {})["entertainment"] = max(
        _w, TAG_SCORES.get(_t, {}).get("entertainment", 0)
    )

# Pure role tags — no niche signal at all.
GENERIC_TAGS = {"kol", "koc", "influencer", "reviewer"}

# Default category when a KOL has no niche signal at all.
FALLBACK = "lifestyle"


def score_kol(kol: dict) -> dict[str, int]:
    """Return {category: total weight} for one KOL."""
    scores: dict[str, int] = {c: 0 for c in CATEGORIES}
    raw_tags: list[str] = []
    raw_tags.extend(kol.get("personal_categories") or [])
    raw_tags.extend(kol.get("service_categories") or [])
    for t in raw_tags:
        key = t.strip().lower()
        if key in GENERIC_TAGS or key not in TAG_SCORES:
            continue
        for cat, w in TAG_SCORES[key].items():
            scores[cat] += w
    return scores


def assign_primary(kols: list[dict], cap: int) -> dict[str, tuple[str, bool]]:
    """Pick a primary category for each KOL via greedy bin-packing.

    For each KOL, we have a score per category (higher = better fit). We
    assign each KOL to the best-fitting category that has not yet hit
    ``cap``. KOLs with the FEWEST viable candidates are assigned first so
    they don't get blocked out — KOLs with many candidates have flexibility.

    Returns ``{slug: (category, is_strong)}``. ``is_strong`` is True when
    the picked category had a score > 0; False when we fell back to
    ``FALLBACK`` because the KOL has no scorable tags.
    """
    # Pre-compute scores
    cands: dict[str, list[tuple[str, int]]] = {}  # slug -> [(cat, score), ...] desc by score
    for k in kols:
        scores = score_kol(k)
        ranked = [(c, scores[c]) for c in CATEGORIES if scores[c] > 0]
        ranked.sort(key=lambda t: -t[1])
        cands[k["slug"]] = ranked

    # Order: fewest candidates first; tie-break by slug for determinism.
    order = sorted(cands, key=lambda s: (len(cands[s]) or 99, s))

    counts = Counter()
    out: dict[str, tuple[str, bool]] = {}
    for slug in order:
        ranked = cands[slug]
        if not ranked:
            # No niche signal — assign to whichever category currently has
            # the most room (favors balance for "Tiktoker only" KOLs).
            target = min(CATEGORIES, key=lambda c: counts[c])
            counts[target] += 1
            out[slug] = (target, False)
            continue
        # Pick the highest-scoring category still under cap.
        chosen = None
        for cat, _ in ranked:
            if counts[cat] < cap:
                chosen = cat
                break
        if chosen is None:
            # All candidates full — overflow into the smallest of the
            # candidate set anyway (preserves niche fit over balance).
            chosen = min((c for c, _ in ranked), key=lambda c: counts[c])
        counts[chosen] += 1
        out[slug] = (chosen, True)
    return out


def add_secondary(
    kols: list[dict],
    primary: dict[str, tuple[str, bool]],
    cap: int,
    max_extra: int,
) -> dict[str, list[str]]:
    """Add up to ``max_extra`` secondary categories per KOL.

    A secondary category is added when (a) the KOL scored against it,
    (b) it isn't already in their assignments, and (c) the destination
    category is below ``cap``. This both reflects multi-niche KOLs
    (most beauty KOLs are also lifestyle) and balances the totals.
    """
    assignments: dict[str, list[str]] = {slug: [primary[slug][0]] for slug in primary}
    counts = Counter(p[0] for p in primary.values())

    # Iterate in a stable order so the output is reproducible.
    for k in sorted(kols, key=lambda x: x["slug"]):
        slug = k["slug"]
        scores = score_kol(k)
        # Rank candidates: highest score first. Ties broken by current count
        # ascending so the lighter category wins, which spreads load.
        ranked = sorted(
            (c for c in CATEGORIES if c not in assignments[slug] and scores[c] > 0),
            key=lambda c: (-scores[c], counts[c]),
        )
        for cand in ranked:
            if len(assignments[slug]) - 1 >= max_extra:
                break
            if counts[cand] < cap:
                assignments[slug].append(cand)
                counts[cand] += 1
    return assignments


def rebalance(
    kols: list[dict],
    assignments: dict[str, list[str]],
    primary_strong: dict[str, bool],
    target: int,
    tolerance: float,
) -> None:
    """Move weak primaries from overweight categories to underweight ones.

    Only touches KOLs whose primary was the FALLBACK (no real signal).
    """
    counts = Counter()
    for cats in assignments.values():
        counts[cats[0]] += 1  # only primary counts toward balance

    high_cap = int(target * (1 + tolerance))
    low_cap = int(target * (1 - tolerance))

    # Round-robin destinations: pick categories sorted by current count asc.
    # Refresh after each move so the lightest one keeps getting picked first.
    by_slug = {k["slug"]: k for k in kols}
    weak_in_overflow = [
        slug
        for slug, cats in assignments.items()
        if not primary_strong[slug] and counts[cats[0]] > high_cap
    ]
    # Stable order
    weak_in_overflow.sort()

    for slug in weak_in_overflow:
        # Re-check after each move
        primary_cat = assignments[slug][0]
        if counts[primary_cat] <= high_cap:
            continue
        # Pick the lightest non-fallback category with room
        candidates = [c for c in CATEGORIES if c != primary_cat and counts[c] < low_cap]
        if not candidates:
            candidates = [c for c in CATEGORIES if c != primary_cat and counts[c] < target]
        if not candidates:
            continue
        new_cat = min(candidates, key=lambda c: counts[c])
        # Move primary
        counts[primary_cat] -= 1
        counts[new_cat] += 1
        # Replace in assignments — keep secondary if any
        rest = assignments[slug][1:]
        # Don't duplicate the new primary
        rest = [c for c in rest if c != new_cat]
        assignments[slug] = [new_cat] + rest


def build_sql(assignments: dict[str, list[str]]) -> str:
    rows: list[str] = []
    for slug in sorted(assignments):
        for cat in assignments[slug]:
            rows.append(f"        ('{slug}', '{cat}')")
    if not rows:
        rows = ["        (NULL::VARCHAR, NULL::VARCHAR)"]

    body = ",\n".join(rows)

    return f"""\
-- ======================================================================
-- Assign 8-category junctions for KOLs imported by the crawler.
--
-- Scoped to app_user.email LIKE '%@kols-koc.imported' so the V8 seeded
-- KOLs (whose category links were set in V8__seed_data.sql) are NOT
-- touched.
--
-- DELETE-then-INSERT pattern means re-running this file resets and
-- repopulates the junctions for those KOLs only.
-- ======================================================================

-- Wipe the junction rows for imported KOLs only.
DELETE FROM kol_category
WHERE kol_profile_id IN (
    SELECT kp.id FROM kol_profile kp
    JOIN app_user au ON au.id = kp.user_id
    WHERE au.email LIKE '%@kols-koc.imported'
);

-- Insert the freshly computed assignments.
WITH input (kol_slug, cat_slug) AS (
    VALUES
{body}
)
INSERT INTO kol_category (kol_profile_id, category_id)
SELECT kp.id, c.id
FROM input i
JOIN kol_profile kp ON kp.slug = i.kol_slug
JOIN app_user au ON au.id = kp.user_id
   AND au.email LIKE '%@kols-koc.imported'
JOIN category c ON c.slug = i.cat_slug
ON CONFLICT DO NOTHING;
"""


def main() -> int:
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("--input", type=Path, default=DEFAULT_INPUT)
    p.add_argument("--output", type=Path, default=DEFAULT_OUTPUT)
    p.add_argument(
        "--target",
        type=int,
        default=None,
        help="Target rows per category for primary balancing. Defaults to ceil(N/8).",
    )
    p.add_argument(
        "--tolerance",
        type=float,
        default=0.30,
        help="Allowed deviation from target before rebalancing kicks in (0.30 = ±30%%).",
    )
    p.add_argument(
        "--max-extra-cats",
        type=int,
        default=2,
        help="Max secondary categories per KOL (in addition to the primary).",
    )
    p.add_argument(
        "--secondary-cap-multiplier",
        type=float,
        default=2.5,
        help="Cap on TOTAL rows per category after secondary pass, as multiple of target.",
    )
    args = p.parse_args()

    if not args.input.exists():
        print(f"Input not found: {args.input}", file=sys.stderr)
        return 1

    data = json.loads(args.input.read_text(encoding="utf-8"))
    kols = data["kols"]

    target = args.target or -(-len(kols) // 8)  # ceil
    primary_cap = int(target * (1 + args.tolerance))
    underweight_threshold = int(target * (1 - args.tolerance))

    primary_pairs = assign_primary(kols, cap=primary_cap)
    primary_strong = {slug: pair[1] for slug, pair in primary_pairs.items()}
    primary = {slug: pair[0] for slug, pair in primary_pairs.items()}

    print(f"Target/category: ~{target} (cap={primary_cap})")
    print("== After PRIMARY pass (bin-packed) ==")
    primary_counts = Counter(primary.values())
    for c in CATEGORIES:
        print(f"  {c:14s} {primary_counts[c]:4d}")
    print(f"  weak (fallback, no niche signal): {sum(1 for v in primary_strong.values() if not v)}")

    secondary_cap = int(target * args.secondary_cap_multiplier)
    assignments = add_secondary(
        kols,
        {s: (primary[s], primary_strong[s]) for s in primary},
        cap=secondary_cap,
        max_extra=args.max_extra_cats,
    )

    print("\n== After SECONDARY pass (all assignments) ==")
    all_counts = Counter()
    for cats in assignments.values():
        for c in cats:
            all_counts[c] += 1
    for c in CATEGORIES:
        print(f"  {c:14s} {all_counts[c]:4d}")
    total_assignments = sum(len(v) for v in assignments.values())
    multi = sum(1 for v in assignments.values() if len(v) > 1)
    print(f"  total junction rows: {total_assignments}  |  KOL with 2 cats: {multi}/{len(kols)}")

    sql = build_sql(assignments)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(sql, encoding="utf-8")
    print(f"\nWrote {args.output} ({sql.count(chr(10)) + 1} lines)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
