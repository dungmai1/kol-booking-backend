#!/usr/bin/env python3
"""
Enrich KOL profiles (reviews, pricingPackages, portfolio) using m-thang3001 as golden sample.

Usage:
  python scripts/enrich_kol_from_golden_sample.py [--dry-run] [--apply]
"""

from __future__ import annotations

import argparse
import json
import math
import os
import re
import sys
import textwrap
import urllib.parse
import urllib.request
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any

try:
    import psycopg2
    from psycopg2.extras import RealDictCursor
except ImportError:
    print("Missing psycopg2. Install: pip install psycopg2-binary", file=sys.stderr)
    sys.exit(1)

ROOT = Path(__file__).resolve().parents[1]
ENV_FILE = ROOT / ".env"
API_BASE = os.environ.get("APP_URL", "http://localhost:8081")
GOLDEN_SLUG = "m-thang3001"
TARGET_REVIEW_COUNT = 5
TARGET_PORTFOLIO_COUNT = 5

FEATURED_SLUGS = [
    "linh-baeli",
    "duc-va-ly",
    "nha-hieu-review",
    "nhan-phuong-chi-xu",
    "hat-tieu-foodie",
    "eatwhning",
    "m-thang3001",
    "bu-ne",
    "ngo-thi-kim-yen",
    "quynh-tran-ne",
]

FOLLOWERS_SLUGS = [
    "quynh-thi",
    "babykopo-home",
    "quyenleodaily",
    "quan-khong-go",
    "thung-long-family",
    "linh-barbie",
    "hach-lien-tu-nguyet",
    "hy-khi-duong-duong",
    "di-di",
    "duong-jin",
]

BRANDS = [
    ("brand.vinamilk@seed.local", "Vinamilk", "F&B / Sữa"),
    ("brand.shopee@seed.local", "Shopee Vietnam", "E-commerce"),
    ("brand.tch@seed.local", "The Coffee House", "F&B / Cà phê"),
    ("brand.bitis@seed.local", "Biti's", "Thời trang / Giày dép"),
    ("brand.highlands@seed.local", "Highlands Coffee", "F&B / Cà phê"),
    ("brand.unilever@seed.local", "Unilever Vietnam", "FMCG / Chăm sóc cá nhân"),
]

REVIEW_THEMES = [
    ("content", "Nội dung {adj} phù hợp lĩnh vực {niche}, storytelling tự nhiên."),
    ("engagement", "Tương tác bình luận và share vượt KPI, cộng đồng phản hồi tích cực."),
    ("campaign", "Chiến dịch đạt reach mục tiêu, CTA rõ ràng và conversion ổn định."),
    ("attitude", "Thái độ làm việc chuyên nghiệp, lắng nghe brief và chủ động đề xuất ý tưởng."),
    ("response", "Phản hồi nhanh qua chat, giao deliverable đúng timeline cam kết."),
]

NICHE_KEYWORDS = {
    "beauty": "làm đẹp & mỹ phẩm",
    "fashion": "thời trang",
    "food": "ẩm thực & F&B",
    "family": "gia đình & lifestyle",
    "review": "review sản phẩm",
    "entertainment": "giải trí & review",
    "fitness": "fitness",
    "default": "nội dung sáng tạo",
}


@dataclass
class KolSnapshot:
    slug: str
    display_name: str
    user_id: int
    review_count: int
    avg_rating: float
    packages: list[dict]
    portfolio: list[dict]
    channels: list[dict]
    category_ids: list[int]
    bio: str = ""


@dataclass
class EnrichmentPlan:
    slug: str
    reviews_to_add: int = 0
    packages_to_add: list[dict] = field(default_factory=list)
    portfolio_to_add: list[dict] = field(default_factory=list)
    skipped: list[str] = field(default_factory=list)


def load_env(path: Path) -> dict[str, str]:
    env: dict[str, str] = {}
    if not path.exists():
        return env
    for line in path.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        env[key.strip()] = value.strip().strip('"').strip("'")
    return env


