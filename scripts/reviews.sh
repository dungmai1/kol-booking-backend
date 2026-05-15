#!/usr/bin/env bash
# Review CRUD + public review listing.
# Run: bash scripts/reviews.sh
source "$(dirname "$0")/_common.sh"

# ============== Public listing trước ==============
clear_token

info "── pick một KOL từ search"
req GET "/kols/search?size=1"
KOL_PROFILE_ID=$(echo "$LAST_BODY" | jq -r '.data.content[0].id')
SLUG=$(echo "$LAST_BODY" | jq -r '.data.content[0].slug')

# Seed KOL có user_id biết trước (V8 seed dùng sequential id, lambaongoc thường id=10 sau v9).
# Dùng login để lấy userId thay vì cố parse public profile.
if [[ -n "$SLUG" && "$SLUG" != "null" ]]; then
  info "── GET /users/{id}/reviews — dùng userId từ auth response"
  login_as "lambaongoc@seed.local" "password123" tmp
  USER_ID=$(echo "$LAST_BODY" | jq -r '.data.userId')
  clear_token  # endpoint là public
  if [[ -n "$USER_ID" && "$USER_ID" != "null" ]]; then
    info "── GET /users/$USER_ID/reviews public"
    req GET "/users/$USER_ID/reviews?page=0&size=10"
    expect "public reviews → 200" 200
  else
    skip "không lấy được userId"
  fi
else
  skip "không có KOL trong search"
fi

# ============== POST review yêu cầu booking COMPLETED ==============
# Test này phụ thuộc booking lifecycle hoàn chỉnh (paid → delivered → approved).
# Ở scope smoke test, ta chỉ verify validation + access control.

# Tạo BRAND mới, booking PENDING
TS=$(date +%s)
BRAND_EMAIL="qa-rev-brand-$TS@test.local"
PASSWORD="Passw0rd!"
req POST /auth/register "{\"email\":\"$BRAND_EMAIL\",\"password\":\"$PASSWORD\",\"role\":\"BRAND\"}"
expect "register BRAND" 201
ACCESS_TOKEN=$(echo "$LAST_BODY" | jq -r '.data.accessToken')
save_token brand "$ACCESS_TOKEN" "$(echo "$LAST_BODY" | jq -r '.data.refreshToken')"

req POST /bookings "{\"kolProfileId\":$KOL_PROFILE_ID,\"campaignTitle\":\"Review test $TS\",\"budget\":100000}"
expect "create booking" 200
BOOKING_ID=$(echo "$LAST_BODY" | jq -r '.data.id')

info "── POST /bookings/$BOOKING_ID/reviews trên booking PENDING"
req POST "/bookings/$BOOKING_ID/reviews" '{"rating":5,"comment":"Great"}'
[[ "$HTTP_CODE" == "400" || "$HTTP_CODE" == "409" ]] \
  && { ok "review on non-completed → $HTTP_CODE business"; PASS=$((PASS+1)); } \
  || { err "expected 400/409, got $HTTP_CODE"; FAIL=$((FAIL+1)); FAILURES+=("review pending: $HTTP_CODE"); }

info "── POST review rating=0"
req POST "/bookings/$BOOKING_ID/reviews" '{"rating":0,"comment":"x"}'
expect "rating=0 → 400" 400

info "── POST review rating=6"
req POST "/bookings/$BOOKING_ID/reviews" '{"rating":6,"comment":"x"}'
expect "rating=6 → 400" 400

info "── POST review comment > 4000"
LONG=$(printf 'a%.0s' {1..4500})
req POST "/bookings/$BOOKING_ID/reviews" "{\"rating\":5,\"comment\":\"$LONG\"}"
expect "comment too long → 400" 400

info "── POST review thiếu token"
clear_token
req POST "/bookings/$BOOKING_ID/reviews" '{"rating":5}'
expect "review no-token → 401" 401

summary
