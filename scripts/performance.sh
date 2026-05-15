#!/usr/bin/env bash
# Basic perf measurement bằng curl. Dùng cho smoke perf — không phải stress test.
# Stress test: dùng ab/wrk/k6 — xem scripts/README.md mục 6.
# Run: bash scripts/performance.sh
source "$(dirname "$0")/_common.sh"

N="${PERF_N:-50}"
OUT="$REPORTS_DIR/perf-$(date +%Y%m%d-%H%M%S).csv"
echo "endpoint,i,http,ttfb_ms,total_ms" > "$OUT"

# Danh sách endpoint perf-sample (public + authenticated).
# Authenticated endpoints sẽ tự login seed KOL để có token.
ENDPOINTS_PUBLIC=(
  "/categories"
  "/kols/search?size=20"
  "/kols/search?platforms=TIKTOK&size=20"
  "/kols/search?minFollower=500000&size=20"
  "/kols/search?q=ngoc&size=20"
  "/kols/featured?limit=10"
  "/kols/lam-bao-ngoc"
)
ENDPOINTS_AUTH=(
  "/kols/me"
  "/wallet/me"
  "/notifications/me?page=0&size=20"
  "/notifications/me/unread-count"
)
ENDPOINTS=("${ENDPOINTS_PUBLIC[@]}" "${ENDPOINTS_AUTH[@]}")

# Login để có ACCESS_TOKEN cho endpoint authenticated
login_as "lambaongoc@seed.local" "password123" perf >/dev/null 2>&1 || warn "seed login failed — auth endpoints sẽ trả 401"
PERF_TOKEN="${ACCESS_TOKEN:-}"

measure_one() {
  local path="$1"; local i="$2"
  local auth_arg=()
  if [[ "$path" =~ ^(/wallet|/notifications)|/me$|/me\?|/me/ ]] && [[ -n "$PERF_TOKEN" ]]; then
    auth_arg=(-H "Authorization: Bearer $PERF_TOKEN")
  fi
  # curl -w format KHÔNG chứa path (tránh Git Bash MSYS dịch /...). Path ghép sau.
  local out
  out=$(curl -sS -o /dev/null "${auth_arg[@]}" \
        -w '%{http_code},%{time_starttransfer},%{time_total}' \
        "$BASE_URL$path")
  printf '%s,%s,%s\n' "$path" "$i" "$out"
}

stats() {
  local label="$1"
  # $5 trong CSV đã là ms (đã *1000 ở pipe ghi). Không nhân lần nữa.
  awk -F',' -v lbl="$label" '
    NR>1 && $1==lbl {a[++n]=$5+0}
    END {
      if (n==0) { print lbl, "no data"; exit }
      asort(a)
      sum=0; for(i=1;i<=n;i++) sum+=a[i]
      printf "%-50s n=%4d  avg=%6.1fms  p50=%5.0fms  p95=%5.0fms  p99=%5.0fms  max=%5.0fms\n",
        lbl, n, sum/n, a[int(n*0.50)], a[int(n*0.95)], a[int(n*0.99)], a[n]
    }
  ' "$OUT"
}

info "Warming up (3 req mỗi endpoint)..."
for ep in "${ENDPOINTS[@]}"; do
  for i in 1 2 3; do curl -sS -o /dev/null "$BASE_URL$ep"; done
done

info "Đo $N request mỗi endpoint → $OUT"
for ep in "${ENDPOINTS[@]}"; do
  for i in $(seq 1 "$N"); do
    measure_one "$ep" "$i" \
      | awk -F',' '{printf "%s,%s,%s,%.0f,%.0f\n", $1, $2, $3, $4*1000, $5*1000}' \
      >> "$OUT"
  done
  printf "."
done
echo

echo
echo "=== SUMMARY (ms) ==="
for ep in "${ENDPOINTS[@]}"; do
  stats "$ep"
done

echo
info "Raw CSV: $OUT"
info "Để stress test sâu hơn, dùng:"
echo "  ab -n 1000 -c 20 -H 'Accept: application/json' '$BASE_URL/kols/search?size=20'"
echo "  wrk -t4 -c50 -d30s --latency '$BASE_URL/kols/search?size=20'"
echo "  k6 run scripts/k6/search.js   # nếu có"
