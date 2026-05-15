"""Crawler for https://kols-koc.com/all-influencers/

Walks every page (1..N), follows each /influencers/<slug>/ detail page,
extracts profile metadata + social channels, and downloads avatar +
portfolio images. Writes the result to ``data/kols.json``.

Run ``python crawl.py --help`` for flags.
"""

from __future__ import annotations

import argparse
import json
import re
import sys
import time
from concurrent.futures import ThreadPoolExecutor, as_completed
from dataclasses import asdict, dataclass, field
from pathlib import Path
from typing import Iterable
from urllib.parse import urljoin, urlparse

import requests
from bs4 import BeautifulSoup, Tag

# UTF-8 stdout on Windows consoles that default to cp1252.
if sys.stdout.encoding and sys.stdout.encoding.lower() != "utf-8":
    try:
        sys.stdout.reconfigure(encoding="utf-8")
    except Exception:
        pass

BASE = "https://kols-koc.com"
LIST_URL = f"{BASE}/all-influencers/"
DEFAULT_END_PAGE = 21

UA = (
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
    "AppleWebKit/537.36 (KHTML, like Gecko) "
    "Chrome/120.0.0.0 Safari/537.36"
)

# Map kols-koc.com social-icon filename → schema platform code.
# The icon URL is the only stable signal because the link text is just a
# follower count ("376K+ followers").
PLATFORM_BY_ICON: dict[str, str] = {
    "tiktok": "TIKTOK",
    "fb": "FACEBOOK",
    "facebook": "FACEBOOK",
    "ig": "INSTAGRAM",
    "insta": "INSTAGRAM",
    "instagram": "INSTAGRAM",
    "youtube": "YOUTUBE",
    "yt": "YOUTUBE",
}

# Fallback: detect platform from URL host.
PLATFORM_BY_HOST: list[tuple[str, str]] = [
    ("tiktok.com", "TIKTOK"),
    ("facebook.com", "FACEBOOK"),
    ("fb.com", "FACEBOOK"),
    ("instagram.com", "INSTAGRAM"),
    ("youtube.com", "YOUTUBE"),
    ("youtu.be", "YOUTUBE"),
]

ROOT = Path(__file__).resolve().parent
DATA_DIR = ROOT / "data"
RAW_DIR = DATA_DIR / "raw"
IMG_DIR = DATA_DIR / "images"


@dataclass
class SocialChannel:
    platform: str
    url: str
    username: str | None
    follower_count: int

    @property
    def key(self) -> tuple[str, str]:
        return (self.platform, self.url)


@dataclass
class PortfolioItem:
    title: str
    remote_url: str
    local_path: str | None  # relative to crawler/ dir, e.g. data/images/<slug>/portfolio_1.jpg


@dataclass
class Kol:
    slug: str
    display_name: str
    profile_url: str
    bio: str
    personal_categories: list[str] = field(default_factory=list)  # "Phân loại" tags
    service_categories: list[str] = field(default_factory=list)  # "Lĩnh vực" tags
    avatar_remote_url: str | None = None
    avatar_local_path: str | None = None
    socials: list[SocialChannel] = field(default_factory=list)
    portfolio: list[PortfolioItem] = field(default_factory=list)


# ---------------------------------------------------------------------------
# HTTP / cache
# ---------------------------------------------------------------------------


class HttpCache:
    """Minimal disk cache so re-runs don't re-fetch the same HTML."""

    def __init__(self, raw_dir: Path, session: requests.Session, page_delay: float):
        self.raw_dir = raw_dir
        self.session = session
        self.page_delay = page_delay
        self.raw_dir.mkdir(parents=True, exist_ok=True)
        self._last_fetch_at = 0.0

    def get(self, url: str, cache_key: str) -> str:
        cache_path = self.raw_dir / cache_key
        if cache_path.exists() and cache_path.stat().st_size > 0:
            return cache_path.read_text(encoding="utf-8")

        # Polite delay between live requests.
        delta = time.monotonic() - self._last_fetch_at
        if delta < self.page_delay:
            time.sleep(self.page_delay - delta)

        resp = self.session.get(url, timeout=30)
        self._last_fetch_at = time.monotonic()
        resp.raise_for_status()
        # The site is UTF-8; force it to avoid requests guessing wrong.
        resp.encoding = "utf-8"
        cache_path.write_text(resp.text, encoding="utf-8")
        return resp.text


