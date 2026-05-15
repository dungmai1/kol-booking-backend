# KOL Crawler — kols-koc.com

Re-crawls KOL data from `https://kols-koc.com/all-influencers/` (pages 1–21,
~250 KOL) and produces an idempotent SQL file ready to apply against the
`kol-booking-backend` Postgres schema.

The original `kol_full_import_clean_1.sql` (at repo root) covered only pages
1–9 and stored no avatars or portfolio images. This crawler is a re-write that
fills both gaps.

## Pipeline (2 steps)

```
[1] crawl.py         ─►  data/kols.json
                         data/images/<slug>/avatar.jpg
                         data/images/<slug>/portfolio_N.jpg
                         data/raw/page_N.html         (HTML cache)
                         data/raw/<slug>.html

[2] generate_sql.py  ─►  output/kol_full_import_v2.sql
```

Splitting into two steps means:
- Re-running step 2 (SQL format change) does NOT re-crawl the network.
- `data/kols.json` is human-readable — easy to inspect/diff before importing.
- HTML cache in `data/raw/` lets you re-parse if a selector bug is fixed.

## Setup

```bash
# From repo root
cd crawler
python -m venv .venv
.venv\Scripts\activate          # Windows
# source .venv/bin/activate     # macOS/Linux
pip install -r requirements.txt
```

Requires Python 3.10+.

## Run

```bash
# Step 1 — full crawl (~5–10 min, polite 1s delay between requests)
python crawl.py

# Or limit to a few pages while testing
python crawl.py --start-page 1 --end-page 1

# Skip image download (much faster, useful for dev iteration)
python crawl.py --no-images

# Step 2 — generate SQL
python generate_sql.py

# The image URL prefix in the SQL defaults to /files/kol-import/
# Change it if your app serves static files from a different path:
python generate_sql.py --image-url-prefix https://cdn.example.com/kol-import/
```

## Output

After both steps:

```
crawler/
├── data/
│   ├── kols.json                          # ~250 KOL records
│   └── images/
│       ├── ngo-thi-kim-yen/
│       │   ├── avatar.jpg
│       │   ├── portfolio_1.jpg
│       │   └── portfolio_2.jpg
│       └── ...
└── output/
    └── kol_full_import_v2.sql             # apply with psql
```

## Apply to DB

Two ways:

### Option A — `apply.py` (recommended)

Wraps the SQL with cleanup + verification + idempotent import in a single
transaction. Reports row counts before/after and refuses to run against the
wrong schema.

```bash
# Pass the DSN directly (use psycopg/libpq URI form):
.venv\Scripts\python apply.py \
    --dsn "postgresql://USER:PASSWORD@HOST:5432/kol_booking"

# Or set $KOL_DB_DSN once:
set KOL_DB_DSN=postgresql://USER:PASSWORD@HOST:5432/kol_booking
.venv\Scripts\python apply.py

# Test syntax first without committing (BEGIN; ... ROLLBACK):
.venv\Scripts\python apply.py --dry-run

# Skip the interactive 'apply' confirmation:
.venv\Scripts\python apply.py --yes
```

The script:
1. Validates the target DB has `app_user`, `kol_profile`, `kol_social_channel`,
   `kol_portfolio_item`. Aborts otherwise.
2. Reports how many `%@kols-koc.imported` rows already exist.
3. Asks for confirmation (skip with `--yes`).
4. Runs CLEANUP (only deletes rows tagged `%@kols-koc.imported` — never
   touches `@seed.local` or anything else).
5. Runs the import SQL.
6. Prints final row counts including how many `kol_profile` rows are
   `APPROVED`.

### Option B — raw `psql`

```bash
psql "postgresql://USER:PASSWORD@HOST:5432/kol_booking" \
     -f output/kol_full_import_v2.sql
```

The SQL is **idempotent**: re-running it inserts only rows that don't exist
yet (matched by `email` for `app_user`, by `slug` for `kol_profile`, by
`(profile, url)` for `kol_social_channel`, by `(profile, media_url)` for
`kol_portfolio_item`).

The SQL file's own CLEANUP block is commented out — `psql` won't delete
unless you uncomment it. `apply.py` runs cleanup separately.

## Image hosting

Images are saved locally under `crawler/data/images/<slug>/`. The generated
SQL references them by path (default `/files/kol-import/<slug>/avatar.jpg`).
You have two options to actually serve them:

1. **Static mount**: configure Spring to serve `crawler/data/images/` at the
   path prefix you passed to `--image-url-prefix`.
2. **Upload to your storage**: copy the image tree to wherever
   `FileStorageService` reads from, then re-run `generate_sql.py` with the
   matching `--image-url-prefix`.

The crawler keeps original URLs in `data/kols.json` (under `avatar_remote_url`
and `portfolio[*].remote_url`) so you can re-host them anywhere later.

## Re-running

The HTML cache makes re-crawls cheap:

- If `data/raw/page_N.html` exists, the list page is not re-fetched.
- If `data/raw/<slug>.html` exists, the detail page is not re-fetched.
- If `data/images/<slug>/avatar.jpg` exists, the avatar is not re-downloaded.

Delete `data/raw/` to force a full network re-crawl. Delete
`data/images/<slug>/` to re-download a single KOL's images.

## Politeness

- 1 second delay between page-list fetches (`--page-delay`).
- 0.5 second delay between detail-page fetches (`--detail-delay`).
- Image downloads run in a small thread pool (default 4 workers,
  `--image-workers`).
- Standard browser User-Agent.

If you get rate-limited, raise the delay flags.
