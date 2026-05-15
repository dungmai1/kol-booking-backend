#!/usr/bin/env bash
# E2E smoke test runner — gọi tuần tự các script domain.
# Stop ở domain đầu tiên fail (set -e), trừ phi truyền --continue.
# Run: bash scripts/smoke.sh [--continue]
source "$(dirname "$0")/_common.sh"

CONTINUE_ON_FAIL=0
[[ "${1:-}" == "--continue" ]] && CONTINUE_ON_FAIL=1

STAMP=$(date +%Y%m%d-%H%M%S)
LOG="$REPORTS_DIR/smoke-$STAMP.log"
mkdir -p "$REPORTS_DIR"

SCRIPTS=(
  categories.sh
  auth.sh
  kols.sh
  brands.sh
  bookings.sh
  payments.sh
  reviews.sh
  notifications.sh
  files.sh
  admin.sh
)

GLOBAL_PASS=0; GLOBAL_FAIL=0; GLOBAL_SKIP=0
FAILED_SCRIPTS=()

# 0. Health check
info "── health check"
HC=$(curl -sS -o /dev/null -w '%{http_code}' "http://localhost:8080/actuator/health" || echo "000")
if [[ "$HC" != "200" ]]; then
  err "backend không UP (actuator/health=$HC). Chạy ./gradlew bootRun trước."
  exit 2
fi
ok "backend UP"

# Chạy từng script và parse summary
for s in "${SCRIPTS[@]}"; do
  echo
  echo "════════════════════════════════════════════════════════════════"
  echo "  ▶  $s"
  echo "════════════════════════════════════════════════════════════════"
  TMPLOG="$REPORTS_DIR/.tmp-$s.log"
  if bash "$SCRIPT_DIR/$s" 2>&1 | tee "$TMPLOG"; then
    EXIT=0
  else
    EXIT=$?
  fi
  cat "$TMPLOG" >> "$LOG"

  # Parse "Total: X Pass: Y Fail: Z Skip: W" từ output (ANSI-stripped)
  SUMMARY=$(grep -aE "^Total: " "$TMPLOG" | sed -E 's/\x1B\[[0-9;]*[a-zA-Z]//g' | tail -1)
  if [[ -n "$SUMMARY" ]]; then
    P=$(echo "$SUMMARY" | sed -E 's/.*Pass: ([0-9]+).*/\1/')
    F=$(echo "$SUMMARY" | sed -E 's/.*Fail: ([0-9]+).*/\1/')
    K=$(echo "$SUMMARY" | sed -E 's/.*Skip: ([0-9]+).*/\1/')
    GLOBAL_PASS=$((GLOBAL_PASS + P))
    GLOBAL_FAIL=$((GLOBAL_FAIL + F))
    GLOBAL_SKIP=$((GLOBAL_SKIP + K))
  fi
  if [[ "$EXIT" != "0" ]]; then
    FAILED_SCRIPTS+=("$s")
    if [[ "$CONTINUE_ON_FAIL" != "1" ]]; then
      err "$s failed — stopping (rerun with --continue để chạy tiếp)"
      break
    fi
  fi
  rm -f "$TMPLOG"
done

echo
echo "════════════════════════════════════════════════════════════════"
echo "  E2E SMOKE SUMMARY"
echo "════════════════════════════════════════════════════════════════"
printf "Pass: %d   Fail: %d   Skip: %d\n" "$GLOBAL_PASS" "$GLOBAL_FAIL" "$GLOBAL_SKIP"
if [[ ${#FAILED_SCRIPTS[@]} -gt 0 ]]; then
  echo "Failed scripts:"
  for s in "${FAILED_SCRIPTS[@]}"; do echo "  - $s"; done
fi
echo "Log: $LOG"

# Generate markdown report
REPORT="$REPORTS_DIR/smoke-$STAMP.md"
{
  echo "# Smoke Test Report — $STAMP"
  echo
  echo "**Backend**: $BASE_URL"
  echo "**Build**: $(git -C "$ROOT_DIR" rev-parse --short HEAD 2>/dev/null || echo unknown)"
  echo "**Branch**: $(git -C "$ROOT_DIR" branch --show-current 2>/dev/null || echo unknown)"
  echo
  echo "| Pass | Fail | Skip |"
  echo "|---:|---:|---:|"
  echo "| $GLOBAL_PASS | $GLOBAL_FAIL | $GLOBAL_SKIP |"
  echo
  if [[ ${#FAILED_SCRIPTS[@]} -gt 0 ]]; then
    echo "## Failed scripts"
    for s in "${FAILED_SCRIPTS[@]}"; do echo "- $s"; done
    echo
  fi
  echo "Full log: \`$LOG\`"
} > "$REPORT"
info "Markdown report: $REPORT"

[[ $GLOBAL_FAIL -eq 0 && ${#FAILED_SCRIPTS[@]} -eq 0 ]]
