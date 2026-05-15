#!/usr/bin/env bash
# Brand profile + favorites.
# Run: bash scripts/brands.sh
source "$(dirname "$0")/_common.sh"

TS=$(date +%s)
BRAND_EMAIL="qa-brand-$TS@test.local"
PASSWORD="Passw0rd!"

info "── bootstrap fresh BRAND"
req POST /auth/register "{\"email\":\"$BRAND_EMAIL\",\"password\":\"$PASSWORD\",\"role\":\"BRAND\"}"
expect "register BRAND → 201" 201
ACCESS_TOKEN=$(echo "$LAST_BODY" | jq -r '.data.accessToken')
REFRESH_TOKEN=$(echo "$LAST_BODY" | jq -r '.data.refreshToken')
save_token brand "$ACCESS_TOKEN" "$REFRESH_TOKEN"
export ACCESS_TOKEN

info "── GET /brands/me"
req GET /brands/me
expect "GET /brands/me → 200" 200 '.data.status != null'

info "── GET /brands/me wrong role"
clear_token
# log in một KOL bất kỳ
login_as "lambaongoc@seed.local" "password123" "kol"
req GET /brands/me
expect "KOL→/brands/me → 403" 403

# trở lại BRAND
load_token brand

info "── PUT /brands/me happy"
req PUT /brands/me "{
  \"companyName\":\"QA Brand $TS\",
  \"taxCode\":\"0300000001\",
  \"industry\":\"FMCG\",
  \"website\":\"https://example.com\",
  \"contactName\":\"QA Tester\",
  \"contactPhone\":\"+84-901-234-567\",
  \"address\":\"1 Test St, HCMC\"
}"
expect "PUT /brands/me happy" 200

info "── PUT /brands/me contactPhone sai pattern"
req PUT /brands/me '{"contactPhone":"abcdef"}'
expect "phone pattern fail → 400" 400 '.errorCode == "VALIDATION_FAILED"'

info "── PUT /brands/me companyName > 200"
LONG=$(printf 'x%.0s' {1..250})
req PUT /brands/me "{\"companyName\":\"$LONG\"}"
expect "companyName too long → 400" 400

info "── POST /brands/me/submit"
req POST /brands/me/submit
expect "brand submit → 200" 200 || warn "submit returned $HTTP_CODE — check business state"

# ============== FAVORITES ==============
info "── tìm 1 KOL public để favorite"
clear_token
req GET "/kols/search?size=1"
KOL_ID=$(echo "$LAST_BODY" | jq -r '.data.content[0].id // empty')
if [[ -z "$KOL_ID" ]]; then err "không tìm được KOL nào để favorite"; summary; exit 1; fi
info "→ KOL_ID=$KOL_ID"

load_token brand

info "── POST /brands/me/favorites/$KOL_ID"
req POST "/brands/me/favorites/$KOL_ID"
expect "add favorite → 200/204" 200 || expect "add favorite → 204" 204

info "── POST favorite lần 2 (idempotent check)"
req POST "/brands/me/favorites/$KOL_ID"
if [[ "$HTTP_CODE" == "200" || "$HTTP_CODE" == "204" ]]; then
  ok "idempotent add favorite ($HTTP_CODE)"; PASS=$((PASS+1))
elif [[ "$HTTP_CODE" == "409" ]]; then
  ok "add duplicate favorite → 409 (non-idempotent contract)"; PASS=$((PASS+1))
else
  err "unexpected $HTTP_CODE on duplicate favorite"; FAIL=$((FAIL+1))
  FAILURES+=("dup favorite: $HTTP_CODE")
fi

info "── POST favorite KOL không tồn tại"
req POST "/brands/me/favorites/999999"
expect "favorite unknown KOL → 404" 404

info "── GET /brands/me/favorites"
req GET "/brands/me/favorites?page=0&size=10"
expect "list favorites → 200" 200 '.data.content != null'

info "── DELETE favorite"
req DELETE "/brands/me/favorites/$KOL_ID"
expect "remove favorite → 200/204" 200 || expect "remove favorite → 204" 204

info "── DELETE favorite chưa từng add"
req DELETE "/brands/me/favorites/$KOL_ID"
[[ "$HTTP_CODE" == "404" || "$HTTP_CODE" == "200" || "$HTTP_CODE" == "204" ]] \
  && { ok "remove non-existent favorite ($HTTP_CODE)"; PASS=$((PASS+1)); } \
  || { err "unexpected $HTTP_CODE"; FAIL=$((FAIL+1)); FAILURES+=("remove non-exist fav: $HTTP_CODE"); }

summary
