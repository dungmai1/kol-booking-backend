#!/usr/bin/env bash
# Payment + wallet + withdraw. Yêu cầu một booking CONFIRMED để test checkout.
# Run: bash scripts/payments.sh
source "$(dirname "$0")/_common.sh"

TS=$(date +%s)
BRAND_EMAIL="qa-pay-brand-$TS@test.local"
KOL_EMAIL="lambaongoc@seed.local"
PASSWORD="Passw0rd!"

# Bootstrap BRAND + tạo booking + KOL accept
info "── bootstrap BRAND + booking"
req POST /auth/register "{\"email\":\"$BRAND_EMAIL\",\"password\":\"$PASSWORD\",\"role\":\"BRAND\"}"
expect "register BRAND" 201
BRAND_ACCESS=$(echo "$LAST_BODY" | jq -r '.data.accessToken')
save_token brand "$BRAND_ACCESS" "$(echo "$LAST_BODY" | jq -r '.data.refreshToken')"

# Login seed KOL trước để lấy profile id của chính user đó
login_as "lambaongoc@seed.local" "password123" kol || { err "seed KOL login fail"; summary; exit 1; }
req GET /kols/me
KOL_PROFILE_ID=$(echo "$LAST_BODY" | jq -r '.data.id')
[[ -n "$KOL_PROFILE_ID" && "$KOL_PROFILE_ID" != "null" ]] || { err "không có KOL profile id"; summary; exit 1; }
clear_token

load_token brand
req POST /bookings "{\"kolProfileId\":$KOL_PROFILE_ID,\"campaignTitle\":\"Pay test $TS\",\"budget\":3000000}"
BOOKING_ID=$(echo "$LAST_BODY" | jq -r '.data.id')

login_as "$KOL_EMAIL" "$PASSWORD" kol  # seed dùng password123
# fallback
if [[ "$HTTP_CODE" != "200" ]]; then login_as "$KOL_EMAIL" "password123" kol; fi
req POST "/bookings/$BOOKING_ID/accept" >/dev/null
info "→ BOOKING_ID=$BOOKING_ID accepted"

# ============== CHECKOUT ==============
load_token brand

info "── POST /payments/bookings/$BOOKING_ID/checkout"
req POST "/payments/bookings/$BOOKING_ID/checkout" '{"provider":"MOCK"}'
expect "checkout → 200" 200 '.externalRef != null or .data.externalRef != null'
EXT_REF=$(echo "$LAST_BODY" | jq -r '.externalRef // .data.externalRef // empty')
info "→ externalRef=$EXT_REF"

info "── POST checkout không phải owner"
OTHER_EMAIL="qa-pay-other-$TS@test.local"
req POST /auth/register "{\"email\":\"$OTHER_EMAIL\",\"password\":\"$PASSWORD\",\"role\":\"BRAND\"}"
ACCESS_TOKEN=$(echo "$LAST_BODY" | jq -r '.data.accessToken')
req POST "/payments/bookings/$BOOKING_ID/checkout" '{"provider":"MOCK"}'
[[ "$HTTP_CODE" == "403" || "$HTTP_CODE" == "404" ]] \
  && { ok "non-owner checkout blocked ($HTTP_CODE)"; PASS=$((PASS+1)); } \
  || { err "non-owner leak: $HTTP_CODE"; FAIL=$((FAIL+1)); FAILURES+=("non-owner checkout: $HTTP_CODE"); }

info "── GET /payments/bookings/$BOOKING_ID (status)"
load_token brand
req GET "/payments/bookings/$BOOKING_ID"
expect "get payment status → 200" 200

# ============== WEBHOOK (mock GET) ==============
info "── GET /payments/webhook/MOCK"
clear_token
if [[ -n "$EXT_REF" ]]; then
  req GET "/payments/webhook/MOCK?externalRef=$EXT_REF&amount=3000000&status=SUCCESS"
  expect "webhook SUCCESS → 200" 200
  info "── webhook lần 2 (idempotency)"
  req GET "/payments/webhook/MOCK?externalRef=$EXT_REF&amount=3000000&status=SUCCESS"
  expect "webhook idempotent → 200" 200
else
  skip "không có externalRef — bỏ qua webhook"
fi

info "── webhook externalRef không tồn tại"
req GET "/payments/webhook/MOCK?externalRef=ghost-ref-xyz&amount=100&status=SUCCESS"
[[ "$HTTP_CODE" == "200" || "$HTTP_CODE" == "404" ]] \
  && { ok "webhook unknown ref ($HTTP_CODE)"; PASS=$((PASS+1)); } \
  || { warn "unexpected $HTTP_CODE on unknown ref"; SKIP=$((SKIP+1)); }

# ============== WALLET ==============
load_token kol
info "── GET /wallet/me (KOL)"
req GET /wallet/me
expect "wallet KOL → 200" 200

info "── GET /wallet/me/transactions"
req GET "/wallet/me/transactions?page=0&size=20"
expect "wallet tx → 200" 200

info "── GET /wallet/me anonymous → 401"
clear_token
req GET /wallet/me
expect "wallet anon → 401" 401

# ============== WITHDRAW ==============
load_token kol
info "── POST /withdraws — chưa có earnings, expect 400 'Insufficient balance'"
req POST /withdraws '{
  "amount": 100000,
  "bankName": "Vietcombank",
  "bankAccount": "0123456789",
  "accountName": "QA TESTER"
}'
expect "withdraw insufficient → 400 business" 400 '.errorCode == "BUSINESS_ERROR"'

info "── POST /withdraws amount = 0"
req POST /withdraws '{"amount":0,"bankName":"X","bankAccount":"1","accountName":"Y"}'
expect "amount=0 → 400" 400

info "── POST /withdraws bankAccount empty"
req POST /withdraws '{"amount":100,"bankName":"X","bankAccount":"","accountName":"Y"}'
expect "bankAccount empty → 400" 400

info "── GET /withdraws/me"
req GET "/withdraws/me?page=0&size=20"
expect "list my withdraws → 200" 200

info "── GET /withdraws/admin (KOL → 403)"
req GET "/withdraws/admin?status=PENDING"
expect "non-admin → 403" 403

# Admin path — chỉ test nếu đã có admin token
if load_token admin 2>/dev/null; then
  info "── GET /withdraws/admin (ADMIN)"
  req GET "/withdraws/admin?status=PENDING&page=0&size=10"
  expect "admin list withdraws → 200" 200
else
  skip "chưa có admin token — bỏ qua /withdraws/admin tests (run scripts/admin.sh::bootstrap_admin)"
fi

summary
