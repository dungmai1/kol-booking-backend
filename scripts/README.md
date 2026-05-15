# KOL Booking Backend — API Test Plan (curl)

Kế hoạch test toàn bộ REST API hiện có của hệ thống bằng `curl`, phục vụ cả functional test lẫn performance test cơ bản. Toàn bộ script chạy được trên **Git Bash (Windows)**, **WSL**, **macOS**, **Linux**. Trên PowerShell thuần xem mục [PowerShell alternative](#powershell-alternative).

> **Base URL thực tế**: `http://localhost:8080/api/v1` (không phải `/api`). Mọi controller đều dưới prefix này — xem `SecurityConfig` để biết path permitAll.

---

## 0. Setup môi trường test

### 0.1. Yêu cầu công cụ

| Tool | Mục đích | Kiểm tra |
|---|---|---|
| `curl` | Gọi API | `curl --version` |
| `jq` | Parse JSON response | `jq --version` (Git Bash: `pacman -S jq` hoặc download binary) |
| `bash` 4+ | Chạy script | Git Bash / WSL |
| (optional) `ab` | Apache Bench performance | `ab -V` |
| (optional) `wrk` | Modern perf tool | `wrk --version` |
| (optional) `k6` | Scripted load test | `k6 version` |

### 0.2. Khởi động backend

```bash
# Postgres remote (Supabase) phải sẵn sàng — env SPRING_DATASOURCE_* lấy từ .env launcher
./gradlew bootRun
```

Verify alive:

```bash
curl -fsS http://localhost:8080/actuator/health
# Expect 200 — {"status":"UP"}
```

### 0.3. Tạo file env riêng cho test

```bash
# scripts/.env.test  (gitignored — KHÔNG commit)
BASE_URL=http://localhost:8080/api/v1
KOL_EMAIL=qa-kol-$(date +%s)@test.local
KOL_PASSWORD=Passw0rd!
BRAND_EMAIL=qa-brand-$(date +%s)@test.local
BRAND_PASSWORD=Passw0rd!
ADMIN_EMAIL=qa-admin@test.local
ADMIN_PASSWORD=Passw0rd!
```

> Seed sẵn 8 KOL có email `*@seed.local`, password `password123` (xem `V8__seed_data.sql`). **Chưa có ADMIN seed sẵn** — phải tự register rồi UPDATE bảng `app_user` set `role='ADMIN'` (xem `auth.sh::bootstrap_admin`).

---

## 1. Tổ chức file test

```
scripts/
├── README.md           # File này — plan + checklist + report format
├── _common.sh          # Helpers chung: curl wrapper, token cache, jq utils
├── auth.sh             # Register / login / refresh / logout / verify-email
├── kols.sh             # KOL profile (private) + search/featured/public (public)
├── brands.sh           # Brand profile + favorites
├── bookings.sh         # Full booking lifecycle (create → accept → deliver → approve)
├── payments.sh         # Checkout + webhook + wallet + withdraw
├── reviews.sh          # Review CRUD + public reviews
├── notifications.sh    # Notification list / unread / mark-read
├── admin.sh            # Admin approve/reject, user ban, category CRUD, stats, audit log
├── categories.sh       # Public categories GET
├── files.sh            # File upload multipart
├── performance.sh      # Perf measurement (curl -w, loop, ab, wrk, k6)
├── smoke.sh            # E2E happy-path runner gọi tuần tự các script trên
└── reports/            # Output logs (gitignored)
    ├── functional-YYYY-MM-DD.log
    └── perf-YYYY-MM-DD.csv
```

Mỗi script đều `source _common.sh` ở đầu để dùng chung biến `BASE_URL`, token cache, helper `req`.

---

## 2. Auth model & role matrix

| Role | Endpoint mẫu cho phép | Endpoint cấm |
|---|---|---|
| **Anonymous** | `/auth/**`, `/categories` (GET), `/kols/search`, `/kols/featured`, `/kols/{slug}`, `/users/{id}/reviews` | tất cả `/me`, `/bookings`, `/admin/**` |
| **KOL** | `/kols/me/**`, `/bookings/incoming`, `/bookings/{id}/accept`, `/withdraws/**` | `/brands/me`, `/admin/**`, `/bookings` (POST) |
| **BRAND** | `/brands/me/**`, `/bookings` (POST), `/bookings/me`, `/payments/bookings/*/checkout` | `/kols/me`, `/admin/**` |
| **ADMIN** | `/admin/**`, `/withdraws/admin/**` | n/a (full access) |

**Token lifecycle**:
- Access token TTL: **900s (15 phút)** — `app.jwt.access-token-ttl-seconds`
- Refresh token TTL: **604800s (7 ngày)** — `app.jwt.refresh-token-ttl-seconds`
- Header: `Authorization: Bearer <accessToken>`

**Response envelope** (mọi endpoint — `ApiResponse<T>`):
```json
{ "success": true, "data": { ... }, "message": null, "errorCode": null }
```
Lỗi:
```json
{ "success": false, "data": null, "message": "Validation failed", "errorCode": "VALIDATION_FAILED" }
```

**Error codes hay gặp**: `VALIDATION_FAILED`, `UNAUTHORIZED`, `FORBIDDEN`, `INTERNAL_ERROR`, `BUSINESS_ERROR` (kèm message domain).

---

## 3. Curl templates — copy & paste

### 3.1. GET public

```bash
curl -sS -X GET "$BASE_URL/categories" \
  -H 'Accept: application/json'
```

### 3.2. GET private (cần token)

```bash
curl -sS -X GET "$BASE_URL/kols/me" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H 'Accept: application/json'
```

### 3.3. POST JSON body

```bash
curl -sS -X POST "$BASE_URL/auth/login" \
  -H 'Content-Type: application/json' \
  -H 'Accept: application/json' \
  -d '{
    "email": "thanhnha25091@seed.local",
    "password": "password123"
  }'
```

### 3.4. PUT cập nhật

```bash
curl -sS -X PUT "$BASE_URL/kols/me" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "displayName": "QA KOL Updated",
    "slug": "qa-kol-updated",
    "city": "Ho Chi Minh",
    "country": "VN",
    "categoryIds": [1, 2]
  }'
```

### 3.5. PATCH

```bash
curl -sS -X PATCH "$BASE_URL/notifications/$NOTI_ID/read" \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

### 3.6. DELETE

```bash
curl -sS -X DELETE "$BASE_URL/kols/me/channels/$CHANNEL_ID" \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

### 3.7. Multipart upload

```bash
curl -sS -X POST "$BASE_URL/files/upload" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -F "file=@./fixtures/sample.jpg"
```

### 3.8. Pagination + query params

```bash
curl -sS -G "$BASE_URL/kols/search" \
  --data-urlencode "q=ngoc" \
  --data-urlencode "platforms=TIKTOK" \
  --data-urlencode "minFollower=100000" \
  --data-urlencode "page=0" \
  --data-urlencode "size=20" \
  --data-urlencode "sort=featured"
```

> Dùng `-G --data-urlencode` để curl tự build query string an toàn (escape ký tự đặc biệt, hỗ trợ unicode VN).

### 3.9. In status code + body riêng

```bash
curl -sS -w "\nHTTP_STATUS:%{http_code}\n" -o /tmp/body.json "$BASE_URL/categories"
cat /tmp/body.json | jq .
```

---

## 4. Checklist test toàn bộ API

Đánh dấu `[x]` khi đã pass cả happy path lẫn negative.

### 4.1. Auth (`auth.sh`)
- [ ] `POST /auth/register` — happy path (KOL, BRAND)
- [ ] `POST /auth/register` — duplicate email → 409 / errorCode business
- [ ] `POST /auth/register` — email sai format → 400 `VALIDATION_FAILED`
- [ ] `POST /auth/register` — password < 8 ký tự → 400
- [ ] `POST /auth/register` — role không hợp lệ (`SUPER`) → 400
- [ ] `POST /auth/login` — happy path
- [ ] `POST /auth/login` — sai password → 401 `UNAUTHORIZED`
- [ ] `POST /auth/login` — email không tồn tại → 401
- [ ] `POST /auth/login` — user bị BANNED → 401/403
- [ ] `POST /auth/refresh` — refresh hợp lệ → token mới
- [ ] `POST /auth/refresh` — refresh sai/expired → 401
- [ ] `POST /auth/logout` — happy path → 200
- [ ] `POST /auth/logout` — refresh đã revoke → vẫn 200 (idempotent) hoặc 401 tùy implementation
- [ ] `POST /auth/verify-email` — token sai → 400
- [ ] `POST /auth/forgot-password` — email tồn tại → 200 (không leak existence)
- [ ] `POST /auth/forgot-password` — email không tồn tại → 200 (cùng response — anti-enumeration)
- [ ] `POST /auth/reset-password` — token hết hạn → 400

### 4.2. KOL Profile — Private (`kols.sh`)
- [ ] `GET /kols/me` — thiếu token → 401
- [ ] `GET /kols/me` — token BRAND → 403
- [ ] `GET /kols/me` — token KOL → 200, profile có status `DRAFT`
- [ ] `PUT /kols/me` — happy path
- [ ] `PUT /kols/me` — slug sai pattern (chứa `_` hoặc chữ hoa) → 400
- [ ] `PUT /kols/me` — slug trùng KOL khác → 409
- [ ] `PUT /kols/me` — displayName > 150 chars → 400
- [ ] `POST /kols/me/channels` — happy path
- [ ] `POST /kols/me/channels` — `followerCount` âm → 400
- [ ] `POST /kols/me/channels` — `engagementRate` > 100 → 400
- [ ] `POST /kols/me/channels` — platform không tồn tại → 400
- [ ] `DELETE /kols/me/channels/{id}` — happy path → 204
- [ ] `DELETE /kols/me/channels/{id}` — ID không thuộc về KOL → 403/404
- [ ] `DELETE /kols/me/channels/{id}` — ID không tồn tại → 404
- [ ] `POST /kols/me/packages` — happy path
- [ ] `POST /kols/me/packages` — `price` âm → 400
- [ ] `DELETE /kols/me/packages/{id}` — như channel
- [ ] `POST /kols/me/portfolio` — happy path
- [ ] `POST /kols/me/portfolio` — `mediaUrl` empty → 400
- [ ] `DELETE /kols/me/portfolio/{id}`
- [ ] `POST /kols/me/submit` — DRAFT → status PENDING_REVIEW
- [ ] `POST /kols/me/submit` — submit lần 2 khi đang PENDING → 400 business

### 4.3. KOL Search — Public (`kols.sh`)
- [ ] `GET /kols/search` — không filter → 200, page mặc định 0, size 20
- [ ] `GET /kols/search?q=ngoc` — text search
- [ ] `GET /kols/search?platforms=TIKTOK,INSTAGRAM`
- [ ] `GET /kols/search?minFollower=500000`
- [ ] `GET /kols/search?minPrice=1000000&maxPrice=10000000`
- [ ] `GET /kols/search?city=Ha+Noi`
- [ ] `GET /kols/search?gender=FEMALE`
- [ ] `GET /kols/search?minRating=4`
- [ ] `GET /kols/search?categoryIds=1&categoryIds=2` (multi)
- [ ] `GET /kols/search?sort=featured` (default)
- [ ] `GET /kols/search?page=-1` → 400 hoặc 200 với clamp (verify behavior)
- [ ] `GET /kols/search?size=10000` → cap về max (default Spring max), verify
- [ ] `GET /kols/search?page=999` — empty `content: []`, totalElements > 0
- [ ] `GET /kols/search?gender=INVALID_ENUM` → 400
- [ ] `GET /kols/featured?limit=5` → ≤5 items
- [ ] `GET /kols/{slug}` — slug seed `lam-bao-ngoc` → 200
- [ ] `GET /kols/{slug}` — slug không tồn tại → 404
- [ ] `GET /kols/{slug}` — slug chứa ký tự bậy `<script>` → 404 (SecurityConfig pattern `[a-z0-9-]+` chặn)

### 4.4. Brand (`brands.sh`)
- [ ] `GET /brands/me` — token BRAND → 200
- [ ] `GET /brands/me` — token KOL → 403
- [ ] `PUT /brands/me` — happy path
- [ ] `PUT /brands/me` — `contactPhone` sai pattern (`abc`) → 400
- [ ] `PUT /brands/me` — `companyName` > 200 chars → 400
- [ ] `POST /brands/me/submit` — DRAFT → PENDING_REVIEW
- [ ] `POST /brands/me/favorites/{kolId}` — happy path
- [ ] `POST /brands/me/favorites/{kolId}` — add lần 2 → 200 idempotent / hoặc 409 (verify)
- [ ] `POST /brands/me/favorites/{kolId}` — kolId không tồn tại → 404
- [ ] `DELETE /brands/me/favorites/{kolId}` — happy
- [ ] `DELETE /brands/me/favorites/{kolId}` — chưa favorite → 404 / 204 (verify)
- [ ] `GET /brands/me/favorites?page=0&size=10`

### 4.5. Booking (`bookings.sh`)
- [ ] `POST /bookings` — BRAND, happy path → status `PENDING`
- [ ] `POST /bookings` — token KOL → 403
- [ ] `POST /bookings` — `budget` âm → 400
- [ ] `POST /bookings` — `campaignTitle` empty → 400
- [ ] `POST /bookings` — `kolProfileId` không tồn tại → 404
- [ ] `POST /bookings` — `endDate` trước `startDate` → 400 business (nếu service có check)
- [ ] `GET /bookings/me` — BRAND list, pagination
- [ ] `GET /bookings/incoming` — KOL list
- [ ] `GET /bookings/{id}` — bên thứ 3 không thuộc booking → 403/404
- [ ] `POST /bookings/{id}/accept` — KOL owner → status CONFIRMED
- [ ] `POST /bookings/{id}/accept` — KOL khác → 403
- [ ] `POST /bookings/{id}/accept` — khi đã CONFIRMED → 400 business state
- [ ] `POST /bookings/{id}/reject` — KOL, kèm `reason`
- [ ] `POST /bookings/{id}/cancel` — BRAND owner
- [ ] `POST /bookings/{id}/cancel` — sau khi đã COMPLETED → 400 business
- [ ] `POST /bookings/{id}/deliverables` — KOL submit
- [ ] `POST /bookings/{id}/deliverables` — chưa được PAID → 400 business
- [ ] `POST /bookings/{id}/approve-delivery` — BRAND
- [ ] `POST /bookings/{id}/dispute` — BRAND
- [ ] `POST /bookings/{id}/messages` — content empty → 400
- [ ] `POST /bookings/{id}/messages` — content > 4000 chars → 400
- [ ] `GET /bookings/{id}/messages?page=0&size=50`

### 4.6. Payment / Wallet / Withdraw (`payments.sh`)
- [ ] `POST /payments/bookings/{bookingId}/checkout` — BRAND, CONFIRMED booking → 200, có `externalRef`
- [ ] `POST /payments/bookings/{bookingId}/checkout` — booking PENDING → 400 business
- [ ] `POST /payments/bookings/{bookingId}/checkout` — không phải owner → 403
- [ ] `GET /payments/bookings/{bookingId}` — get status
- [ ] `GET /payments/webhook/{provider}?externalRef=X&amount=Y&status=SUCCESS` — mock webhook → 200, booking chuyển PAID
- [ ] `GET /payments/webhook/{provider}` — `externalRef` không tồn tại → 200 (webhook không expose business)
- [ ] `GET /wallet/me` — bất kỳ user → balance
- [ ] `GET /wallet/me` — anonymous → 401
- [ ] `GET /wallet/me/transactions?page=0&size=20`
- [ ] `POST /withdraws` — KOL, amount = 1 → 200
- [ ] `POST /withdraws` — KOL, amount = 0 → 400
- [ ] `POST /withdraws` — `bankAccount` empty → 400
- [ ] `POST /withdraws` — vượt số dư wallet → 400 business
- [ ] `GET /withdraws/me?page=0&size=20`
- [ ] `GET /withdraws/admin?status=PENDING` — ADMIN
- [ ] `GET /withdraws/admin` — KOL → 403
- [ ] `POST /withdraws/admin/{id}/approve` — ADMIN
- [ ] `POST /withdraws/admin/{id}/paid` — ADMIN
- [ ] `POST /withdraws/admin/{id}/reject` — ADMIN, reason
- [ ] State machine: PENDING → APPROVED → PAID (đi đúng thứ tự), reject ở mọi step

### 4.7. Review (`reviews.sh`)
- [ ] `POST /bookings/{id}/reviews` — BRAND review KOL sau COMPLETED → 200
- [ ] `POST /bookings/{id}/reviews` — KOL review BRAND sau COMPLETED → 200
- [ ] `POST /bookings/{id}/reviews` — booking chưa COMPLETED → 400 business
- [ ] `POST /bookings/{id}/reviews` — review lần 2 → 409 business
- [ ] `POST /bookings/{id}/reviews` — `rating=0` → 400
- [ ] `POST /bookings/{id}/reviews` — `rating=6` → 400
- [ ] `POST /bookings/{id}/reviews` — `comment` > 4000 → 400
- [ ] `PUT /reviews/{reviewId}` — owner → 200
- [ ] `PUT /reviews/{reviewId}` — không phải owner → 403
- [ ] `GET /users/{userId}/reviews` — anonymous → 200 (public)
- [ ] `GET /users/{userId}/reviews?page=0&size=20`

### 4.8. Notification (`notifications.sh`)
- [ ] `GET /notifications/me` — paginated
- [ ] `GET /notifications/me?unreadOnly=true`
- [ ] `GET /notifications/me/unread-count` → `{ "count": N }`
- [ ] `PATCH /notifications/{id}/read` — owner → 200
- [ ] `PATCH /notifications/{id}/read` — notification của user khác → 403/404
- [ ] `POST /notifications/me/read-all` → `{ "updated": N }`
- [ ] Anonymous gọi bất kỳ → 401

### 4.9. Category (`categories.sh`)
- [ ] `GET /categories` — public, trả tree
- [ ] Verify children được nest đúng (parent_id)

### 4.10. File Upload (`files.sh`)
- [ ] `POST /files/upload` — image jpg < 20MB → 200, có `url`
- [ ] `POST /files/upload` — không gửi `file` → 400
- [ ] `POST /files/upload` — file > 20MB → 413 Payload Too Large
- [ ] `POST /files/upload` — content-type không phải multipart → 415/400
- [ ] Verify file truy cập được tại `http://localhost:8080/uploads/<url>`

### 4.11. Admin (`admin.sh`)
- [ ] Tất cả endpoint dưới `/admin/**` — non-ADMIN → 403
- [ ] `GET /admin/kols?status=PENDING_REVIEW`
- [ ] `POST /admin/kols/{id}/approve` → KOL nhận notification, status APPROVED
- [ ] `POST /admin/kols/{id}/reject` — kèm reason
- [ ] `POST /admin/kols/{id}/approve` — đã APPROVED → 400 business
- [ ] `GET /admin/brands?status=PENDING_REVIEW`
- [ ] `POST /admin/brands/{id}/approve`
- [ ] `POST /admin/brands/{id}/reject`
- [ ] `GET /admin/users?q=ngoc&role=KOL`
- [ ] `POST /admin/users/{id}/ban` — user không login được nữa
- [ ] `POST /admin/users/{id}/unban`
- [ ] `POST /admin/categories` — happy
- [ ] `POST /admin/categories` — slug trùng → 409
- [ ] `PUT /admin/categories/{id}`
- [ ] `DELETE /admin/categories/{id}` — có child → 400 business
- [ ] `DELETE /admin/categories/{id}` — đang được KOL dùng → 400 business
- [ ] `GET /admin/stats/overview`
- [ ] `GET /admin/stats/bookings?from=2026-01-01T00:00:00&to=2026-05-15T23:59:59`
- [ ] `GET /admin/stats/top-kols?limit=5`
- [ ] `GET /admin/stats/revenue`
- [ ] `GET /admin/audit-logs?page=0&size=50`

### 4.12. Cross-cutting security
- [ ] Mỗi endpoint private → test thiếu header `Authorization` → **401**
- [ ] Mỗi endpoint private → test token sai (`Bearer xxx`) → **401**
- [ ] Mỗi endpoint private → test access token đã expire (đợi >900s hoặc dùng jwt giả expire) → **401**
- [ ] Mỗi endpoint role-restricted → test wrong role → **403**
- [ ] CORS: `Origin: http://evil.com` → header `Access-Control-Allow-Origin` **không** echo back
- [ ] Path traversal: `/uploads/../application.properties` → **403/404**
- [ ] SQL injection: query param `q=' OR 1=1--` → **400/200 sạch**, không 500

---

## 5. Performance test cơ bản — curl

### 5.1. Đo timing chi tiết 1 request

```bash
curl -sS -o /dev/null \
  -w "lookup=%{time_namelookup}s connect=%{time_connect}s ttfb=%{time_starttransfer}s total=%{time_total}s status=%{http_code}\n" \
  "$BASE_URL/kols/search?size=20"
```

Output mẫu:
```
lookup=0.000s connect=0.001s ttfb=0.045s total=0.052s status=200
```

Ý nghĩa:
- `time_namelookup` — DNS (≈0 với localhost)
- `time_connect` — TCP handshake
- `time_starttransfer` (TTFB) — backend xử lý xong byte đầu (chỉ số quan trọng nhất)
- `time_total` — toàn bộ bao gồm download body

### 5.2. Format curl-format.txt tái sử dụng

```
# scripts/curl-format.txt
namelookup:  %{time_namelookup}\n
connect:     %{time_connect}\n
appconnect:  %{time_appconnect}\n
pretransfer: %{time_pretransfer}\n
redirect:    %{time_redirect}\n
starttransfer: %{time_starttransfer}\n
----------\n
total:       %{time_total}\n
size_down:   %{size_download}\n
http_code:   %{http_code}\n
```

```bash
curl -sS -o /dev/null -w "@scripts/curl-format.txt" "$BASE_URL/kols/search"
```

### 5.3. Loop N request, ghi CSV

```bash
# scripts/performance.sh::measure
N=100
URL="$BASE_URL/kols/search?size=20"
OUT="scripts/reports/perf-$(date +%Y%m%d-%H%M%S).csv"
mkdir -p scripts/reports
echo "i,http,ttfb_ms,total_ms" > "$OUT"
for i in $(seq 1 $N); do
  curl -sS -o /dev/null \
    -w "$i,%{http_code},%{time_starttransfer},%{time_total}\n" "$URL" \
    | awk -F',' '{printf "%s,%s,%.0f,%.0f\n", $1, $2, $3*1000, $4*1000}' \
    >> "$OUT"
done
echo "→ $OUT"
```

### 5.4. Tính p50/p95/p99/avg

```bash
# nhanh, không cần Python
awk -F',' 'NR>1 {a[NR-1]=$4} END {
  n=NR-1; asort(a);
  sum=0; for(i=1;i<=n;i++) sum+=a[i];
  printf "n=%d avg=%.0fms p50=%.0fms p95=%.0fms p99=%.0fms max=%.0fms\n",
    n, sum/n, a[int(n*0.50)], a[int(n*0.95)], a[int(n*0.99)], a[n]
}' "$OUT"
```

Output mẫu:
```
n=100 avg=48ms p50=45ms p95=82ms p99=120ms max=140ms
```

### 5.5. PowerShell alternative

```powershell
# scripts/perf.ps1
$BaseUrl = "http://localhost:8080/api/v1"
$N = 100
$results = 1..$N | ForEach-Object {
    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    $resp = Invoke-WebRequest -Uri "$BaseUrl/kols/search?size=20" -UseBasicParsing
    $sw.Stop()
    [pscustomobject]@{ i = $_; status = $resp.StatusCode; ms = $sw.ElapsedMilliseconds }
}
$results | Export-Csv scripts\reports\perf-ps.csv -NoTypeInformation
$ms = $results.ms | Sort-Object
"avg={0:N0} p50={1} p95={2} p99={3}" -f ($ms | Measure-Object -Average).Average, $ms[[int]($N*0.5)], $ms[[int]($N*0.95)], $ms[[int]($N*0.99)]
```

---

## 6. Performance test nâng cao

### 6.1. Apache Bench (đơn giản nhất)

```bash
# 1000 request, concurrency 20
ab -n 1000 -c 20 -H "Accept: application/json" "$BASE_URL/kols/search?size=20"
```

Đọc output: `Requests per second`, `Time per request`, `Percentage of the requests served within a certain time (ms)`.

Với endpoint cần token:
```bash
ab -n 1000 -c 20 -H "Authorization: Bearer $ACCESS_TOKEN" "$BASE_URL/wallet/me"
```

### 6.2. wrk (chính xác hơn, dùng cho hot path)

```bash
wrk -t4 -c50 -d30s --latency "$BASE_URL/kols/search?size=20"
```

`-t` threads, `-c` connections, `-d` duration. Cờ `--latency` cho phân phối p50/p90/p99.

### 6.3. k6 (kịch bản phức tạp, scenario)

```javascript
// scripts/k6/search.js
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '30s', target: 20 },
    { duration: '1m', target: 50 },
    { duration: '30s', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<200', 'p(99)<500'],
    http_req_failed: ['rate<0.01'],
  },
};

export default function () {
  const r = http.get('http://localhost:8080/api/v1/kols/search?size=20');
  check(r, { 'status 200': (x) => x.status === 200 });
  sleep(1);
}
```

```bash
k6 run scripts/k6/search.js
```

### 6.4. Khi nào dùng gì

| Mục tiêu | Tool |
|---|---|
| Đo 1 request có nhanh không | `curl -w` |
| Smoke perf 1 endpoint | `ab` |
| Đo p99 ổn định, hot path | `wrk` |
| Login + browse + book (multi-step scenario) | `k6` |
| Pre-prod stress, ramp-up | `k6` hoặc JMeter |

> Với hệ thống hiện tại dùng **Supabase pooler 15-client cap**, chú ý: chạy `wrk -c 50` từ máy local sẽ ăn hết slot pool → gây lỗi Hikari `Connection is not available`. Test perf nên chạy lên môi trường staging với pool size phù hợp, **không** stress qua Supabase pooler.

---

## 7. Cách chạy script

```bash
cd kol-booking-backend

# Functional smoke E2E
bash scripts/smoke.sh

# Chạy một domain
bash scripts/auth.sh
bash scripts/kols.sh

# Perf
bash scripts/performance.sh

# Log output
bash scripts/smoke.sh 2>&1 | tee scripts/reports/smoke-$(date +%Y%m%d-%H%M%S).log
```

Trên PowerShell:
```powershell
bash .\scripts\smoke.sh
# hoặc gọi script PS1 nếu có
.\scripts\perf.ps1
```

---

## 8. Report format

Mỗi lần chạy CI/smoke test, lưu vào `scripts/reports/`:

### 8.1. Functional report (markdown)

```markdown
# Functional Test Report — 2026-05-15

**Build**: commit 0c2a8e4
**Tester**: QA-bot
**Env**: local / Supabase staging

| Domain | Total | Pass | Fail | Skipped | Notes |
|---|---:|---:|---:|---:|---|
| Auth | 17 | 17 | 0 | 0 | |
| KOL profile | 22 | 21 | 1 | 0 | DELETE channel of others returns 404 not 403 |
| Brand | 12 | 12 | 0 | 0 | |
| Booking | 25 | 23 | 2 | 0 | dispute state machine missing |
| Payment | 18 | 18 | 0 | 0 | |
| Review | 12 | 12 | 0 | 0 | |
| Notification | 8 | 8 | 0 | 0 | |
| Admin | 22 | 20 | 0 | 2 | category delete cascade skipped |
| Security | 8 | 8 | 0 | 0 | |
| **Total** | **144** | **139** | **3** | **2** | |

## Failures
1. `kols.sh::test_delete_channel_other_owner` — expected 403, got 404
2. `bookings.sh::test_dispute_after_complete` — expected 400, got 500
3. `bookings.sh::test_accept_already_confirmed` — expected 400, got 200 (idempotent — clarify spec)
```

### 8.2. Performance report (csv → table)

```csv
endpoint,n,concurrency,avg_ms,p50,p95,p99,errors
GET /kols/search,1000,20,48,45,82,120,0
GET /categories,1000,20,5,4,9,15,0
GET /kols/featured,1000,20,12,10,22,35,0
GET /wallet/me,1000,20,18,16,30,55,0
```

### 8.3. Quy ước

- Mỗi failure ghi: endpoint, request input, expected vs actual response (status + body).
- Perf report kèm git SHA + env (local/staging) + DB pool size.
- Lưu raw curl output vào `scripts/reports/raw/` nếu cần debug.

---

## 9. Notes & gotchas khi test

- **JWT secret**: ở dev đang dùng key hardcode trong `application.properties` — token gen local **không** verify được trên staging và ngược lại.
- **Pre-seeded password**: 8 KOL seed có password `password123`. Sau khi login, gọi `/kols/me` để có profile sẵn `APPROVED` — dùng làm fixture cho booking test.
- **ADMIN account**: phải tự bootstrap. Cách nhanh: register một user, rồi vào Postgres `UPDATE app_user SET role='ADMIN' WHERE email='qa-admin@test.local';`. Script `auth.sh::bootstrap_admin` làm việc này tự động.
- **Idempotent webhook**: payment webhook gọi lần 2 với cùng `externalRef` nên không double-credit wallet — test case bắt buộc.
- **Notification side-effects**: khi admin approve KOL, KOL phải nhận 1 notification — verify cả 2 phía.
- **Booking state machine**: PENDING → CONFIRMED (accept) → PAID (webhook) → DELIVERED (KOL submit) → COMPLETED (BRAND approve) — mỗi transition sai trạng thái phải 400 business, không 500.
- **`spring.jpa.open-in-view=false`** → lazy access ngoài transaction sẽ 500 — test các response có nested entity (channels, packages) chú ý.

---

## 10. Maintenance

- Khi thêm endpoint mới → cập nhật section 4 (checklist) + thêm test trong domain script tương ứng.
- Khi đổi DTO field → grep `scripts/` tìm reference cũ.
- File `scripts/.env.test` **không commit**. Mẫu nằm trong section 0.3.
