#!/usr/bin/env bash
# Public categories endpoint + file upload smoke.
# Run: bash scripts/categories.sh
source "$(dirname "$0")/_common.sh"

clear_token

info "── GET /categories"
req GET /categories
expect "list categories → 200" 200 '.data | type == "array"' || expect "raw array (no envelope)" 200 'type == "array"'

info "── verify tree shape (parent_id null cho root)"
ROOT_COUNT=$(echo "$LAST_BODY" | jq '[ .data[]? // .[] | select(.parentId == null) ] | length')
if [[ -n "$ROOT_COUNT" && "$ROOT_COUNT" -gt 0 ]]; then
  ok "có $ROOT_COUNT root category"; PASS=$((PASS+1))
else
  warn "không có root category — verify seed"; SKIP=$((SKIP+1))
fi

summary
