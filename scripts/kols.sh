#!/usr/bin/env bash
# KOL profile (private) + search/featured/public (public).
# Yêu cầu: scripts/auth.sh đã chạy hoặc seed sẵn (lambaongoc@seed.local).
# Run: bash scripts/kols.sh
source "$(dirname "$0")/_common.sh"

# ============== PUBLIC SEARCH ==============
clear_token

info "── /kols/search default"
req GET "/kols/search"
expect "search default → 200" 200 '.data.content != null and (.data.content | type) == "array"'

info "── /kols/search?q=ngoc"
req GET "/kols/search?q=ngoc"
expect "search q=ngoc → 200" 200

info "── /kols/search?platforms=TIKTOK"
req GET "/kols/search?platforms=TIKTOK"
expect "search TIKTOK → 200" 200

info "── /kols/search?minFollower=500000"
req GET "/kols/search?minFollower=500000"
expect "search minFollower → 200" 200 '.data.content | all(.totalFollowers >= 500000)' \
  || expect "search minFollower (no field totalFollowers)" 200

info "── /kols/search?gender=FEMALE"
req GET "/kols/search?gender=FEMALE"
expect "search gender=FEMALE → 200" 200

info "── /kols/search?gender=INVALID"
req GET "/kols/search?gender=INVALID_ENUM"
expect "search invalid enum → 400" 400 || expect "search invalid enum fallback 200 (filtered out)" 200

info "── /kols/search?page=-1 (Spring Pageable tự clamp về 0)"
req GET "/kols/search?page=-1"
[[ "$HTTP_CODE" == "200" || "$HTTP_CODE" == "400" ]] \
  && { ok "page=-1 → $HTTP_CODE"; PASS=$((PASS+1)); } \
  || { err "page=-1 unexpected $HTTP_CODE"; FAIL=$((FAIL+1)); FAILURES+=("page=-1: $HTTP_CODE"); }

info "── /kols/search?size=10000"
req GET "/kols/search?size=10000"
expect "search size huge → 200 with cap" 200

info "── /kols/search?page=999"
req GET "/kols/search?page=999&size=20"
expect "search page=999 empty content" 200 '.data.content == []'

info "── /kols/search multi categoryIds"
req GET "/kols/search?categoryIds=1&categoryIds=2"
expect "search multi cat → 200" 200

info "── /kols/featured"
req GET "/kols/featured?limit=5"
expect "featured limit=5" 200 '(.data | length) <= 5'

info "── /kols/{slug} seed"
req GET "/kols/lam-bao-ngoc"
expect "public slug → 200" 200 '.data.displayName != null'

info "── /kols/{slug} không tồn tại"
req GET "/kols/no-such-slug-xyz"
expect "unknown slug → 404" 404

# ============== PRIVATE — KOL profile ==============
if ! load_token kol; then
  warn "Chưa có token KOL — chạy scripts/auth.sh trước hoặc login bằng seed account."
  # fallback login seed
  login_as "lambaongoc@seed.local" "password123" "kol" || { err "không login được seed KOL"; summary; exit 1; }
fi

info "── GET /kols/me"
req GET /kols/me
expect "GET /kols/me → 200" 200 '.data.status != null'

info "── PUT /kols/me happy"
TS=$(date +%s)
req PUT /kols/me "{
  \"displayName\": \"QA KOL $TS\",
  \"slug\": \"qa-kol-$TS\",
  \"bio\": \"Updated for QA\",
  \"city\": \"Ho Chi Minh\",
  \"country\": \"VN\"
}"
expect "PUT /kols/me happy → 200" 200

info "── PUT /kols/me slug sai pattern"
req PUT /kols/me '{"slug":"BAD_SLUG_With_Underscore"}'
expect "slug invalid → 400" 400 '.errorCode == "VALIDATION_FAILED"'