# ---------------------------------------------------------------------------
# Parsing
# ---------------------------------------------------------------------------


def list_kol_links(html: str) -> list[str]:
    """Extract unique /influencers/<slug>/ profile URLs from a list page."""
    soup = BeautifulSoup(html, "lxml")
    seen: dict[str, None] = {}
    for a in soup.find_all("a", href=True):
        href = a["href"]
        if "/influencers/" not in href:
            continue
        # Skip the listing page itself, only keep individual profiles.
        m = re.match(r"^https?://[^/]+/influencers/([^/?#]+)/?$", href)
        if not m:
            continue
        seen.setdefault(href, None)
    return list(seen.keys())


def slug_from_profile_url(url: str) -> str:
    m = re.match(r"^https?://[^/]+/influencers/([^/?#]+)/?$", url)
    if not m:
        raise ValueError(f"not a profile URL: {url}")
    return m.group(1)


def _img_src(img: Tag) -> str | None:
    """Pick the real image URL from an <img>, accounting for lazy-loading.

    The site uses a lazy-load script that puts a base64 SVG placeholder in
    ``src`` and the real URL in ``data-src``.
    """
    for attr in ("data-src", "src"):
        v = img.get(attr)
        if not v or v.startswith("data:"):
            continue
        return v.strip()
    return None


def _platform_from_icon_url(icon_url: str) -> str | None:
    name = Path(urlparse(icon_url).path).stem.lower()
    # Filename examples: 'tiktok-1', 'fb', 'youtube', 'instagram'
    for key, code in PLATFORM_BY_ICON.items():
        if key in name:
            return code
    return None


def _platform_from_link_url(link_url: str) -> str | None:
    host = urlparse(link_url).netloc.lower()
    for needle, code in PLATFORM_BY_HOST:
        if needle in host:
            return code
    return None


def _username_from_link(link_url: str, platform: str) -> str | None:
    path = urlparse(link_url).path.strip("/")
    if not path:
        return None
    if platform == "TIKTOK":
        # /@username
        seg = path.split("/")[0]
        return seg.lstrip("@") or None
    if platform == "INSTAGRAM":
        return path.split("/")[0] or None
    if platform == "FACEBOOK":
        # FB has /username, /profile.php?id=...
        seg = path.split("/")[0]
        if seg == "profile.php":
            qs = urlparse(link_url).query
            m = re.search(r"id=(\d+)", qs)
            return f"profile_{m.group(1)}" if m else None
        return seg or None
    if platform == "YOUTUBE":
        # Handles /@handle, /channel/<id>, /c/<name>, /user/<name>.
        segs = [s for s in path.split("/") if s]
        if not segs:
            return None
        first = segs[0]
        if first.startswith("@"):
            return first.lstrip("@") or None
        if first in {"channel", "c", "user"} and len(segs) > 1:
            return segs[1]
        return first
    return None


_FOLLOWER_RE = re.compile(r"([\d.,]+)\s*([KMB])?", re.IGNORECASE)


def _parse_follower_count(text: str) -> int:
    """'376K+ followers' -> 376000.  '1.2M' -> 1_200_000.  '12,500' -> 12500."""
    if not text:
        return 0
    m = _FOLLOWER_RE.search(text)
    if not m:
        return 0
    num_str = m.group(1).replace(",", "")
    suffix = (m.group(2) or "").upper()
    try:
        num = float(num_str)
    except ValueError:
        return 0
    multiplier = {"K": 1_000, "M": 1_000_000, "B": 1_000_000_000}.get(suffix, 1)
    return int(num * multiplier)