def parse_jdbc_url(jdbc_url: str) -> dict[str, Any]:
    # jdbc:postgresql://host:5432/db?params
    parsed = urllib.parse.urlparse(jdbc_url.replace("jdbc:", "", 1))
    query = urllib.parse.parse_qs(parsed.query)
    return {
        "host": parsed.hostname,
        "port": parsed.port or 5432,
        "dbname": parsed.path.lstrip("/"),
        "user": None,
        "password": None,
        "sslmode": query.get("sslmode", ["require"])[0],
    }


def api_get(path: str) -> dict:
    url = f"{API_BASE.rstrip('/')}{path}"
    req = urllib.request.Request(url, headers={"Accept": "application/json"})
    with urllib.request.urlopen(req, timeout=30) as resp:
        return json.loads(resp.read().decode("utf-8"))


def fetch_kol(slug: str) -> KolSnapshot:
    payload = api_get(f"/api/v1/kols/{slug}")["data"]
    return KolSnapshot(
        slug=payload["slug"],
        display_name=payload["displayName"],
        user_id=payload["userId"],
        review_count=payload["reviewCount"],
        avg_rating=float(payload["avgRating"]),
        packages=payload.get("pricingPackages") or [],
        portfolio=payload.get("portfolio") or [],
        channels=payload.get("channels") or [],
        category_ids=payload.get("categoryIds") or [],
        bio=payload.get("bio") or "",
    )


def dedupe_target_slugs() -> list[str]:
    seen: set[str] = set()
    ordered: list[str] = []
    for slug in FEATURED_SLUGS + FOLLOWERS_SLUGS:
        if slug == GOLDEN_SLUG or slug in seen:
            continue
        seen.add(slug)
        ordered.append(slug)
    return ordered


def infer_niche(kol: KolSnapshot) -> str:
    bio = (kol.bio or "").lower()
    cats = set(kol.category_ids)
    if 1 in cats or "mỹ phẩm" in bio or "makeup" in bio or "skincare" in bio:
        return NICHE_KEYWORDS["beauty"]
    if 2 in cats or "thời trang" in bio or "fashion" in bio:
        return NICHE_KEYWORDS["fashion"]
    if 4 in cats or "ẩm thực" in bio or "food" in bio or "quán" in bio:
        return NICHE_KEYWORDS["food"]
    if 12 in cats or "gia đình" in bio or "couple" in bio or "vlog" in bio:
        return NICHE_KEYWORDS["family"]
    if "review" in bio:
        return NICHE_KEYWORDS["review"]
    if 5 in cats:
        return NICHE_KEYWORDS["family"]
    return NICHE_KEYWORDS["default"]


def primary_platform(kol: KolSnapshot) -> tuple[str, str, int]:
    if not kol.channels:
        return "TIKTOK", "tiktok", 10000
    ch = max(kol.channels, key=lambda c: c.get("followerCount") or 0)
    platform = ch["platform"]
    username = ch.get("username") or kol.slug
    followers = int(ch.get("followerCount") or 0)
    # Cap obviously bad follower counts (e.g. quynh-thi Instagram 897M)
    if followers > 100_000_000:
        tiktok = next((c for c in kol.channels if c["platform"] == "TIKTOK"), None)
        if tiktok:
            platform = "TIKTOK"
            username = tiktok.get("username") or username
            followers = int(tiktok.get("followerCount") or followers)
    return platform, username, followers


def price_tier(followers: int) -> int:
    if followers < 50_000:
        return 1
    if followers < 200_000:
        return 2
    if followers < 1_000_000:
        return 3
    if followers < 10_000_000:
        return 4
    return 5


