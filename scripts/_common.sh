#!/usr/bin/env bash
# Shared helpers cho mọi test script. Source ở đầu mỗi file:
#   source "$(dirname "$0")/_common.sh"

set -uo pipefail

# --- Config ---
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
REPORTS_DIR="$SCRIPT_DIR/reports"
mkdir -p "$REPORTS_DIR"

# Load env nếu có
if [[ -f "$SCRIPT_DIR/.env.test" ]]; then
  set -a; source "$SCRIPT_DIR/.env.test"; set +a
fi
BASE_URL="${BASE_URL:-http://localhost:8080/api/v1}"

# Token cache mỗi role một file (lưu access + refresh)
TOKEN_DIR="$SCRIPT_DIR/.tokens"
mkdir -p "$TOKEN_DIR"

# --- Colors ---
if [[ -t 1 ]]; then
  C_RED=$'\033[31m'; C_GREEN=$'\033[32m'; C_YEL=$'\033[33m'
  C_BLUE=$'\033[34m'; C_DIM=$'\033[2m'; C_OFF=$'\033[0m'
else
  C_RED=""; C_GREEN=""; C_YEL=""; C_BLUE=""; C_DIM=""; C_OFF=""
fi

# --- Counters ---
PASS=0; FAIL=0; SKIP=0
FAILURES=()

log()   { printf "%s\n" "$*" >&2; }
info()  { printf "${C_BLUE}[INFO]${C_OFF} %s\n" "$*" >&2; }
warn()  { printf "${C_YEL}[WARN]${C_OFF} %s\n" "$*" >&2; }
err()   { printf "${C_RED}[ERR ]${C_OFF} %s\n" "$*" >&2; }
ok()    { printf "${C_GREEN}[ OK ]${C_OFF} %s\n" "$*" >&2; }

# --- Dep check ---
need() {
  for cmd in "$@"; do
    command -v "$cmd" >/dev/null 2>&1 || { err "missing dependency: $cmd"; exit 127; }
  done
}
need curl jq

# --- req: gọi API, in HTTP code ra stderr, body ra stdout ---
# Usage: req METHOD PATH [json_body] [extra_curl_args...]
# Trả về body. HTTP code lưu vào biến global HTTP_CODE.
HTTP_CODE=0
LAST_BODY=""
req() {
  local method="$1"; local path="$2"; local body="${3:-}"
  shift; shift; [[ $# -gt 0 ]] && shift || true
  local url="$BASE_URL$path"
  local tmp; tmp="$(mktemp)"
  local args=(-sS -o "$tmp" -w '%{http_code}' -X "$method" -H 'Accept: application/json')
  if [[ -n "$body" ]]; then
    args+=(-H 'Content-Type: application/json' -d "$body")
  fi
  if [[ -n "${ACCESS_TOKEN:-}" ]]; then
    args+=(-H "Authorization: Bearer $ACCESS_TOKEN")
  fi
  args+=("$@")
  HTTP_CODE=$(curl "${args[@]}" "$url" || echo "000")
  LAST_BODY="$(cat "$tmp")"
  rm -f "$tmp"
  printf "%s" "$LAST_BODY"
}

# --- expect: assert HTTP code + optional jq filter on body ---
# Usage: expect <test_name> <expected_status> [jq_check_expr]
# Phải gọi NGAY sau req. jq_check_expr trả true thì pass.
expect() {
  local name="$1"; local want="$2"; local jq_expr="${3:-}"
  if [[ "$HTTP_CODE" != "$want" ]]; then
    FAIL=$((FAIL+1))
    FAILURES+=("$name :: want $want got $HTTP_CODE :: body=$(echo "$LAST_BODY" | head -c 200)")
    err "$name :: expected $want, got $HTTP_CODE"
    return 1
  fi
  if [[ -n "$jq_expr" ]]; then
    if ! echo "$LAST_BODY" | jq -e "$jq_expr" >/dev/null 2>&1; then
      FAIL=$((FAIL+1))
      FAILURES+=("$name :: jq filter failed: $jq_expr :: body=$(echo "$LAST_BODY" | head -c 200)")
      err "$name :: jq check failed: $jq_expr"
      return 1
    fi
  fi
  PASS=$((PASS+1))
  ok "$name"
  return 0
}

skip() { SKIP=$((SKIP+1)); warn "SKIP $*"; }

# --- Token management ---
save_token() {
  local role="$1"; local access="$2"; local refresh="${3:-}"
  printf "%s\n%s\n" "$access" "$refresh" > "$TOKEN_DIR/$role.tok"
}
load_token() {
  local role="$1"
  local f="$TOKEN_DIR/$role.tok"
  [[ -f "$f" ]] || return 1
  ACCESS_TOKEN="$(sed -n 1p "$f")"
  REFRESH_TOKEN="$(sed -n 2p "$f")"
  export ACCESS_TOKEN REFRESH_TOKEN
}
clear_token() { unset ACCESS_TOKEN REFRESH_TOKEN; }

login_as() {
  local email="$1"; local password="$2"; local role_label="${3:-user}"
  clear_token
  req POST /auth/login "{\"email\":\"$email\",\"password\":\"$password\"}"
  if [[ "$HTTP_CODE" != "200" ]]; then
    err "login_as $role_label failed: HTTP $HTTP_CODE — $LAST_BODY"
    return 1
  fi
  local at; at=$(echo "$LAST_BODY" | jq -r '.data.accessToken')
  local rt; rt=$(echo "$LAST_BODY" | jq -r '.data.refreshToken')
  save_token "$role_label" "$at" "$rt"
  ACCESS_TOKEN="$at"; REFRESH_TOKEN="$rt"
  export ACCESS_TOKEN REFRESH_TOKEN
  info "logged in as $role_label ($email)"
}

# --- Summary ---
summary() {
  local total=$((PASS+FAIL+SKIP))
  echo
  echo "================================================================"
  printf "Total: %d   ${C_GREEN}Pass: %d${C_OFF}   ${C_RED}Fail: %d${C_OFF}   ${C_YEL}Skip: %d${C_OFF}\n" \
    "$total" "$PASS" "$FAIL" "$SKIP"
  if [[ $FAIL -gt 0 ]]; then
    echo "Failures:"
    for f in "${FAILURES[@]}"; do echo "  - $f"; done
  fi
  echo "================================================================"
  [[ $FAIL -eq 0 ]]
}