def parse_detail(html: str, profile_url: str) -> Kol:
    soup = BeautifulSoup(html, "lxml")
    slug = slug_from_profile_url(profile_url)

    # Display name = first <h1>.
    h1 = soup.find("h1")
    display_name = h1.get_text(strip=True) if h1 else slug

    # Personal categories ("Phân loại"): links to /nhan_influencer/<slug>/
    personal: list[str] = []
    for a in soup.find_all("a", href=True):
        if "/nhan_influencer/" in a["href"]:
            txt = a.get_text(strip=True)
            if txt and txt not in personal:
                personal.append(txt)

    # Service categories ("Lĩnh vực"): <ul class="...danh_muc_dich_vu"> links
    service: list[str] = []
    for ul in soup.find_all("ul", class_=re.compile(r"danh_muc_dich_vu")):
        for a in ul.find_all("a"):
            txt = a.get_text(strip=True)
            if txt and txt not in service:
                service.append(txt)

    # Bio: <h4>Chi tiết</h4> followed by a div with the description.
    bio = ""
    for h4 in soup.find_all(["h4", "h3"]):
        if h4.get_text(strip=True).lower().startswith("chi tiết"):
            sib = h4.find_next_sibling()
            if sib:
                bio = sib.get_text(" ", strip=True)
            break

    # Avatar: <img> whose URL contains anhr-dai-dien (or alt mentions it).
    avatar_url: str | None = None
    for img in soup.find_all("img"):
        src = _img_src(img)
        alt = (img.get("alt") or "").lower()
        if not src:
            continue
        low = src.lower()
        if "anhr-dai-dien" in low or "dai-dien" in low or "anhr" in low or "đại diện" in alt:
            avatar_url = src
            break
    # Fallback: first /uploads/ image that is not a logo or social icon.
    if not avatar_url:
        for img in soup.find_all("img"):
            src = _img_src(img)
            if not src or "/uploads/" not in src:
                continue
            low = src.lower()
            if any(skip in low for skip in ("logo-kol", "tiktok", "/fb.", "instagram", "youtube", "contact-image")):
                continue
            avatar_url = src
            break

    # Social channels: pairs of <a href="<social>"> blocks under <h4>Social</h4>.
    socials: dict[tuple[str, str], SocialChannel] = {}
    social_h = next(
        (
            h
            for h in soup.find_all(["h4", "h3"])
            if h.get_text(strip=True).lower() == "social"
        ),
        None,
    )
    search_root = social_h.parent if social_h else soup
    for a in search_root.find_all("a", href=True):
        href = a["href"]
        platform = _platform_from_link_url(href)
        if not platform:
            # Maybe the <a> wraps a platform icon (e.g. tiktok-1.png).
            img = a.find("img")
            if img:
                src = _img_src(img) or ""
                platform = _platform_from_icon_url(src)
        if not platform:
            continue
        # Follower count: prefer the sibling text-link "376K+ followers".
        followers = 0
        text = a.get_text(" ", strip=True)
        if text and "follower" in text.lower():
            followers = _parse_follower_count(text)
        else:
            # Look for an adjacent <a> in the same wrapper with the count text.
            for sib in a.parent.parent.find_all("a") if a.parent and a.parent.parent else []:
                stxt = sib.get_text(" ", strip=True)
                if "follower" in stxt.lower():
                    followers = _parse_follower_count(stxt)
                    break
        username = _username_from_link(href, platform)
        ch = SocialChannel(
            platform=platform,
            url=href,
            username=username,
            follower_count=followers,
        )
        # Dedup by (platform, url); keep the entry with the higher follower count.
        existing = socials.get(ch.key)
        if existing is None or ch.follower_count > existing.follower_count:
            socials[ch.key] = ch

    # Portfolio: <img> whose URL contains hinh-sp- (sample work).
    portfolio: list[PortfolioItem] = []
    seen_remote: set[str] = set()
    for img in soup.find_all("img"):
        src = _img_src(img)
        if not src or "hinh-sp" not in src.lower():
            continue
        if src in seen_remote:
            continue
        seen_remote.add(src)
        # Title: nearest preceding <h2>/<h3> heading text, fallback to the alt.
        title = (img.get("alt") or "").strip() or display_name
        for prev in img.find_all_previous(["h2", "h3"], limit=1):
            t = prev.get_text(strip=True)
            if t:
                title = t
                break
        portfolio.append(PortfolioItem(title=title, remote_url=src, local_path=None))

    return Kol(
        slug=slug,
        display_name=display_name,
        profile_url=profile_url,
        bio=bio,
        personal_categories=personal,
        service_categories=service,
        avatar_remote_url=avatar_url,
        socials=list(socials.values()),
        portfolio=portfolio,
    )