# VIDEO/POST: 1–5 triệu theo quy mô KOL; STORY/COMBO scale tương ứng
PACKAGE_PRICES: dict[int, dict[str, int]] = {
    1: {"video": 1_500_000, "post": 1_200_000, "story": 800_000, "combo": 3_500_000},
    2: {"video": 2_000_000, "post": 1_800_000, "story": 1_000_000, "combo": 4_500_000},
    3: {"video": 3_000_000, "post": 2_500_000, "story": 1_500_000, "combo": 6_000_000},
    4: {"video": 4_000_000, "post": 3_500_000, "story": 2_000_000, "combo": 8_000_000},
    5: {"video": 5_000_000, "post": 4_500_000, "story": 2_500_000, "combo": 10_000_000},
}


def build_packages(kol: KolSnapshot) -> list[dict]:
    platform, username, followers = primary_platform(kol)
    tier = price_tier(followers)
    prices = PACKAGE_PRICES[tier]
    niche = infer_niche(kol)

    packages = [
        {
            "type": "VIDEO",
            "platform": platform,
            "price": prices["video"],
            "description": (
                f"1 video {platform} 30–90 giây về {niche}, quay và đăng trên kênh @{username}. "
                f"Thời gian thực hiện: 5–7 ngày làm việc."
            ),
        },
        {
            "type": "STORY",
            "platform": platform,
            "price": prices["story"],
            "description": (
                f"2–3 story {platform} giới thiệu sản phẩm/dịch vụ trong {niche}. "
                f"Deliverables: story có sticker/link, tag brand. Thời gian: 2–3 ngày."
            ),
        },
        {
            "type": "COMBO",
            "platform": platform,
            "price": prices["combo"],
            "description": (
                f"Combo 1 video + 2 story + 1 bài đăng cross-post về {niche}. "
                f"Báo cáo reach/engagement sau 7 ngày. Thời gian: 10–14 ngày."
            ),
        },
    ]

    other_platforms = sorted(
        {c["platform"] for c in kol.channels if c["platform"] != platform},
        key=lambda p: max(
            (c.get("followerCount") or 0 for c in kol.channels if c["platform"] == p),
            default=0,
        ),
        reverse=True,
    )
    if other_platforms:
        p2 = other_platforms[0]
        u2 = next(c.get("username") or kol.slug for c in kol.channels if c["platform"] == p2)
        packages.append(
            {
                "type": "POST",
                "platform": p2,
                "price": prices["post"],
                "description": (
                    f"1 bài đăng {p2} (@{u2}) review/giới thiệu phù hợp {niche}. "
                    f"Thời gian thực hiện: 4–6 ngày."
                ),
            }
        )
    return packages


def build_portfolio_items(kol: KolSnapshot, needed: int) -> list[dict]:
    platform, username, _ = primary_platform(kol)
    niche = infer_niche(kol)
    existing_titles = {(p.get("title") or "").lower() for p in kol.portfolio}
    templates = [
        (
            f"Campaign {kol.display_name} x thương hiệu F&B",
            f"Series nội dung {niche} kết hợp trải nghiệm thực tế, reach 500K+ views.",
            "Brand collaboration",
        ),
        (
            f"Review sản phẩm trending — {kol.display_name}",
            f"Đánh giá chi tiết sản phẩm trong lĩnh vực {niche}, tỷ lệ save cao.",
            "Product review",
        ),
        (
            f"Viral clip {platform} — {kol.display_name}",
            f"Video ngắn viral về {niche}, engagement rate trên mức trung bình ngành.",
            "Organic viral",
        ),
        (
            f"Mini-series lifestyle — {kol.display_name}",
            f"3 tập nội dung {niche}, tăng follower và nhận diện thương hiệu cá nhân.",
            "Content series",
        ),
        (
            f"Livestream recap — {kol.display_name}",
            f"Tổng hợp highlights livestream {niche}, chuyển đổi sang đơn hàng.",
            "Live commerce",
        ),
    ]
    media_base = next(
        (c.get("url") for c in kol.channels if c.get("url")),
        f"https://www.tiktok.com/@{username}",
    )
    items: list[dict] = []
    idx = 0
    while len(items) < needed and idx < len(templates) * 3:
        title, desc, campaign = templates[idx % len(templates)]
        unique_title = f"{title} #{idx + 1}"
        if unique_title.lower() not in existing_titles:
            items.append(
                {
                    "title": unique_title,
                    "media_url": media_base,
                    "media_type": "VIDEO" if platform in ("TIKTOK", "YOUTUBE") else "IMAGE",
                    "campaign_name": f"{campaign} — {desc[:80]}",
                }
            )
        idx += 1
    return items[:needed]


