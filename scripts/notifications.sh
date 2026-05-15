#!/usr/bin/env bash
# Notification list / unread / mark-read.
# Run: bash scripts/notifications.sh
source "$(dirname "$0")/_common.sh"

# Login bất kỳ user
login_as "lambaongoc@seed.local" "password123" kol

info "── GET /notifications/me"
req GET "/notifications/me?page=0&size=20"
expect "list → 200" 200 '.content != null or .data.content != null'

info "── GET /notifications/me?unreadOnly=true"
req GET "/notifications/me?unreadOnly=true&page=0&size=20"
expect "unreadOnly=true → 200" 200

info "── GET /notifications/me/unread-count"
req GET "/notifications/me/unread-count"
expect "unread count → 200" 200 '.count != null or .data.count != null'

info "── POST /notifications/me/read-all"
req POST "/notifications/me/read-all"
expect "read all → 200" 200

# Lấy 1 notification id để test PATCH (nếu có)
req GET "/notifications/me?page=0&size=1"
NOTI_ID=$(echo "$LAST_BODY" | jq -r '.content[0].id // .data.content[0].id // empty')
if [[ -n "$NOTI_ID" ]]; then
  info "── PATCH /notifications/$NOTI_ID/read"
  req PATCH "/notifications/$NOTI_ID/read"
  expect "mark read → 200" 200

  info "── PATCH /notifications/$NOTI_ID/read lần 2"
  req PATCH "/notifications/$NOTI_ID/read"
  expect "mark read idempotent → 200" 200

  info "── PATCH notification của user khác (login KOL khác, dùng cùng NOTI_ID)"
  login_as "tien.tien@seed.local" "password123" kol2
  req PATCH "/notifications/$NOTI_ID/read"
  [[ "$HTTP_CODE" == "403" || "$HTTP_CODE" == "404" ]] \
    && { ok "cross-user mark-read blocked ($HTTP_CODE)"; PASS=$((PASS+1)); } \
    || { err "leak: $HTTP_CODE"; FAIL=$((FAIL+1)); FAILURES+=("noti cross-user: $HTTP_CODE"); }
else
  skip "không có notification — bỏ qua PATCH tests"
fi

info "── GET /notifications/me anonymous"
clear_token
req GET "/notifications/me"
expect "anon → 401" 401

summary