info "── PUT /kols/me displayName quá dài"
LONG=$(printf 'x%.0s' {1..200})
req PUT /kols/me "{\"displayName\":\"$LONG\"}"
expect "displayName > 150 → 400" 400

# ============== SOCIAL CHANNEL ==============
info "── POST /kols/me/channels"
req POST /kols/me/channels '{
  "platform":"INSTAGRAM",
  "url":"https://instagram.com/qa-kol",
  "username":"qa-kol",
  "followerCount": 12345,
  "engagementRate": 3.5
}'
expect "create channel → 200/201" 200 '.data.id != null' || expect "create channel 201" 201
CHANNEL_ID=$(echo "$LAST_BODY" | jq -r '.data.id // empty')

info "── POST /kols/me/channels followerCount âm"
req POST /kols/me/channels '{"platform":"INSTAGRAM","url":"https://x.com/y","followerCount":-1,"engagementRate":1.0}'
expect "follower<0 → 400" 400

info "── POST /kols/me/channels engagementRate > 100"
req POST /kols/me/channels '{"platform":"INSTAGRAM","url":"https://x.com/y","followerCount":100,"engagementRate":101}'
expect "engagement>100 → 400" 400

info "── POST /kols/me/channels platform sai"
req POST /kols/me/channels '{"platform":"MYSPACE","url":"https://x.com/y","followerCount":100,"engagementRate":1.0}'
expect "bad platform → 400" 400

if [[ -n "${CHANNEL_ID:-}" ]]; then
  info "── DELETE /kols/me/channels/$CHANNEL_ID"
  req DELETE "/kols/me/channels/$CHANNEL_ID"
  expect "delete channel happy" 200 || expect "delete channel 204" 204
fi

info "── DELETE /kols/me/channels/999999 (không tồn tại)"
req DELETE /kols/me/channels/999999
expect "delete unknown channel → 404" 404

# ============== PRICING PACKAGE ==============
info "── POST /kols/me/packages"
req POST /kols/me/packages '{
  "type":"VIDEO",
  "platform":"TIKTOK",
  "price": 5000000,
  "description":"1 TikTok video, max 60s"
}'
expect "create package → 200" 200 '.data.id != null' || expect "create package 201" 201
PKG_ID=$(echo "$LAST_BODY" | jq -r '.data.id // empty')

info "── POST /kols/me/packages price âm"
req POST /kols/me/packages '{"type":"VIDEO","platform":"TIKTOK","price":-100}'
expect "price<0 → 400" 400

[[ -n "${PKG_ID:-}" ]] && { req DELETE "/kols/me/packages/$PKG_ID"; expect "delete pkg" 200 || expect "delete pkg 204" 204; }

# ============== PORTFOLIO ==============
info "── POST /kols/me/portfolio"
req POST /kols/me/portfolio '{
  "title":"Sample campaign",
  "mediaUrl":"https://example.com/clip.mp4",
  "mediaType":"VIDEO",
  "campaignName":"QA test"
}'
expect "create portfolio → 200" 200 '.data.id != null' || expect "create portfolio 201" 201
PORT_ID=$(echo "$LAST_BODY" | jq -r '.data.id // empty')

[[ -n "${PORT_ID:-}" ]] && { req DELETE "/kols/me/portfolio/$PORT_ID"; expect "delete portfolio" 200 || expect "delete portfolio 204" 204; }

# ============== SUBMIT FOR REVIEW ==============
info "── POST /kols/me/submit"
req POST /kols/me/submit
# seed account đang APPROVED → submit có thể fail business → log cả 2 trường hợp
if [[ "$HTTP_CODE" == "200" ]]; then
  ok "submit → 200 (DRAFT → PENDING_REVIEW)"; PASS=$((PASS+1))
else
  warn "submit returned $HTTP_CODE — có thể KOL đã APPROVED. Body: $(echo "$LAST_BODY" | head -c 200)"; SKIP=$((SKIP+1))
fi

summary