def rating_for_index(i: int) -> int:
    pattern = [5, 4, 5, 4, 4]
    return pattern[i % len(pattern)]


def build_review_rows(kol: KolSnapshot, count: int, start_idx: int = 0) -> list[dict]:
    niche = infer_niche(kol)
    rows: list[dict] = []
    for i in range(count):
        brand_email, brand_name, brand_industry = BRANDS[(start_idx + i) % len(BRANDS)]
        theme_key, theme_tpl = REVIEW_THEMES[(start_idx + i) % len(REVIEW_THEMES)]
        adj = ["sáng tạo", "chân thực", "chuyên nghiệp", "cuốn hút", "rõ ràng"][i % 5]
        comment = theme_tpl.format(adj=adj, niche=niche)
        if theme_key == "content":
            comment = f"{comment} Phù hợp định vị {brand_name}."
        elif theme_key == "engagement":
            comment = f"{comment} Brand mention tự nhiên, không gượng ép."
        elif theme_key == "campaign":
            comment = f"{comment} KPI chiến dịch {brand_industry} đạt {85 + i * 2}%."
        elif theme_key == "attitude":
            comment = f"{comment} Brief {brand_name} được triển khai đúng tinh thần."
        else:
            comment = f"{comment} Team {brand_name} đánh giá cao tốc độ phản hồi."

        title = f"V34 Enrich {kol.slug} — {brand_name} {theme_key.title()} {start_idx + i + 1}"
        rows.append(
            {
                "brand_email": brand_email,
                "campaign_title": title,
                "campaign_brief": f"Chiến dịch {niche} cùng {brand_name} — nội dung {theme_key}.",
                "deliverables": f"1–2 {primary_platform(kol)[0]} content pieces + story",
                "budget": 8_000_000 + (start_idx + i) * 1_250_000,
                "rating": rating_for_index(start_idx + i),
                "comment": comment,
            }
        )
    return rows


def plan_enrichment(kol: KolSnapshot, golden: KolSnapshot) -> EnrichmentPlan:
    plan = EnrichmentPlan(slug=kol.slug)

    if kol.review_count >= TARGET_REVIEW_COUNT:
        plan.skipped.append(f"reviews (đã có {kol.review_count})")
    else:
        plan.reviews_to_add = TARGET_REVIEW_COUNT - kol.review_count

    if kol.packages:
        plan.skipped.append(f"pricingPackages (đã có {len(kol.packages)})")
    else:
        plan.packages_to_add = build_packages(kol)

    portfolio_count = len(kol.portfolio)
    if portfolio_count >= TARGET_PORTFOLIO_COUNT:
        plan.skipped.append(f"portfolio (đã có {portfolio_count})")
    else:
        plan.portfolio_to_add = build_portfolio_items(kol, TARGET_PORTFOLIO_COUNT - portfolio_count)

    return plan


def connect_db(env: dict[str, str]):
    jdbc = env.get("SPRING_DATASOURCE_URL", "")
    cfg = parse_jdbc_url(jdbc)
    cfg["user"] = env.get("SPRING_DATASOURCE_USERNAME")
    cfg["password"] = env.get("SPRING_DATASOURCE_PASSWORD", "")
    sslmode = cfg.pop("sslmode", "require")
    conn = psycopg2.connect(
        host=cfg["host"],
        port=cfg["port"],
        dbname=cfg["dbname"],
        user=cfg["user"],
        password=cfg["password"],
        sslmode=sslmode,
    )
    conn.autocommit = False
    return conn


