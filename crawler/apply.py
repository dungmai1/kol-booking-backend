"""Apply ``output/kol_full_import_v2.sql`` to a Postgres database.

By default the script does CLEANUP (deletes existing rows imported by this
crawler, matched by ``email LIKE '%@kols-koc.imported'``) followed by IMPORT
in a single transaction — pass ``--no-cleanup`` to skip the delete step and
just upsert.

Connection
----------
Pass a libpq DSN via ``--dsn`` or set the ``KOL_DB_DSN`` env var. Examples::

    --dsn "postgresql://user:pass@host:5432/dbname"
    --dsn "host=localhost port=5432 dbname=kol_booking user=postgres password=secret"

Run ``python apply.py --help`` for more.
"""

from __future__ import annotations

import argparse
import os
import sys
from pathlib import Path

import psycopg

if sys.stdout.encoding and sys.stdout.encoding.lower() != "utf-8":
    try:
        sys.stdout.reconfigure(encoding="utf-8")
    except Exception:
        pass

ROOT = Path(__file__).resolve().parent
DEFAULT_SQL = ROOT / "output" / "kol_full_import_v2.sql"

CLEANUP_SQL = """\
DELETE FROM kol_portfolio_item
WHERE kol_profile_id IN (
    SELECT kp.id FROM kol_profile kp
    JOIN app_user au ON au.id = kp.user_id
    WHERE au.email LIKE '%@kols-koc.imported'
);

DELETE FROM kol_social_channel
WHERE kol_profile_id IN (
    SELECT kp.id FROM kol_profile kp
    JOIN app_user au ON au.id = kp.user_id
    WHERE au.email LIKE '%@kols-koc.imported'
);

DELETE FROM kol_profile
WHERE user_id IN (SELECT id FROM app_user WHERE email LIKE '%@kols-koc.imported');

DELETE FROM app_user WHERE email LIKE '%@kols-koc.imported';
"""


def main() -> int:
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("--dsn", default=os.environ.get("KOL_DB_DSN"), help="libpq DSN. Defaults to $KOL_DB_DSN.")
    p.add_argument("--sql", type=Path, default=DEFAULT_SQL, help="Path to the import SQL file.")
    p.add_argument("--no-cleanup", action="store_true", help="Skip the DELETE step; just run the (idempotent) import.")
    p.add_argument("--dry-run", action="store_true", help="BEGIN, run, then ROLLBACK — verifies syntax without committing.")
    p.add_argument("--yes", action="store_true", help="Skip the interactive confirmation.")
    args = p.parse_args()

    if not args.dsn:
        print(
            "ERROR: no DSN provided.\n"
            "Pass --dsn 'postgresql://user:pass@host:5432/db' or set $KOL_DB_DSN.",
            file=sys.stderr,
        )
        return 2

    if not args.sql.exists():
        print(f"ERROR: import SQL not found at {args.sql}\nRun generate_sql.py first.", file=sys.stderr)
        return 2

    sql_body = args.sql.read_text(encoding="utf-8")
    if not sql_body.strip():
        print("ERROR: SQL file is empty.", file=sys.stderr)
        return 2

    # Pre-flight check: confirm we're talking to the right schema before doing anything.
    with psycopg.connect(args.dsn, autocommit=True) as conn:
        with conn.cursor() as cur:
            cur.execute(
                "SELECT current_database(), current_user, "
                "(SELECT COUNT(*) FROM information_schema.tables "
                " WHERE table_name IN ('app_user','kol_profile','kol_social_channel','kol_portfolio_item'))"
            )
            db, usr, table_hits = cur.fetchone()
            if table_hits != 4:
                print(
                    f"ERROR: target DB '{db}' (user '{usr}') is missing one or more KOL tables "
                    f"(found {table_hits}/4). Refusing to proceed.",
                    file=sys.stderr,
                )
                return 3
            cur.execute("SELECT COUNT(*) FROM app_user WHERE email LIKE '%@kols-koc.imported'")
            existing = cur.fetchone()[0]

    print(f"Target DB: {db} (user {usr})")
    print(f"Existing imported rows (matched by '%@kols-koc.imported'): {existing} app_user records")
    print(f"Import file: {args.sql} ({sql_body.count(chr(10))+1} lines)")
    print(f"Mode: {'DRY-RUN (rollback)' if args.dry_run else 'COMMIT'}, "
          f"cleanup: {'skip' if args.no_cleanup else 'YES (delete then re-import)'}")

    if not args.yes and not args.dry_run:
        print()
        ans = input("Type 'apply' to proceed: ").strip().lower()
        if ans != "apply":
            print("Aborted.", file=sys.stderr)
            return 1

    with psycopg.connect(args.dsn) as conn:
        with conn.cursor() as cur:
            if not args.no_cleanup:
                print("\n-- CLEANUP --")
                cur.execute(CLEANUP_SQL)
                print(f"  deleted {cur.rowcount} rows from app_user (cascaded children)")

            print("\n-- IMPORT --")
            cur.execute(sql_body)

            cur.execute("SELECT COUNT(*) FROM app_user WHERE email LIKE '%@kols-koc.imported'")
            after_users = cur.fetchone()[0]
            cur.execute(
                "SELECT COUNT(*) FROM kol_profile kp "
                "JOIN app_user au ON au.id = kp.user_id "
                "WHERE au.email LIKE '%@kols-koc.imported'"
            )
            after_profiles = cur.fetchone()[0]
            cur.execute(
                "SELECT COUNT(*) FROM kol_social_channel ksc "
                "JOIN kol_profile kp ON kp.id = ksc.kol_profile_id "
                "JOIN app_user au ON au.id = kp.user_id "
                "WHERE au.email LIKE '%@kols-koc.imported'"
            )
            after_socials = cur.fetchone()[0]
            cur.execute(
                "SELECT COUNT(*) FROM kol_portfolio_item kpi "
                "JOIN kol_profile kp ON kp.id = kpi.kol_profile_id "
                "JOIN app_user au ON au.id = kp.user_id "
                "WHERE au.email LIKE '%@kols-koc.imported'"
            )
            after_portfolio = cur.fetchone()[0]
            cur.execute(
                "SELECT COUNT(*) FROM kol_profile kp "
                "JOIN app_user au ON au.id = kp.user_id "
                "WHERE au.email LIKE '%@kols-koc.imported' AND kp.status = 'APPROVED'"
            )
            approved = cur.fetchone()[0]

        if args.dry_run:
            conn.rollback()
            print("\nDRY-RUN — rolled back. No changes persisted.")
        else:
            conn.commit()

    print("\nResult counts (rows tagged '%@kols-koc.imported'):")
    print(f"  app_user            : {after_users}")
    print(f"  kol_profile         : {after_profiles}")
    print(f"   of which APPROVED  : {approved}")
    print(f"  kol_social_channel  : {after_socials}")
    print(f"  kol_portfolio_item  : {after_portfolio}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
