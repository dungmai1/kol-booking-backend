#!/usr/bin/env bash
# File upload tests.
# Run: bash scripts/files.sh
source "$(dirname "$0")/_common.sh"

FIXTURE_DIR="$SCRIPT_DIR/fixtures"
mkdir -p "$FIXTURE_DIR"
SAMPLE="$FIXTURE_DIR/sample.jpg"

# Tạo 1 JPG nhỏ giả (chỉ cần header valid)
if [[ ! -f "$SAMPLE" ]]; then
  # 1x1 JPEG bytes
  printf '\xff\xd8\xff\xe0\x00\x10JFIF\x00\x01\x01\x00\x00\x01\x00\x01\x00\x00\xff\xdb\x00C\x00\x08\x06\x06\x07\x06\x05\x08\x07\x07\x07\x09\x09\x08\n\x0c\x14\r\x0c\x0b\x0b\x0c\x19\x12\x13\x0f\x14\x1d\x1a\x1f\x1e\x1d\x1a\x1c\x1c $.'"' "'(7),01444\x1f''9=82<.342\xff\xc0\x00\x0b\x08\x00\x01\x00\x01\x01\x01\x11\x00\xff\xc4\x00\x1f\x00\x00\x01\x05\x01\x01\x01\x01\x01\x01\x00\x00\x00\x00\x00\x00\x00\x00\x01\x02\x03\x04\x05\x06\x07\x08\t\n\x0b\xff\xc4\x00\xb5\x10\x00\x02\x01\x03\x03\x02\x04\x03\x05\x05\x04\x04\x00\x00\x01}\x01\x02\x03\x00\x04\x11\x05\x12!1A\x06\x13Qa\x07"q\x142\x81\x91\xa1\x08#B\xb1\xc1\x15R\xd1\xf0$3br\x82\t\n\x16\x17\x18\x19\x1a%&'\''()*456789:CDEFGHIJSTUVWXYZcdefghijstuvwxyz\x83\x84\x85\x86\x87\x88\x89\x8a\x92\x93\x94\x95\x96\x97\x98\x99\x9a\xa2\xa3\xa4\xa5\xa6\xa7\xa8\xa9\xaa\xb2\xb3\xb4\xb5\xb6\xb7\xb8\xb9\xba\xc2\xc3\xc4\xc5\xc6\xc7\xc8\xc9\xca\xd2\xd3\xd4\xd5\xd6\xd7\xd8\xd9\xda\xe1\xe2\xe3\xe4\xe5\xe6\xe7\xe8\xe9\xea\xf1\xf2\xf3\xf4\xf5\xf6\xf7\xf8\xf9\xfa\xff\xda\x00\x08\x01\x01\x00\x00?\x00\xfb\xd0\xff\xd9' > "$SAMPLE"
fi

# Upload yêu cầu authenticated (không trong permitAll list). Login seed KOL.
login_as "lambaongoc@seed.local" "password123" kol || { err "không login được"; summary; exit 1; }

info "── POST /files/upload anonymous (cần 401)"
HTTP_CODE=$(curl -sS -o /tmp/upload.json -w '%{http_code}' \
  -X POST "$BASE_URL/files/upload" \
  -F "file=@$SAMPLE")
LAST_BODY=$(cat /tmp/upload.json)
expect "upload anon → 401" 401

info "── POST /files/upload happy (có token)"
HTTP_CODE=$(curl -sS -o /tmp/upload.json -w '%{http_code}' \
  -X POST "$BASE_URL/files/upload" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -F "file=@$SAMPLE")
LAST_BODY=$(cat /tmp/upload.json)
expect "upload jpg → 200" 200 '.data.url != null or .url != null'

info "── POST /files/upload không kèm file"
HTTP_CODE=$(curl -sS -o /tmp/upload.json -w '%{http_code}' \
  -X POST "$BASE_URL/files/upload" \
  -H "Authorization: Bearer $ACCESS_TOKEN")
LAST_BODY=$(cat /tmp/upload.json)
expect "missing file → 400" 400

info "── POST /files/upload với JSON content-type (sai)"
HTTP_CODE=$(curl -sS -o /tmp/upload.json -w '%{http_code}' \
  -X POST "$BASE_URL/files/upload" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{}')
LAST_BODY=$(cat /tmp/upload.json)
[[ "$HTTP_CODE" == "400" || "$HTTP_CODE" == "415" ]] \
  && { ok "wrong content-type → $HTTP_CODE"; PASS=$((PASS+1)); } \
  || { err "unexpected $HTTP_CODE"; FAIL=$((FAIL+1)); FAILURES+=("wrong CT: $HTTP_CODE"); }

# Skip big-file test by default (tốn disk)
skip "file > 20MB → 413 (manual: dd if=/dev/zero of=big.bin bs=1M count=25 && curl -F file=@big.bin)"

summary
