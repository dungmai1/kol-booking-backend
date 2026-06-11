# KOL Booking Backend — Flow & Hướng dẫn vận hành

Tài liệu mô tả chi tiết **những gì đã triển khai** trong đợt làm việc này, **cách chạy test**, **setup chạy local**, và **đưa lên production**.

> Stack: Java 21, Spring Boot 3.5.13, PostgreSQL 16, Flyway, Gradle. Package gốc `kolbooking.datn`.

---

## 1. Tổng quan những gì đã làm

| # | Yêu cầu | Trạng thái | Điểm chính |
|---|---------|-----------|------------|
| 1 | Admin ăn % hoa hồng khi booking trả KOL | ✅ | Snapshot tỷ lệ phí vào từng booking; giải ngân tách net/phí; endpoint admin xem doanh thu |
| 2 | Brand đăng sản phẩm + KOL ứng tuyển + lọc top 5 | ✅ | Module mới `product`; rank theo rating/reviews/followers; duyệt → tự tạo booking |
| 3 | Gửi mail xác nhận | ✅ | `EmailService` gửi HTML thật qua `JavaMailSender`; resend + link click được |
| 4 | Bắt buộc xác nhận email mới vào hệ thống | ✅ | `EmailVerificationInterceptor` fail-closed → `403 EMAIL_NOT_VERIFIED` |
| 5 | Thanh toán production-ready | ✅ | VNPay (HMAC-SHA512 + IPN/Return) + mock có chữ ký; vá lỗ hổng webhook |
| 🐛 | **Bug nghiêm trọng có sẵn** | ✅ Đã sửa | Ví hoa hồng `user_id=0` thiếu FK → settlement luôn crash; seed tài khoản hệ thống |

Tất cả đã được **verify end-to-end** trên Postgres thật (29 migrations áp dụng sạch, Hibernate `validate` pass, kịch bản E2E + test suite xanh).

---

## 2. Chi tiết từng tính năng

### 2.1. Xác thực email bắt buộc (Yêu cầu #3, #4)

**Vấn đề trước đây:** đăng ký xong được cấp token ngay, và tài khoản `PENDING_VERIFICATION` **không bị chặn gì** — khác biệt verified/chưa verified chỉ là 1 field `status`. Đây là lỗ hổng.

**Giải pháp (chính sách đã chọn: vẫn cấp token, nhưng khoá tính năng):**