def reprice_packages(conn, snapshots: dict[str, KolSnapshot]) -> dict[str, int]:
    """Cập nhật giá các gói đã tồn tại theo bảng giá mới (1–5tr cho video/post)."""
    updated_by_slug: dict[str, int] = {}
    with conn.cursor(cursor_factory=RealDictCursor) as cur:
        for slug, kol in snapshots.items():
            cur.execute("SELECT id FROM kol_profile WHERE slug = %s", (slug,))
            row = cur.fetchone()
            if not row:
                continue
            kol_profile_id = row["id"]
            count = 0
            for pkg in build_packages(kol):
                cur.execute(
                    """
                    UPDATE kol_pricing_package
                    SET price = %s, description = %s
                    WHERE kol_profile_id = %s AND type = %s AND platform = %s
                    """,
                    (
                        pkg["price"],
                        pkg["description"],
                        kol_profile_id,
                        pkg["type"],
                        pkg["platform"],
                    ),
                )
                count += cur.rowcount

            cur.execute(
                """
                UPDATE kol_profile
                SET min_price = (
                        SELECT MIN(p.price) FROM kol_pricing_package p
                        WHERE p.kol_profile_id = %s
                    ),
                    updated_at = NOW()
                WHERE id = %s
                """,
                (kol_profile_id, kol_profile_id),
            )
            updated_by_slug[slug] = count
        conn.commit()
    return updated_by_slug