# ---------------------------------------------------------------------------
# Image download
# ---------------------------------------------------------------------------


def _ext_from_url(url: str, default: str = ".jpg") -> str:
    path = urlparse(url).path
    ext = Path(path).suffix.lower()
    if ext in {".jpg", ".jpeg", ".png", ".webp", ".gif"}:
        return ext
    return default


def download_one(url: str, dest: Path, session: requests.Session) -> bool:
    """Download ``url`` to ``dest`` if it doesn't exist. Returns True on success."""
    if dest.exists() and dest.stat().st_size > 0:
        return True
    dest.parent.mkdir(parents=True, exist_ok=True)
    try:
        r = session.get(url, timeout=60, stream=True)
        r.raise_for_status()
        tmp = dest.with_suffix(dest.suffix + ".part")
        with tmp.open("wb") as f:
            for chunk in r.iter_content(chunk_size=64 * 1024):
                if chunk:
                    f.write(chunk)
        tmp.replace(dest)
        return True
    except requests.RequestException as e:
        print(f"  ! image download failed {url}: {e}", flush=True)
        if dest.exists():
            try:
                dest.unlink()
            except OSError:
                pass
        return False


def download_kol_images(
    kol: Kol,
    session: requests.Session,
    workers: int,
) -> None:
    jobs: list[tuple[str, Path, str]] = []  # (url, dest, label)
    if kol.avatar_remote_url:
        ext = _ext_from_url(kol.avatar_remote_url)
        dest = IMG_DIR / kol.slug / f"avatar{ext}"
        jobs.append((kol.avatar_remote_url, dest, "avatar"))
    for i, item in enumerate(kol.portfolio, start=1):
        ext = _ext_from_url(item.remote_url)
        dest = IMG_DIR / kol.slug / f"portfolio_{i}{ext}"
        jobs.append((item.remote_url, dest, f"portfolio_{i}"))
    if not jobs:
        return
    with ThreadPoolExecutor(max_workers=workers) as pool:
        futures = {pool.submit(download_one, url, dest, session): (url, dest, lbl) for url, dest, lbl in jobs}
        for fut in as_completed(futures):
            url, dest, lbl = futures[fut]
            ok = fut.result()
            if not ok:
                continue
            rel = dest.relative_to(ROOT).as_posix()
            if lbl == "avatar":
                kol.avatar_local_path = rel
            else:
                # Match back to the portfolio item by remote_url.
                for item in kol.portfolio:
                    if item.remote_url == url and item.local_path is None:
                        item.local_path = rel
                        break


# ---------------------------------------------------------------------------
# Orchestration
# ---------------------------------------------------------------------------


def crawl_pages(
    cache: HttpCache,
    start: int,
    end: int,
) -> list[str]:
    all_links: list[str] = []
    seen: set[str] = set()
    for page in range(start, end + 1):
        url = LIST_URL if page == 1 else f"{LIST_URL}page/{page}/"
        cache_key = f"page_{page}.html"
        try:
            html = cache.get(url, cache_key)
        except requests.HTTPError as e:
            print(f"page {page}: HTTP error {e} — stopping", flush=True)
            break
        links = list_kol_links(html)
        new_links = [l for l in links if l not in seen]
        for l in new_links:
            seen.add(l)
            all_links.append(l)
        print(f"page {page}: {len(links)} links ({len(new_links)} new)", flush=True)
    return all_links