- `EmailVerificationInterceptor` (`common/config/EmailVerificationInterceptor.java`) chạy **fail-closed**: tài khoản đã đăng nhập nhưng chưa verify → trả `403 EMAIL_NOT_VERIFIED` ở **mọi** endpoint, **trừ** allowlist:
  - Mọi method: `/api/v1/auth/**`, `/api/v1/users/me`, `/api/v1/payments/vnpay/**`, `/api/v1/payments/webhook/**`, swagger, actuator, uploads.
  - Chỉ GET (đọc công khai): catalog KOL/category/plan/**product**.
- `AppUserPrincipal` mang thêm cờ `emailVerified` (+ authority `EMAIL_VERIFIED`), nạp tươi từ DB mỗi request qua JWT filter.
- `EmailService` (`auth/service/EmailService.java`): gửi **HTML thật** qua `JavaMailSender` ở prod; dev (`app.mail.dev-mode=true`) chỉ log link. Email lỗi không làm vỡ request (best-effort, `@Async`).
- Endpoint:
  - `GET /api/v1/auth/verify-email?token=...` → trang HTML xác nhận (click trực tiếp từ email).
  - `POST /api/v1/auth/verify-email` → JSON (cho SPA).
  - `POST /api/v1/auth/resend-verification` (body `{ "email": "..." }`) → gửi lại link (không lộ email có tồn tại hay không).

**Luồng:**
```
register (BRAND/KOL) → token + email verify gửi đi (status=PENDING_VERIFICATION)
  → gọi endpoint nghiệp vụ ⇒ 403 EMAIL_NOT_VERIFIED
  → click link / POST verify-email ⇒ status=ACTIVE, emailVerified=true
  → dùng đầy đủ tính năng
```

### 2.2. Sản phẩm/đăng tin + ứng tuyển + top 5 (Yêu cầu #2)

Module mới `kolbooking.datn.product`: `Product`, `ProductApplication`, repo/service/controller/mapper, `ProductSpecification` cho browse.

- **Brand** (hồ sơ `APPROVED`):
  - `POST /api/v1/products` — đăng tin (title, description, budget, category, requiredPlatform, minFollowers, slots, deadline).
  - `PUT /api/v1/products/{id}`, `POST .../close`, `POST .../reopen`, `DELETE` (chỉ khi chưa có ứng tuyển), `GET /api/v1/products/mine`.
  - `GET /api/v1/products/{id}/applications?status=&page=&size=` — danh sách ứng viên.
  - `GET /api/v1/products/{id}/applications/top?by=rating|reviews|followers&limit=5` — **lọc top N** (dùng số liệu denormalized `avg_rating`/`review_count`/`max_follower_count` của KOL).
- **KOL** (hồ sơ `APPROVED`):
  - `POST /api/v1/products/{id}/applications` — ứng tuyển (message, proposedPrice). Unique 1 lần/sản phẩm.
  - `GET /api/v1/applications/mine`, `POST /api/v1/applications/{id}/withdraw`.
- **Brand duyệt ứng viên** (`POST /api/v1/applications/{id}/accept`):
  - **Tự tạo booking PENDING** (tái dùng `BookingService.createBookingFromApplication` → luồng booking/payment/ví/hoa hồng có sẵn).
  - Budget = `proposedPrice` (nếu có) hoặc `product.budget`.
  - Link `application.bookingId`, tự **đóng tin** khi đủ `slots`.
  - `shortlist` / `reject` kèm thông báo + email cho KOL.
- **Public**: `GET /api/v1/products` (filter q/category/platform/budget), `GET /api/v1/products/{id}` (chỉ OPEN hoặc chủ sở hữu).

**Luồng end-to-end:**
```
Brand đăng tin (OPEN)
  → nhiều KOL ứng tuyển (PENDING)
  → Brand xem top 5 theo follow/review/rating
  → Brand accept 1 KOL ⇒ tạo booking PENDING + đóng tin (slots=1)
  → KOL accept booking ⇒ ACCEPTED
  → Brand checkout/thanh toán ⇒ IN_PROGRESS
  → KOL nộp deliverable ⇒ DELIVERED
  → Brand duyệt ⇒ COMPLETED ⇒ giải ngân (KOL net + admin phí)
```

### 2.3. Thanh toán (Yêu cầu #5) + Hoa hồng admin (Yêu cầu #1)

**Hoa hồng:**
- `app.platform.fee-percent` (mặc định 10%) được **snapshot** vào `booking.platform_fee_percent` lúc tạo booking → booking cũ giữ đúng tỷ lệ dù config đổi sau.
- Khi `COMPLETED`: `WalletService.releaseToKol` tính `fee = gross × % (HALF_UP, 2 chữ số)`, `net = gross − fee` (fee + net = gross chính xác). Lưu `platform_fee_amount`/`kol_net_amount` lên booking. KOL nhận net (ví available), ví nền tảng nhận fee.
- Admin: `GET /api/v1/admin/stats/commission` → `{ defaultFeePercent, platformWalletAvailable, totalCommission, commissionTransactions }`.

**Thanh toán — vá lỗ hổng:** webhook MOCK cũ là GET công khai có `status=PAID` → ai biết `externalRef` cũng đánh dấu đã trả. Giờ **bắt buộc chữ ký**:
- MOCK: webhook ký `HMAC-SHA256(app.payment.mock.secret, externalRef + "|" + status)`; sai chữ ký → `403 SIGNATURE_INVALID`; idempotent (webhook trùng bị bỏ qua); kiểm tra số tiền khớp order.
- VNPay (`payment/gateway/VnPayGateway.java`): URL ký **HMAC-SHA512** (canonical: sort key + URL-encode value), verify chữ ký ở:
  - `GET /api/v1/payments/vnpay/ipn` — server-to-server, **authoritative**, trả `{RspCode, Message}` đúng chuẩn VNPay (00/01/02/04/97).
  - `GET /api/v1/payments/vnpay/return` — verify + settle idempotent rồi 302 về frontend `…/payment/result`.
  - Kiểm tra: chữ ký, order tồn tại, `vnp_Amount == budget×100`, đã trả chưa.
- `POST /api/v1/payments/bookings/{bookingId}/checkout` (BRAND, booking `ACCEPTED`) body `{ "provider": "VNPAY" | "MOCK" }` → trả `paymentUrl`. VNPay chưa cấu hình credential → `503` báo rõ ràng.

### 2.4. 🐛 Bug nghiêm trọng đã sửa (migration V29)

Ví hoa hồng nền tảng dùng `wallet.user_id = 0`, nhưng `wallet.user_id` có **FK tới `app_user(id)`** mà **không tồn tại user id=0** → mỗi lần giải ngân booking đều `wallet_user_id_fkey` violation → tính năng hoa hồng **luôn fail ở production**. Đã sửa: seed tài khoản hệ thống `user_id=0`, **role mới `SYSTEM`** (không đăng nhập được — password hash không hợp lệ; không gây nhiễu thống kê ADMIN). `register` giờ chỉ cho phép tự đăng ký `BRAND`/`KOL`.

---

## 3. Migrations mới

| File | Nội dung |
|------|----------|
| `V26__booking_platform_fee.sql` | Thêm `booking.platform_fee_percent` (default 10, backfill), `platform_fee_amount`, `kol_net_amount`; backfill cho booking đã COMPLETED |
| `V27__payment_provider_columns.sql` | Thêm `payment_order.provider_txn_ref`, `raw_callback` (audit) |
| `V28__create_product_application.sql` | Tạo bảng `product` + `product_application` (unique `(product_id, kol_profile_id)`, các index) |
| `V29__seed_platform_wallet_user.sql` | Seed tài khoản hệ thống `id=0` (role SYSTEM) + ví nền tảng — **bắt buộc** để hoa hồng hoạt động |

> Mọi thay đổi schema đều qua Flyway; **không** dùng `ddl-auto=update`. Hibernate chạy ở chế độ `validate`.

---

## 4. Biến môi trường

| Biến | Ý nghĩa | Bắt buộc prod |
|------|---------|---------------|
| `SPRING_DATASOURCE_URL` / `_USERNAME` / `_PASSWORD` | Kết nối Postgres | ✅ |
| `APP_JWT_SECRET` | Khoá ký JWT (base64, đủ dài) | ✅ (đổi khỏi mặc định dev) |
| `APP_CORS_ALLOWED_ORIGINS` | Origin frontend (phẩy ngăn cách) | ✅ |
| `APP_URL` | Base URL backend (link trong email verify) | ✅ |
| `APP_FRONTEND_URL` | Base URL frontend (reset password, redirect VNPay) | ✅ |
| `SPRING_MAIL_HOST` / `_PORT` / `_USERNAME` / `_PASSWORD` | SMTP gửi mail | ✅ |
| `VNPAY_TMN_CODE` / `VNPAY_HASH_SECRET` | Credential VNPay merchant | ✅ nếu dùng VNPay |
| `VNPAY_PAY_URL` | URL cổng VNPay (mặc định sandbox) | — |
| `VNPAY_RETURN_URL` | Return URL (trỏ về `…/api/v1/payments/vnpay/return`) | ✅ nếu dùng VNPay |
| `PAYMENT_MOCK_SECRET` | Khoá ký webhook mock | ✅ đổi khỏi mặc định |

Dev mặc định: profile `dev`, `app.mail.dev-mode=true` (chỉ log mail), VNPay để trống (chỉ dùng MOCK).

---

## 5. Setup & chạy local

> Máy dev hiện **không có JDK/Postgres trên PATH** — đã cài sẵn:
> - JDK 21 (Temurin): `/Users/trustwow4/.jdks/jdk-21.0.11+10/Contents/Home`
> - PostgreSQL 16 (brew, keg-only): `/opt/homebrew/opt/postgresql@16/bin`

### 5.1. Khởi động Postgres + tạo DB
```bash
PGBIN=/opt/homebrew/opt/postgresql@16/bin
PGDATA=/opt/homebrew/var/postgresql@16
"$PGBIN/pg_ctl" -D "$PGDATA" -l /tmp/pg.log -w start   # khởi động server
"$PGBIN/createdb" kolbooking                            # tạo DB (1 lần)
```
Superuser là user OS hiện tại (vd `trustwow4`), trust auth, mật khẩu rỗng.

### 5.2. Chạy app
```bash
export JAVA_HOME="/Users/trustwow4/.jdks/jdk-21.0.11+10/Contents/Home"
export SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/kolbooking"
export SPRING_DATASOURCE_USERNAME="trustwow4"
export SPRING_DATASOURCE_PASSWORD=""

sh ./gradlew bootRun
# hoặc build jar rồi chạy:
sh ./gradlew bootJar
"$JAVA_HOME/bin/java" -jar build/libs/datn-0.0.1-SNAPSHOT.jar
```
- Flyway tự chạy migrations V1→V29 khi khởi động.
- Health: `GET http://localhost:8080/actuator/health` → `{"status":"UP"}`.
- Swagger: `http://localhost:8080/swagger-ui.html`.
- Admin bootstrap: `admin@kolbooking.local` / `Admin@123` (đổi qua `app.admin.email`/`app.admin.password`).

---

## 6. Chạy test

```bash
export JAVA_HOME="/Users/trustwow4/.jdks/jdk-21.0.11+10/Contents/Home"
# Test bao gồm @SpringBootTest contextLoads → CẦN datasource env (Postgres đang chạy)
export SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/kolbooking"
export SPRING_DATASOURCE_USERNAME="trustwow4"
export SPRING_DATASOURCE_PASSWORD=""

sh ./gradlew clean test
```

Test gồm:
- `DatnApplicationTests` — load toàn bộ Spring context (cần DB).
- `AuthServiceRegisterTest`, `KolSortResolverTest` — unit (Mockito, không cần DB).
- `VnPayGatewayTest` — roundtrip ký HMAC-SHA512 + verify + chống giả mạo (không cần DB).

Chạy riêng unit test (không cần DB):
```bash
sh ./gradlew test --tests "*AuthServiceRegisterTest" --tests "*KolSortResolverTest" --tests "*VnPayGatewayTest"
```

---

## 7. Kịch bản smoke test E2E (curl)

Kiểm tra toàn luồng tính năng mới. Yêu cầu app đang chạy + Postgres. (Dùng DB chỉ để mô phỏng admin duyệt hồ sơ cho nhanh; phần còn lại đi qua API thật.)

```bash
BASE=http://localhost:8080/api/v1
PSQL="/opt/homebrew/opt/postgresql@16/bin/psql -d kolbooking -t -A"
PW=password123
J(){ jq -r "$1"; }

# 1) Đăng ký brand → có token nhưng chưa verify
BREG=$(curl -s -X POST $BASE/auth/register -H 'Content-Type: application/json' \
  -d "{\"email\":\"brand@test.local\",\"password\":\"$PW\",\"role\":\"BRAND\"}")
BTOK=$(echo "$BREG"|J .data.accessToken); BUID=$(echo "$BREG"|J .data.userId)

# 2) Chưa verify → tạo product bị chặn (kỳ vọng 403 EMAIL_NOT_VERIFIED)
curl -s -X POST $BASE/products -H "Authorization: Bearer $BTOK" \
  -H 'Content-Type: application/json' -d '{"title":"x","budget":1000000}' | J .errorCode

# 3) Verify email (lấy token từ DB cho deterministic) → click GET link
BVT=$($PSQL -c "SELECT token FROM verification_token WHERE user_id=$BUID AND purpose='EMAIL_VERIFICATION' AND used_at IS NULL ORDER BY id DESC LIMIT 1")
curl -s "$BASE/auth/verify-email?token=$BVT" >/dev/null

# 4) Tạo hồ sơ brand (commit) + admin duyệt (mô phỏng bằng DB)
curl -s -X PUT $BASE/brands/me -H "Authorization: Bearer $BTOK" -H 'Content-Type: application/json' \
  -d '{"companyName":"E2E Brand","contactName":"E2E"}' >/dev/null
$PSQL -c "UPDATE brand_profile SET status='APPROVED' WHERE user_id=$BUID;" >/dev/null

# 5) Brand đăng sản phẩm
PID=$(curl -s -X POST $BASE/products -H "Authorization: Bearer $BTOK" -H 'Content-Type: application/json' \
  -d '{"title":"Can KOL review sua","budget":2000000,"slots":1}' | J .data.id)

# 6) Đăng ký + verify + duyệt 2 KOL (số liệu khác nhau để test ranking)
reg(){ curl -s -X POST $BASE/auth/register -H 'Content-Type: application/json' -d "{\"email\":\"$1\",\"password\":\"$PW\",\"role\":\"KOL\"}"; }
K1=$(reg kol1@test.local); K1TOK=$(echo "$K1"|J .data.accessToken); K1UID=$(echo "$K1"|J .data.userId)
K2=$(reg kol2@test.local); K2TOK=$(echo "$K2"|J .data.accessToken); K2UID=$(echo "$K2"|J .data.userId)
for U in $K1UID $K2UID; do T=$($PSQL -c "SELECT token FROM verification_token WHERE user_id=$U AND purpose='EMAIL_VERIFICATION' AND used_at IS NULL ORDER BY id DESC LIMIT 1"); curl -s "$BASE/auth/verify-email?token=$T">/dev/null; done
$PSQL -c "UPDATE kol_profile SET status='APPROVED', review_count=120, avg_rating=4.9, max_follower_count=500000 WHERE user_id=$K1UID;" >/dev/null
$PSQL -c "UPDATE kol_profile SET status='APPROVED', review_count=10,  avg_rating=4.2, max_follower_count=20000  WHERE user_id=$K2UID;" >/dev/null

# 7) Hai KOL ứng tuyển
A1=$(curl -s -X POST $BASE/products/$PID/applications -H "Authorization: Bearer $K1TOK" -H 'Content-Type: application/json' -d '{"proposedPrice":1800000}' | J .data.id)
curl -s -X POST $BASE/products/$PID/applications -H "Authorization: Bearer $K2TOK" -H 'Content-Type: application/json' -d '{}' >/dev/null

# 8) Brand xem top theo followers (kỳ vọng KOL mạnh đứng đầu)
curl -s "$BASE/products/$PID/applications/top?by=followers&limit=5" -H "Authorization: Bearer $BTOK" | jq '.data[]|{kolDisplayName,kolMaxFollowerCount}'

# 9) Brand duyệt KOL mạnh → tạo booking PENDING
BKID=$(curl -s -X POST $BASE/applications/$A1/accept -H "Authorization: Bearer $BTOK" | J .data.bookingId)

# 10) KOL accept booking → 11) Brand checkout (MOCK) → 12) gọi URL đã ký → IN_PROGRESS
curl -s -X POST $BASE/bookings/$BKID/accept -H "Authorization: Bearer $K1TOK" >/dev/null
PURL=$(curl -s -X POST $BASE/payments/bookings/$BKID/checkout -H "Authorization: Bearer $BTOK" -H 'Content-Type: application/json' -d '{"provider":"MOCK"}' | J .data.paymentUrl)
curl -s "$PURL" >/dev/null

# 13) KOL nộp deliverable → 14) Brand duyệt → COMPLETED + settlement
curl -s -X POST $BASE/bookings/$BKID/deliverables -H "Authorization: Bearer $K1TOK" -H 'Content-Type: application/json' -d '{"type":"VIDEO","platform":"TIKTOK","submittedUrl":"https://x"}' >/dev/null
curl -s -X POST $BASE/bookings/$BKID/approve-delivery -H "Authorization: Bearer $BTOK" | jq '.data|{status,platformFeePercent,platformFeeAmount,kolNetAmount}'

# 15) Admin xem hoa hồng
ATOK=$(curl -s -X POST $BASE/auth/login -H 'Content-Type: application/json' -d '{"email":"admin@kolbooking.local","password":"Admin@123"}' | J .data.accessToken)
curl -s "$BASE/admin/stats/commission" -H "Authorization: Bearer $ATOK" | jq
```

Kết quả kỳ vọng đã verify: bước 2 → `EMAIL_NOT_VERIFIED`; bước 8 → KOL mạnh đứng đầu; bước 14 → `COMPLETED`, fee 10% = 180.000, net = 1.620.000; bước 15 → `platformWalletAvailable` tăng đúng phí.

---

## 8. Đưa lên production

### 8.1. Build
```bash
JAVA_HOME=/path/to/jdk-21 ./gradlew clean bootJar
# artifact: build/libs/datn-0.0.1-SNAPSHOT.jar
```

### 8.2. Chạy (profile prod)
```bash
SPRING_PROFILES_ACTIVE=prod \
SPRING_DATASOURCE_URL=jdbc:postgresql://<host>:5432/<db> \
SPRING_DATASOURCE_USERNAME=<user> SPRING_DATASOURCE_PASSWORD=<pass> \
APP_JWT_SECRET=<base64-secret-dai> \
APP_URL=https://api.kol.example APP_FRONTEND_URL=https://kol.example \
APP_CORS_ALLOWED_ORIGINS=https://kol.example \
SPRING_MAIL_HOST=smtp.gmail.com SPRING_MAIL_PORT=587 \
SPRING_MAIL_USERNAME=<email> SPRING_MAIL_PASSWORD=<app-password> \
VNPAY_TMN_CODE=<tmn> VNPAY_HASH_SECRET=<secret> \
VNPAY_RETURN_URL=https://api.kol.example/api/v1/payments/vnpay/return \
PAYMENT_MOCK_SECRET=<random-secret> \
java -jar build/libs/datn-0.0.1-SNAPSHOT.jar
```
Flyway tự áp dụng migrations lên DB prod khi khởi động. Profile `prod` đã bật `app.mail.dev-mode=false` (gửi SMTP thật).

### 8.3. Checklist production (bắt buộc)
- [ ] **Đổi** `APP_JWT_SECRET` và `PAYMENT_MOCK_SECRET` khỏi giá trị mặc định.
- [ ] **SMTP**: nếu Gmail, dùng *App Password* (không dùng mật khẩu thường). Kiểm tra gửi được mail verify.
- [ ] **VNPay**: khai báo **IPN URL** trong cổng merchant = `https://api.kol.example/api/v1/payments/vnpay/ipn`; `VNPAY_RETURN_URL` trỏ về backend return. IPN là nguồn xác nhận chính.
- [ ] **Reverse proxy/HTTPS**: forward header `X-Forwarded-For` (dùng cho `vnp_IpAddr`). Bắt buộc HTTPS.
- [ ] **DB**: backup trước khi deploy; Flyway chỉ chạy tiến (forward-only).
- [ ] (Tuỳ chọn) tắt Swagger: `springdoc.api-docs.enabled=false`, `springdoc.swagger-ui.enabled=false`.
- [ ] Admin bootstrap: đặt `app.admin.email`/`app.admin.password` qua env và **đổi mật khẩu** sau lần đăng nhập đầu.

### 8.4. Lưu ý vận hành
- Tài khoản hệ thống ví hoa hồng (`user_id=0`, role `SYSTEM`) **không đăng nhập được** — không xoá. Theo dõi số dư qua `GET /api/v1/admin/stats/commission`.
- Webhook/IPN/Return là `permitAll` ở filter nhưng **được bảo vệ bằng chữ ký** trong service — không endpoint thanh toán nào tin payload mà không verify chữ ký.
- Tài khoản chưa xác nhận email bị khoá tính năng (fail-closed); người dùng dùng `POST /api/v1/auth/resend-verification` để nhận lại link.

---

## 9. Tóm tắt endpoint mới/đáng chú ý

| Method | Path | Role | Mô tả |
|--------|------|------|------|
| GET  | `/api/v1/auth/verify-email?token=` | public | Trang HTML xác nhận email |
| POST | `/api/v1/auth/resend-verification` | public | Gửi lại link xác nhận |
| POST | `/api/v1/products` | BRAND | Đăng sản phẩm |
| GET  | `/api/v1/products` | public | Browse (filter q/category/platform/budget) |
| GET  | `/api/v1/products/{id}` | public | Chi tiết |
| GET  | `/api/v1/products/mine` | BRAND | Sản phẩm của tôi |
| PUT/DELETE | `/api/v1/products/{id}` (+`/close`,`/reopen`) | BRAND | Quản lý tin |
| POST | `/api/v1/products/{id}/applications` | KOL | Ứng tuyển |
| GET  | `/api/v1/products/{id}/applications` | BRAND | Danh sách ứng viên |
| GET  | `/api/v1/products/{id}/applications/top?by=&limit=` | BRAND | **Top N** ứng viên |
| GET  | `/api/v1/applications/mine` | KOL | Ứng tuyển của tôi |
| POST | `/api/v1/applications/{id}/withdraw` | KOL | Rút ứng tuyển |
| POST | `/api/v1/applications/{id}/shortlist`/`accept`/`reject` | BRAND | Xử lý ứng viên (accept → tạo booking) |
| POST | `/api/v1/payments/bookings/{id}/checkout` | BRAND | Tạo thanh toán (VNPAY/MOCK) |
| GET  | `/api/v1/payments/vnpay/ipn` | public(ký) | IPN VNPay |
| GET  | `/api/v1/payments/vnpay/return` | public(ký) | Return VNPay |
| GET  | `/api/v1/admin/stats/commission` | ADMIN | Doanh thu hoa hồng |