def apply_enrichment(conn, plans: dict[str, EnrichmentPlan], snapshots: dict[str, KolSnapshot]) -> dict[str, dict]:
    stats: dict[str, dict] = {}
    with conn.cursor(cursor_factory=RealDictCursor) as cur:
        for slug, plan in plans.items():
            kol = snapshots[slug]
            slug_stats = {
                "reviews_added": 0,
                "packages_added": 0,
                "portfolio_added": 0,
                "skipped": plan.skipped,
            }

            cur.execute("SELECT id FROM kol_profile WHERE slug = %s", (slug,))
            row = cur.fetchone()
            if not row:
                continue
            kol_profile_id = row["id"]

            if plan.packages_to_add:
                cur.execute(
                    "SELECT COUNT(*) AS c FROM kol_pricing_package WHERE kol_profile_id = %s",
                    (kol_profile_id,),
                )
                if cur.fetchone()["c"] == 0:
                    for pkg in plan.packages_to_add:
                        cur.execute(
                            """
                            INSERT INTO kol_pricing_package (kol_profile_id, type, platform, price, description)
                            VALUES (%s, %s, %s, %s, %s)
                            """,
                            (
                                kol_profile_id,
                                pkg["type"],
                                pkg["platform"],
                                pkg["price"],
                                pkg["description"],
                            ),
                        )
                        slug_stats["packages_added"] += 1

            for item in plan.portfolio_to_add:
                cur.execute(
                    """
                    INSERT INTO kol_portfolio_item (kol_profile_id, title, media_url, media_type, campaign_name)
                    SELECT %s, %s, %s, %s, %s
                    WHERE NOT EXISTS (
                        SELECT 1 FROM kol_portfolio_item pi
                        WHERE pi.kol_profile_id = %s AND pi.title = %s
                    )
                    """,
                    (
                        kol_profile_id,
                        item["title"],
                        item["media_url"],
                        item["media_type"],
                        item["campaign_name"],
                        kol_profile_id,
                        item["title"],
                    ),
                )
                if cur.rowcount:
                    slug_stats["portfolio_added"] += 1

            review_rows = build_review_rows(kol, plan.reviews_to_add, start_idx=kol.review_count)
            for rr in review_rows:
                cur.execute(
                    """
                    INSERT INTO booking (
                        brand_profile_id, kol_profile_id, campaign_title, campaign_brief,
                        deliverables, budget, start_date, end_date, status
                    )
                    SELECT bp.id, kp.id, %s, %s, %s, %s,
                           DATE '2026-05-01' + (%s || ' days')::INTERVAL,
                           DATE '2026-05-15' + (%s || ' days')::INTERVAL,
                           'COMPLETED'
                    FROM app_user bu
                    JOIN brand_profile bp ON bp.user_id = bu.id
                    JOIN kol_profile kp ON kp.slug = %s
                    WHERE bu.email = %s
                      AND NOT EXISTS (SELECT 1 FROM booking b WHERE b.campaign_title = %s)
                    """,
                    (
                        rr["campaign_title"],
                        rr["campaign_brief"],
                        rr["deliverables"],
                        rr["budget"],
                        slug_stats["reviews_added"],
                        slug_stats["reviews_added"] + 10,
                        slug,
                        rr["brand_email"],
                        rr["campaign_title"],
                    ),
                )
                if cur.rowcount:
                    cur.execute(
                        """
                        INSERT INTO review (booking_id, author_id, target_id, direction, rating, comment)
                        SELECT b.id, bu.id, ku.id, 'BRAND_TO_KOL', %s, %s
                        FROM booking b
                        JOIN brand_profile bp ON bp.id = b.brand_profile_id
                        JOIN app_user bu ON bu.id = bp.user_id
                        JOIN kol_profile kp ON kp.id = b.kol_profile_id
                        JOIN app_user ku ON ku.id = kp.user_id
                        WHERE b.campaign_title = %s
                          AND bu.email = %s
                          AND NOT EXISTS (
                              SELECT 1 FROM review r
                              WHERE r.booking_id = b.id AND r.direction = 'BRAND_TO_KOL'
                          )
                        """,
                        (rr["rating"], rr["comment"], rr["campaign_title"], rr["brand_email"]),
                    )
                    if cur.rowcount:
                        slug_stats["reviews_added"] += 1

            cur.execute(
                """
                UPDATE kol_profile k SET
                    avg_rating = stats.avg_rating,
                    review_count = stats.review_count,
                    min_price = stats.min_price,
                    updated_at = NOW()
                FROM (
                    SELECT
                        kp.id AS kol_profile_id,
                        COALESCE(ROUND(AVG(r.rating)::NUMERIC, 2), 0) AS avg_rating,
                        COUNT(r.id)::INTEGER AS review_count,
                        (SELECT MIN(p.price) FROM kol_pricing_package p WHERE p.kol_profile_id = kp.id) AS min_price
                    FROM kol_profile kp
                    LEFT JOIN app_user ku ON ku.id = kp.user_id
                    LEFT JOIN review r ON r.target_id = ku.id AND r.direction = 'BRAND_TO_KOL'
                    WHERE kp.slug = %s
                    GROUP BY kp.id
                ) stats
                WHERE k.id = stats.kol_profile_id
                """,
                (slug,),
            )
            stats[slug] = slug_stats
        conn.commit()
    return stats


def snapshot_summary(kol: KolSnapshot) -> dict:
    return {
        "slug": kol.slug,
        "displayName": kol.display_name,
        "reviewCount": kol.review_count,
        "avgRating": kol.avg_rating,
        "packages": len(kol.packages),
        "portfolio": len(kol.portfolio),
        "minPrice": min((p["price"] for p in kol.packages), default=0),
    }


