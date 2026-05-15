#!/usr/bin/env bash
# Admin endpoints. Yêu cầu ADMIN account.
# Vì chưa có ADMIN seed, có hàm bootstrap_admin() — chạy SQL UPDATE qua psql.
# Set ADMIN_EMAIL / ADMIN_PASSWORD trong scripts/.env.test, hoặc dùng default qa-admin@test.local.
# Run: bash scripts/admin.sh
source "$(dirname "$0")/_common.sh"

# AdminBootstrap.java auto-seed admin@kolbooking.local / Admin@123 trên startup.
# Override qua env: app.admin.email / app.admin.password.
ADMIN_EMAIL="${ADMIN_EMAIL:-admin@kolbooking.local}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-Admin@123}"

if ! login_as "$ADMIN_EMAIL" "$ADMIN_PASSWORD" admin; then
  err "Không login được admin với $ADMIN_EMAIL / $ADMIN_PASSWORD. Check AdminBootstrap đã chạy chưa."
  summary; exit 1
fi

# ============== 403 cho non-admin ==============
info "── non-admin gọi /admin/users → 403"
login_as "lambaongoc@seed.local" "password123" kol
req GET "/admin/users"
expect "KOL→/admin → 403" 403

# ============== ADMIN list profiles ==============
load_token admin

info "── GET /admin/kols?status=PENDING_REVIEW"
req GET "/admin/kols?status=PENDING_REVIEW&page=0&size=20"
expect "admin list KOLs → 200" 200

info "── GET /admin/kols (no filter)"
req GET "/admin/kols?page=0&size=20"
expect "admin list KOLs all → 200" 200

info "── GET /admin/brands?status=PENDING_REVIEW"
req GET "/admin/brands?status=PENDING_REVIEW&page=0&size=20"
expect "admin list brands → 200" 200

# ============== USER MANAGEMENT ==============
info "── GET /admin/users"
req GET "/admin/users?page=0&size=20"
expect "list users → 200" 200 '.content != null'

info "── GET /admin/users?q=ngoc&role=KOL"
req GET "/admin/users?q=ngoc&role=KOL&page=0&size=20"
expect "search users → 200" 200

# ============== CATEGORY CRUD ==============
TS=$(date +%s)
info "── POST /admin/categories"
req POST /admin/categories "{\"name\":\"QA Cat $TS\",\"slug\":\"qa-cat-$TS\"}"
expect "create category → 200" 200 '.id != null or .data.id != null'
CAT_ID=$(echo "$LAST_BODY" | jq -r '.id // .data.id // empty')

info "── POST /admin/categories slug trùng"
req POST /admin/categories "{\"name\":\"Dup\",\"slug\":\"qa-cat-$TS\"}"
expect "duplicate slug → 409/400" 409 || expect "duplicate slug fallback 400" 400

info "── POST /admin/categories slug sai pattern"
req POST /admin/categories '{"name":"Bad","slug":"BAD_SLUG"}'
expect "bad slug → 400" 400

if [[ -n "${CAT_ID:-}" ]]; then
  info "── PUT /admin/categories/$CAT_ID"
  req PUT "/admin/categories/$CAT_ID" "{\"name\":\"Updated $TS\",\"slug\":\"qa-cat-$TS\"}"
  expect "update category → 200" 200

  info "── DELETE /admin/categories/$CAT_ID"
  req DELETE "/admin/categories/$CAT_ID"
  [[ "$HTTP_CODE" == "200" || "$HTTP_CODE" == "204" ]] \
    && { ok "delete category → $HTTP_CODE"; PASS=$((PASS+1)); } \
    || { err "delete unexpected $HTTP_CODE"; FAIL=$((FAIL+1)); FAILURES+=("delete: $HTTP_CODE"); }

  info "── DELETE category đã xóa"
  req DELETE "/admin/categories/$CAT_ID"
  expect "delete deleted → 404" 404
fi

# ============== STATS ==============
info "── GET /admin/stats/overview"
req GET /admin/stats/overview
expect "stats overview → 200" 200

info "── GET /admin/stats/bookings"
req GET "/admin/stats/bookings"
expect "stats bookings → 200" 200

info "── GET /admin/stats/bookings?from&to (ISO Instant với Z)"
req GET "/admin/stats/bookings?from=2026-01-01T00:00:00Z&to=2026-05-15T23:59:59Z"
expect "stats bookings ranged → 200" 200

info "── GET /admin/stats/top-kols?limit=5"
req GET "/admin/stats/top-kols?limit=5"
expect "top kols → 200" 200

info "── GET /admin/stats/revenue"
req GET "/admin/stats/revenue"
expect "revenue → 200" 200

# ============== AUDIT LOGS ==============
info "── GET /admin/audit-logs"
req GET "/admin/audit-logs?page=0&size=50"
expect "audit logs → 200" 200

# ============== APPROVAL FLOW (smoke) ==============
info "── tìm 1 KOL profile bất kỳ để test endpoint approve (idempotent check)"
req GET "/admin/kols?page=0&size=1"
KOL_PROFILE_ID=$(echo "$LAST_BODY" | jq -r '.content[0].id // .data.content[0].id // empty')
if [[ -n "$KOL_PROFILE_ID" ]]; then
  info "── POST /admin/kols/$KOL_PROFILE_ID/approve"
  req POST "/admin/kols/$KOL_PROFILE_ID/approve"
  [[ "$HTTP_CODE" == "200" || "$HTTP_CODE" == "204" || "$HTTP_CODE" == "400" || "$HTTP_CODE" == "409" ]] \
    && { ok "approve returned $HTTP_CODE (400/409 nếu đã APPROVED)"; PASS=$((PASS+1)); } \
    || { err "unexpected $HTTP_CODE"; FAIL=$((FAIL+1)); FAILURES+=("approve: $HTTP_CODE"); }
else
  skip "không có KOL profile"
fi

summary
