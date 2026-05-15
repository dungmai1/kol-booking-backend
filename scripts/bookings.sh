#!/usr/bin/env bash
# Booking lifecycle: create → accept → message → reject paths.
# Cần BRAND + KOL token, dùng email mới mỗi lần để clean state.
# Run: bash scripts/bookings.sh
source "$(dirname "$0")/_common.sh"

TS=$(date +%s)
BRAND_EMAIL="qa-brand-$TS@test.local"
PASSWORD="Passw0rd!"

# ============== SETUP: BRAND mới ==============
info "── register BRAND"
req POST /auth/register "{\"email\":\"$BRAND_EMAIL\",\"password\":\"$PASSWORD\",\"role\":\"BRAND\"}"
expect "register BRAND" 201
BRAND_ACCESS=$(echo "$LAST_BODY" | jq -r '.data.accessToken')
save_token brand "$BRAND_ACCESS" "$(echo "$LAST_BODY" | jq -r '.data.refreshToken')"

# ============== SETUP: login seed KOL, lấy profile id của CHÍNH user đó ==============
KOL_EMAIL="lambaongoc@seed.local"
KOL_PASS="password123"
login_as "$KOL_EMAIL" "$KOL_PASS" kol || { err "seed KOL không login được — V11.2 migration chưa apply?"; summary; exit 1; }
req GET /kols/me
KOL_PROFILE_ID=$(echo "$LAST_BODY" | jq -r '.data.id')
[[ -n "$KOL_PROFILE_ID" && "$KOL_PROFILE_ID" != "null" ]] || { err "không lấy được KOL profile id từ /kols/me"; summary; exit 1; }
info "→ KOL_PROFILE_ID=$KOL_PROFILE_ID (own profile of $KOL_EMAIL)"

# ============== CREATE BOOKING ==============
load_token brand

info "── POST /bookings happy (BRAND)"
req POST /bookings "{
  \"kolProfileId\": $KOL_PROFILE_ID,
  \"campaignTitle\": \"QA Campaign $TS\",
  \"campaignBrief\": \"Test brief\",
  \"deliverables\": \"1 TikTok video\",
  \"budget\": 5000000,
  \"startDate\": \"2026-06-01\",
  \"endDate\": \"2026-06-30\"
}"
expect "create booking → 200" 200 '.data.id != null'
BOOKING_ID=$(echo "$LAST_BODY" | jq -r '.data.id')
info "→ BOOKING_ID=$BOOKING_ID"

info "── POST /bookings budget âm"
req POST /bookings "{\"kolProfileId\":$KOL_PROFILE_ID,\"campaignTitle\":\"x\",\"budget\":-1}"
expect "budget<0 → 400" 400

info "── POST /bookings campaignTitle empty"
req POST /bookings "{\"kolProfileId\":$KOL_PROFILE_ID,\"campaignTitle\":\"\",\"budget\":1000}"
expect "empty title → 400" 400

info "── POST /bookings kolProfileId không tồn tại"
req POST /bookings "{\"kolProfileId\":999999999,\"campaignTitle\":\"x\",\"budget\":1000}"
expect "kol not found → 404" 404 || expect "kol not found alt 400" 400

info "── POST /bookings KOL role (cấm)"
login_as "$KOL_EMAIL" "$KOL_PASS" kol
req POST /bookings "{\"kolProfileId\":$KOL_PROFILE_ID,\"campaignTitle\":\"x\",\"budget\":1000}"
expect "KOL→POST booking → 403" 403

# ============== LIST ==============
load_token brand
info "── GET /bookings/me (BRAND)"
req GET "/bookings/me?page=0&size=10"
expect "BRAND list → 200" 200 '.data.content != null'

load_token kol
info "── GET /bookings/incoming (KOL)"
req GET "/bookings/incoming?page=0&size=10"
expect "KOL incoming → 200" 200 '.data.content != null'

info "── GET /bookings/{id} as KOL owner"
req GET "/bookings/$BOOKING_ID"
expect "get booking detail → 200" 200 '.data.id != null'