def print_report(
    before: dict[str, KolSnapshot],
    after: dict[str, KolSnapshot],
    stats: dict[str, dict],
    golden: KolSnapshot,
) -> None:
    out = sys.stdout
    if hasattr(out, "reconfigure"):
        try:
            out.reconfigure(encoding="utf-8")
        except Exception:
            pass

    print("\n" + "=" * 72, file=out)
    print(f"BAO CAO ENRICH KOL — GOLDEN SAMPLE: {GOLDEN_SLUG}", file=out)
    print("=" * 72, file=out)
    print(
        f"\nMau chuan: reviews={golden.review_count}, packages={len(golden.packages)}, "
        f"portfolio={len(golden.portfolio)}, avgRating={golden.avg_rating}",
        file=out,
    )
    print(f"\nKOL đã cập nhật ({len(stats)}):")
    for slug in sorted(stats):
        s = stats[slug]
        print(
            f"  - {slug}: +{s['reviews_added']} reviews, +{s['packages_added']} packages, "
            f"+{s['portfolio_added']} portfolio"
        )
        if s["skipped"]:
            print(f"      bỏ qua: {', '.join(s['skipped'])}")

    print("\n--- Before/After (3 ví dụ) ---")
    examples = ["nha-hieu-review", "quynh-thi", "linh-baeli"]
    for slug in examples:
        if slug not in before:
            continue
        b = snapshot_summary(before[slug])
        a = snapshot_summary(after[slug])
        print(f"\n[{slug}]")
        print(f"  BEFORE: {json.dumps(b, ensure_ascii=False)}")
        print(f"  AFTER:  {json.dumps(a, ensure_ascii=False)}")

    totals = {
        "reviews": sum(s["reviews_added"] for s in stats.values()),
        "packages": sum(s["packages_added"] for s in stats.values()),
        "portfolio": sum(s["portfolio_added"] for s in stats.values()),
    }
    print("\n--- Tổng kết ---")
    print(f"  Reviews thêm:           {totals['reviews']}")
    print(f"  Pricing packages thêm:  {totals['packages']}")
    print(f"  Portfolio thêm:         {totals['portfolio']}")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--dry-run", action="store_true", help="Chỉ phân tích, không ghi DB")
    parser.add_argument("--apply", action="store_true", help="Áp dụng thay đổi vào DB")
    parser.add_argument(
        "--reprice-packages",
        action="store_true",
        help="Chỉ cập nhật lại giá pricing packages theo bảng giá mới",
    )
    args = parser.parse_args()
    if not args.dry_run and not args.apply and not args.reprice_packages:
        args.apply = True

    env = load_env(ENV_FILE)
    if not env.get("SPRING_DATASOURCE_URL"):
        print("Thiếu SPRING_DATASOURCE_URL trong .env", file=sys.stderr)
        return 1

    target_slugs = dedupe_target_slugs()
    print(f"Target KOLs ({len(target_slugs)}): {', '.join(target_slugs)}")

    snapshots = {slug: fetch_kol(slug) for slug in target_slugs}

    if args.reprice_packages:
        if args.dry_run:
            for slug, kol in snapshots.items():
                pkgs = build_packages(kol)
                video = next((p for p in pkgs if p["type"] == "VIDEO"), None)
                post = next((p for p in pkgs if p["type"] == "POST"), None)
                print(
                    f"{slug}: VIDEO={video['price'] if video else '-'}, "
                    f"POST={post['price'] if post else '-'}"
                )
            return 0
        conn = connect_db(env)
        try:
            updated = reprice_packages(conn, snapshots)
            print(f"Da cap nhat gia cho {sum(updated.values())} packages ({len(updated)} KOL)")
            for slug in ("linh-baeli", "quynh-thi", "babykopo-home"):
                pkgs = build_packages(snapshots[slug])
                print(f"  {slug}: {[(p['type'], p['price']) for p in pkgs]}")
        finally:
            conn.close()
        return 0

    golden = fetch_kol(GOLDEN_SLUG)
    before = snapshots
    plans = {slug: plan_enrichment(before[slug], golden) for slug in target_slugs}

    if args.dry_run:
        for slug, plan in plans.items():
            print(
                f"{slug}: reviews+{plan.reviews_to_add}, packages+{len(plan.packages_to_add)}, "
                f"portfolio+{len(plan.portfolio_to_add)}, skip={plan.skipped}"
            )
        return 0

    conn = connect_db(env)
    try:
        stats = apply_enrichment(conn, plans, before)
    finally:
        conn.close()

    after = {slug: fetch_kol(slug) for slug in target_slugs}
    print_report(before, after, stats, golden)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