def crawl_details(
    cache: HttpCache,
    profile_urls: Iterable[str],
    detail_delay: float,
    download_images: bool,
    image_workers: int,
    session: requests.Session,
) -> list[Kol]:
    out: list[Kol] = []
    last_live = 0.0
    for i, url in enumerate(profile_urls, start=1):
        slug = slug_from_profile_url(url)
        cache_key = f"detail_{slug}.html"
        cached_existed = (RAW_DIR / cache_key).exists()
        if not cached_existed:
            delta = time.monotonic() - last_live
            if delta < detail_delay:
                time.sleep(detail_delay - delta)
        try:
            html = cache.get(url, cache_key)
        except requests.HTTPError as e:
            print(f"  [{i}] {slug}: HTTP error {e} — skip", flush=True)
            continue
        if not cached_existed:
            last_live = time.monotonic()
        try:
            kol = parse_detail(html, url)
        except Exception as e:
            print(f"  [{i}] {slug}: parse error {e} — skip", flush=True)
            continue
        if download_images:
            download_kol_images(kol, session, image_workers)
        out.append(kol)
        print(
            f"  [{i}] {slug}: socials={len(kol.socials)} portfolio={len(kol.portfolio)} "
            f"avatar={'y' if kol.avatar_local_path else ('remote' if kol.avatar_remote_url else 'none')}",
            flush=True,
        )
    return out


def to_json(kols: list[Kol]) -> str:
    payload = {
        "source": LIST_URL,
        "count": len(kols),
        "kols": [_kol_to_dict(k) for k in kols],
    }
    return json.dumps(payload, ensure_ascii=False, indent=2)


def _kol_to_dict(k: Kol) -> dict:
    d = asdict(k)
    return d


def main() -> int:
    parser = argparse.ArgumentParser(description="Crawl kols-koc.com influencer directory.")
    parser.add_argument("--start-page", type=int, default=1)
    parser.add_argument("--end-page", type=int, default=DEFAULT_END_PAGE)
    parser.add_argument("--page-delay", type=float, default=1.0, help="Seconds between LIST page fetches.")
    parser.add_argument("--detail-delay", type=float, default=0.5, help="Seconds between DETAIL page fetches.")
    parser.add_argument("--no-images", action="store_true", help="Skip image downloads.")
    parser.add_argument("--image-workers", type=int, default=4)
    parser.add_argument(
        "--output",
        type=Path,
        default=DATA_DIR / "kols.json",
        help="Where to write the JSON output.",
    )
    args = parser.parse_args()

    DATA_DIR.mkdir(parents=True, exist_ok=True)
    RAW_DIR.mkdir(parents=True, exist_ok=True)
    if not args.no_images:
        IMG_DIR.mkdir(parents=True, exist_ok=True)

    session = requests.Session()
    session.headers.update({"User-Agent": UA, "Accept-Language": "vi,en;q=0.8"})

    cache = HttpCache(RAW_DIR, session, args.page_delay)

    print(f"== Crawling list pages {args.start_page}..{args.end_page} ==", flush=True)
    profile_urls = crawl_pages(cache, args.start_page, args.end_page)
    print(f"\nFound {len(profile_urls)} unique profile URLs.\n", flush=True)

    print("== Crawling detail pages ==", flush=True)
    kols = crawl_details(
        cache,
        profile_urls,
        detail_delay=args.detail_delay,
        download_images=not args.no_images,
        image_workers=args.image_workers,
        session=session,
    )

    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(to_json(kols), encoding="utf-8")

    total_socials = sum(len(k.socials) for k in kols)
    total_portfolio = sum(len(k.portfolio) for k in kols)
    with_avatar = sum(1 for k in kols if k.avatar_local_path)
    print(
        f"\nWrote {args.output} — {len(kols)} KOL, {total_socials} social channels, "
        f"{total_portfolio} portfolio items, {with_avatar} avatars on disk.",
        flush=True,
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