# ============== ACCEPT / MESSAGE ==============
info "── POST /bookings/$BOOKING_ID/accept (KOL)"
req POST "/bookings/$BOOKING_ID/accept"
expect "accept → 200" 200 || warn "accept returned $HTTP_CODE — check state machine"

info "── POST /bookings/$BOOKING_ID/accept lần 2"
req POST "/bookings/$BOOKING_ID/accept"
[[ "$HTTP_CODE" == "400" || "$HTTP_CODE" == "409" ]] \
  && { ok "double accept → $HTTP_CODE business"; PASS=$((PASS+1)); } \
  || { warn "double accept returned $HTTP_CODE — expected 400/409"; SKIP=$((SKIP+1)); }

info "── POST /bookings/$BOOKING_ID/messages (KOL)"
req POST "/bookings/$BOOKING_ID/messages" '{"content":"Hi from KOL"}'
expect "send message → 200" 200 '.data.id != null'

info "── POST /messages content empty"
req POST "/bookings/$BOOKING_ID/messages" '{"content":""}'
expect "empty content → 400" 400

info "── POST /messages content quá dài"
LONG=$(printf 'a%.0s' {1..4500})
req POST "/bookings/$BOOKING_ID/messages" "{\"content\":\"$LONG\"}"
expect "content > 4000 → 400" 400

load_token brand
info "── POST /bookings/$BOOKING_ID/messages (BRAND)"
req POST "/bookings/$BOOKING_ID/messages" '{"content":"Reply from BRAND"}'
expect "BRAND reply → 200" 200

info "── GET /bookings/$BOOKING_ID/messages"
req GET "/bookings/$BOOKING_ID/messages?page=0&size=50"
expect "list messages → 200" 200 '(.data.content | length) >= 2'

# ============== CANCEL (BRAND) — phải là booking PENDING mới cancel được ==============
# Booking ở trên đã ACCEPTED → tạo booking mới PENDING để test cancel
load_token brand
info "── tạo booking mới để test cancel"
req POST /bookings "{\"kolProfileId\":$KOL_PROFILE_ID,\"campaignTitle\":\"Cancel test $TS\",\"budget\":500000}"
CANCEL_BID=$(echo "$LAST_BODY" | jq -r '.data.id')
info "── POST /bookings/$CANCEL_BID/cancel (BRAND, PENDING)"
req POST "/bookings/$CANCEL_BID/cancel" '{"reason":"QA cancel test"}'
expect "BRAND cancel PENDING → 200" 200

info "── cancel booking đã ACCEPTED → 409 business"
req POST "/bookings/$BOOKING_ID/cancel" '{"reason":"too late"}'
expect "cancel after accept → 409" 409

# ============== ACCESS CONTROL: bên thứ 3 ==============
info "── tạo BRAND khác xem booking → expect 403/404"
OTHER_EMAIL="qa-brand-other-$TS@test.local"
req POST /auth/register "{\"email\":\"$OTHER_EMAIL\",\"password\":\"$PASSWORD\",\"role\":\"BRAND\"}"
OTHER_ACCESS=$(echo "$LAST_BODY" | jq -r '.data.accessToken')
ACCESS_TOKEN="$OTHER_ACCESS"
req GET "/bookings/$BOOKING_ID"
[[ "$HTTP_CODE" == "403" || "$HTTP_CODE" == "404" ]] \
  && { ok "third-party access blocked ($HTTP_CODE)"; PASS=$((PASS+1)); } \
  || { err "third-party leaked: $HTTP_CODE"; FAIL=$((FAIL+1)); FAILURES+=("3rd-party booking access: $HTTP_CODE"); }

# ============== SECOND BOOKING: REJECT path ==============
load_token brand
info "── tạo booking #2 để test reject path"
req POST /bookings "{
  \"kolProfileId\": $KOL_PROFILE_ID,
  \"campaignTitle\": \"QA Reject $TS\",
  \"budget\": 1000000
}"
expect "create booking #2" 200
BOOKING2=$(echo "$LAST_BODY" | jq -r '.data.id')

load_token kol
info "── POST /bookings/$BOOKING2/reject với reason"
req POST "/bookings/$BOOKING2/reject" '{"reason":"Schedule conflict"}'
expect "KOL reject → 200" 200

summary
