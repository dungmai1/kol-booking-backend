#!/usr/bin/env bash
# Auth flow tests: register, login, refresh, logout, negatives.
# Run: bash scripts/auth.sh
source "$(dirname "$0")/_common.sh"

TS=$(date +%s)
EMAIL_KOL="qa-kol-$TS@test.local"
EMAIL_BRAND="qa-brand-$TS@test.local"
PASSWORD="Passw0rd!"

info "BASE_URL=$BASE_URL  TS=$TS"

# ============== REGISTER ==============
info "── register KOL"
req POST /auth/register "{\"email\":\"$EMAIL_KOL\",\"password\":\"$PASSWORD\",\"role\":\"KOL\"}"
expect "register KOL happy" 201 '.success == true and .data.accessToken != null and .data.role == "KOL"'

info "── register BRAND"
req POST /auth/register "{\"email\":\"$EMAIL_BRAND\",\"password\":\"$PASSWORD\",\"role\":\"BRAND\"}"
expect "register BRAND happy" 201 '.success == true and .data.role == "BRAND"'

info "── register duplicate email"
req POST /auth/register "{\"email\":\"$EMAIL_KOL\",\"password\":\"$PASSWORD\",\"role\":\"KOL\"}"
expect "register duplicate → 409/400" "409" || expect "register duplicate fallback 400" "400"

info "── register email sai format"
req POST /auth/register "{\"email\":\"not-an-email\",\"password\":\"$PASSWORD\",\"role\":\"KOL\"}"
expect "register invalid email → 400" "400" '.errorCode == "VALIDATION_FAILED"'

info "── register password < 8"
req POST /auth/register "{\"email\":\"x$TS@test.local\",\"password\":\"abc\",\"role\":\"KOL\"}"
expect "register short password → 400" "400" '.errorCode == "VALIDATION_FAILED"'

info "── register role không hợp lệ"
req POST /auth/register "{\"email\":\"y$TS@test.local\",\"password\":\"$PASSWORD\",\"role\":\"SUPER\"}"
expect "register invalid role → 400" "400"

# ============== LOGIN ==============
info "── login KOL happy"
req POST /auth/login "{\"email\":\"$EMAIL_KOL\",\"password\":\"$PASSWORD\"}"
expect "login KOL → 200" 200 '.data.accessToken != null and .data.refreshToken != null'
ACCESS_KOL=$(echo "$LAST_BODY" | jq -r '.data.accessToken')
REFRESH_KOL=$(echo "$LAST_BODY" | jq -r '.data.refreshToken')
save_token "kol" "$ACCESS_KOL" "$REFRESH_KOL"

info "── login sai password"
req POST /auth/login "{\"email\":\"$EMAIL_KOL\",\"password\":\"WrongPass!\"}"
expect "login wrong password → 401" "401" '.errorCode == "UNAUTHORIZED" or .success == false'

info "── login email không tồn tại"
req POST /auth/login "{\"email\":\"ghost-$TS@nope.local\",\"password\":\"$PASSWORD\"}"
expect "login unknown email → 401" "401"

info "── login với seed account"
req POST /auth/login '{"email":"lambaongoc@seed.local","password":"password123"}'
expect "login seed KOL → 200" 200 '.data.role == "KOL"'

# ============== REFRESH ==============
info "── refresh token happy"
req POST /auth/refresh "{\"refreshToken\":\"$REFRESH_KOL\"}"
expect "refresh → 200" 200 '.data.accessToken != null'

info "── refresh token rác"
req POST /auth/refresh '{"refreshToken":"this-is-not-a-jwt"}'
expect "refresh invalid → 401" "401" || expect "refresh invalid fallback 400" "400"

# ============== PROTECTED CHECK ==============
info "── GET /kols/me thiếu token"
clear_token
req GET /kols/me
# Spring Security trả 401 (Unauthorized) hoặc 403 (Access denied) tuỳ filter order
[[ "$HTTP_CODE" == "401" || "$HTTP_CODE" == "403" ]] \
  && { ok "no-token → $HTTP_CODE (anonymous denied)"; PASS=$((PASS+1)); } \
  || { err "no-token unexpected $HTTP_CODE"; FAIL=$((FAIL+1)); FAILURES+=("no-token: $HTTP_CODE"); }

info "── GET /kols/me với token rác"
ACCESS_TOKEN="thisIsNotAJwt"; req GET /kols/me
[[ "$HTTP_CODE" == "401" || "$HTTP_CODE" == "403" ]] \
  && { ok "bad-token → $HTTP_CODE"; PASS=$((PASS+1)); } \
  || { err "bad-token unexpected $HTTP_CODE"; FAIL=$((FAIL+1)); FAILURES+=("bad-token: $HTTP_CODE"); }

info "── GET /kols/me wrong role (BRAND token → KOL endpoint)"
req POST /auth/login "{\"email\":\"$EMAIL_BRAND\",\"password\":\"$PASSWORD\"}"
ACCESS_TOKEN=$(echo "$LAST_BODY" | jq -r '.data.accessToken')
req GET /kols/me
expect "BRAND→/kols/me → 403" "403"

info "── GET /kols/me với KOL token → 200"
load_token kol
req GET /kols/me
expect "KOL→/kols/me → 200" "200" '.data != null'

# ============== LOGOUT ==============
info "── logout"
req POST /auth/logout "{\"refreshToken\":\"$REFRESH_KOL\"}"
expect "logout → 200" "200"

info "── refresh sau khi logout"
req POST /auth/refresh "{\"refreshToken\":\"$REFRESH_KOL\"}"
expect "refresh after logout → 401" "401" || expect "refresh after logout fallback 400" "400"

# ============== PASSWORD RESET (smoke — token đến qua email) ==============
info "── forgot-password email tồn tại"
req POST /auth/forgot-password "{\"email\":\"$EMAIL_KOL\"}"
expect "forgot-password known email → 200" "200"

info "── forgot-password email không tồn tại (anti-enum)"
req POST /auth/forgot-password "{\"email\":\"nope-$TS@nope.local\"}"
expect "forgot-password unknown email → 200 (same response)" "200"

summary
