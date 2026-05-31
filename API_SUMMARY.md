# KOL Booking Backend — API Summary

Tài liệu hợp đồng API cho team frontend. Đây là nguồn sự thật cho:
endpoint, envelope, enum, seed accounts, business rules.

> Backend: Spring Boot 3.5.13 / Java 21 / PostgreSQL 16 / Flyway.
> Swagger UI: `http://<host>:8080/swagger-ui.html` — OpenAPI JSON: `/v3/api-docs`.

---

## 1. Base URL & Envelope

- Base path: **`/api/v1`** (mọi endpoint nghiệp vụ đều prefix này, trừ Swagger / Actuator).
- Mọi response thành công và lỗi nghiệp vụ đều bọc trong `ApiResponse<T>`:

```json
{
  "success": true,
  "data": { ... },
  "message": "Subscription cancelled",
  "errorCode": null
}
```

```json
{
  "success": false,
  "data": null,
  "message": "Authentication required",
  "errorCode": "UNAUTHORIZED"
}
```

- Các response phân trang dùng `PageResponse<T>` (bọc trong `ApiResponse`):

```json
{
  "success": true,
  "data": {
    "content": [ ... ],
    "totalElements": 137,
    "totalPages": 7,
    "currentPage": 0,
    "pageSize": 20,
    "hasNext": true,
    "hasPrevious": false
  },
  "message": null,
  "errorCode": null
}
```

`currentPage` là 0-based. `pageSize` mặc định 20 trừ khi endpoint quy định khác (xem mục Endpoints).

---

## 2. Authentication

- JWT Bearer token. Header: `Authorization: Bearer <accessToken>`.
- Public endpoints (không cần token) — xem mục Public bên dưới.
- Lỗi 401 (chưa đăng nhập / token sai) → `errorCode: "UNAUTHORIZED"`.
- Lỗi 403 (đăng nhập nhưng không đủ quyền) → `errorCode: "FORBIDDEN"`.
- KHÔNG trả stacktrace. Mọi exception đi qua `@RestControllerAdvice` trả `ApiResponse.error`.

### CORS (đã bật sẵn cho dev)

Allowed origins (configured ở `application.properties → app.cors.allowed-origins`):

- `http://localhost:3000`
- `http://localhost:3001`
- `http://localhost:5173`

Methods/headers cho phép tất cả; credentials = true.

### Public endpoints (không cần JWT)

| Method | Path |
|--------|------|
| ANY    | `/api/v1/auth/**` |
| ANY    | `/api/v1/payments/webhook/**` |
| GET    | `/api/v1/categories/**` |
| GET    | `/api/v1/kols/search` |
| GET    | `/api/v1/kols/featured` |
| GET    | `/api/v1/kols/{slug}` (slug = `[a-z0-9-]+`) |
| GET    | `/api/v1/kols/{id}/reviews` |
| GET    | `/api/v1/users/{id}/reviews` |
| GET    | `/api/v1/plans` |
| GET    | `/api/v1/plans/{code}` |
| ANY    | `/swagger-ui.html`, `/swagger-ui/**`, `/v3/api-docs/**`, `/actuator/health`, `/actuator/info`, `/uploads/**` |

---

## 3. Enums — nguồn sự thật

Tất cả enum dùng `EnumType.STRING`. Frontend giữ đúng case.

| Enum | Values |
|------|--------|
| `Role` | `ADMIN`, `BRAND`, `KOL` |
| `UserStatus` | `PENDING_VERIFICATION`, `ACTIVE`, `INACTIVE`, `BANNED` |
| `KolProfileStatus` | `DRAFT`, `PENDING_REVIEW`, `APPROVED`, `REJECTED` |
| `BrandProfileStatus` | `DRAFT`, `PENDING_REVIEW`, `APPROVED`, `REJECTED` |
| `BookingStatus` | `PENDING`, `ACCEPTED`, `REJECTED`, `CANCELLED`, `IN_PROGRESS`, `DELIVERED`, `COMPLETED`, `DISPUTED`, `CANCELLED_BY_ADMIN` |
| `DeliverableStatus` | `PENDING`, `SUBMITTED`, `ACCEPTED`, `REJECTED` |
| `TransactionType` | `DEPOSIT`, `HOLD`, `RELEASE`, `WITHDRAW`, `REFUND`, `FEE` |
| `WithdrawStatus` | `PENDING`, `APPROVED`, `REJECTED`, `PAID` |
| `SubscriptionStatus` | `PENDING_PAYMENT`, `ACTIVE`, `EXPIRED`, `CANCELLED` |
| `PaymentProvider` | `VNPAY`, `MOMO`, `STRIPE`, `MOCK` |
| `NotificationType` | `BOOKING_CREATED`, `BOOKING_ACCEPTED`, `BOOKING_REJECTED`, `BOOKING_CANCELLED`, `BOOKING_IN_PROGRESS`, `DELIVERABLE_SUBMITTED`, `BOOKING_COMPLETED`, `BOOKING_DISPUTED`, `PAYMENT_SUCCESS`, `REVIEW_RECEIVED`, `WITHDRAW_APPROVED`, `WITHDRAW_REJECTED`, `PROFILE_APPROVED`, `PROFILE_REJECTED`, `NEW_MESSAGE` |
| `Platform` | `TIKTOK`, `INSTAGRAM`, `YOUTUBE`, `FACEBOOK` |
| `Gender` | `MALE`, `FEMALE`, `OTHER` |
| `PricingPackageType` | `POST`, `VIDEO`, `LIVESTREAM`, `STORY`, `COMBO` |
| `MediaType` | `IMAGE`, `VIDEO`, `LINK` |

### Error codes (`errorCode` field)

`VALIDATION_FAILED`, `RESOURCE_NOT_FOUND`, `BUSINESS_ERROR`, `UNAUTHORIZED`, `FORBIDDEN`, `INTERNAL_ERROR`, `EMAIL_ALREADY_EXISTS`, `INVALID_CREDENTIALS`, `ACCOUNT_INACTIVE`, `ACCOUNT_BANNED`, `TOKEN_INVALID`, `TOKEN_EXPIRED`, `TOKEN_USED`.

### Category slugs (8 canonical)

`beauty`, `fashion`, `food`, `lifestyle`, `travel`, `fitness`, `tech`, `entertainment`.

---

## 4. Endpoints theo 18 chức năng

> Format: `METHOD PATH` — role yêu cầu — mô tả ngắn.
> Path đã bao gồm prefix `/api/v1`.

### 4.1 Auth (public)

| Method | Path | Mô tả |
|--------|------|-------|
| POST | `/api/v1/auth/register` | Đăng ký (201, trả `AuthTokens`) |
| POST | `/api/v1/auth/login` | Đăng nhập (trả `AuthTokens`) |
| POST | `/api/v1/auth/refresh` | Refresh access token |
| POST | `/api/v1/auth/logout` | Revoke refresh token |
| POST | `/api/v1/auth/verify-email` | Xác minh email từ token |
| POST | `/api/v1/auth/forgot-password` | Gửi email reset |
| POST | `/api/v1/auth/reset-password` | Reset bằng token |

### 4.2 User self-service (mọi role đã đăng nhập)

| Method | Path | Mô tả |
|--------|------|-------|
| GET    | `/api/v1/users/me` | Hồ sơ user hiện tại |
| PATCH  | `/api/v1/users/me/deactivate` | Tự deactivate tài khoản (status `INACTIVE`) |
| DELETE | `/api/v1/users/me` | Soft delete tài khoản |

### 4.3 KOL discovery & profile

Public:
| Method | Path | Mô tả |
|--------|------|-------|
| GET | `/api/v1/kols/search` | Filter list KOL — params bên dưới |
| GET | `/api/v1/kols/featured?limit=10` | KOL nổi bật |
| GET | `/api/v1/kols/{slug}` | Chi tiết KOL theo slug (slug `[a-z0-9-]+`) |

Tham số `/kols/search`:
- `q` (string) — search theo display name / slug / bio
- `categoryIds` (List<Long>)
- `platforms` (Set<Platform>)
- `minFollower`, `maxFollower` (Long)
- `minPrice`, `maxPrice` (BigDecimal, VND)
- `city`, `country` (string)
- `gender` (`Gender`)
- `minRating` (1..5, Double)
- `sort` (default `featured`; gợi ý: `rating`, `priceAsc`, `priceDesc`, `newest`)
- `page` (0-based), `size` (default 20)

Trả `ApiResponse<PageResponse<KolSummaryResponse>>`. Nếu request có JWT BRAND, mỗi item có `isFavorite: boolean` cho biết BRAND có favorite KOL đó hay chưa.

Role KOL (quản lý profile của chính mình):
| Method | Path | Mô tả |
|--------|------|-------|
| GET    | `/api/v1/kols/me` | Lấy profile KOL hiện tại |
| PUT    | `/api/v1/kols/me` | Cập nhật profile |
| POST   | `/api/v1/kols/me/submit` | Submit profile chờ admin duyệt (`DRAFT` → `PENDING_REVIEW`) |
| POST   | `/api/v1/kols/me/channels` | Thêm social channel |
| DELETE | `/api/v1/kols/me/channels/{id}` | Xoá channel |
| POST   | `/api/v1/kols/me/packages` | Thêm pricing package |
| DELETE | `/api/v1/kols/me/packages/{id}` | Xoá package |
| POST   | `/api/v1/kols/me/portfolio` | Thêm portfolio item |
| DELETE | `/api/v1/kols/me/portfolio/{id}` | Xoá portfolio item |

### 4.4 Brand profile & favorites

Role BRAND:
| Method | Path | Mô tả |
|--------|------|-------|
| GET  | `/api/v1/brands/me` | Profile brand hiện tại |
| PUT  | `/api/v1/brands/me` | Cập nhật profile |
| POST | `/api/v1/brands/me/submit` | Submit profile (`DRAFT` → `PENDING_REVIEW`) |
| POST | `/api/v1/brands/me/favorites/{kolId}` | Add favorite KOL |
| DELETE | `/api/v1/brands/me/favorites/{kolId}` | Remove favorite KOL |
| GET  | `/api/v1/brands/me/favorites?page=&size=` | List favorites (paged) |

### 4.5 Bookings

Role BRAND tạo booking, KOL accept/reject, BRAND approve/dispute, cả 2 xem.

| Method | Path | Role | Mô tả |
|--------|------|------|-------|
| POST | `/api/v1/bookings` | BRAND | Tạo booking (body `CreateBookingRequest`) |
| GET  | `/api/v1/bookings/me?status=&page=&size=` | BRAND | Bookings của mình |
| GET  | `/api/v1/bookings/incoming?status=&page=&size=` | KOL | Bookings tới mình |
| GET  | `/api/v1/bookings/{id}` | BRAND/KOL (participant) | Chi tiết |
| POST | `/api/v1/bookings/{id}/cancel` | BRAND | Huỷ (body `ReasonRequest`) |
| POST | `/api/v1/bookings/{id}/accept` | KOL | Accept |
| POST | `/api/v1/bookings/{id}/reject` | KOL | Reject (`ReasonRequest`) |
| POST | `/api/v1/bookings/{id}/deliverables` | KOL | Submit deliverable (`SubmitDeliverableRequest`) |
| POST | `/api/v1/bookings/{id}/approve-delivery` | BRAND | Approve → `COMPLETED` + release payout |
| POST | `/api/v1/bookings/{id}/dispute` | BRAND | Mở dispute (`ReasonRequest`) |
| POST | `/api/v1/bookings/{id}/messages` | BRAND/KOL | Gửi message (body `MessageRequest`) |
| GET  | `/api/v1/bookings/{id}/messages?page=&size=50` | BRAND/KOL | List messages (paged, default size 50) |

State machine:
`PENDING` → (KOL accept) → `ACCEPTED` → (BRAND pay) → `IN_PROGRESS` → (KOL submit) → `DELIVERED` → (BRAND approve) → `COMPLETED`.
Side branches: `REJECTED` (KOL từ chối), `CANCELLED` (BRAND huỷ trước accept), `DISPUTED` (BRAND mở tranh chấp), `CANCELLED_BY_ADMIN` (admin huỷ).

### 4.6 Reviews

| Method | Path | Role | Mô tả |
|--------|------|------|-------|
| POST | `/api/v1/bookings/{bookingId}/reviews` | BRAND/KOL | Tạo review sau khi booking COMPLETED (body `ReviewCreateRequest`) |
| PUT  | `/api/v1/reviews/{reviewId}` | author | Sửa review của mình |
| GET  | `/api/v1/users/{userId}/reviews?page=&size=` | public | Reviews nhận bởi user (KOL hoặc Brand) |
| GET  | `/api/v1/kols/{kolId}/reviews?page=&size=` | public | Reviews nhận bởi KOL (alias tiện cho frontend) |

### 4.7 Payments

| Method | Path | Role | Mô tả |
|--------|------|------|-------|
| POST | `/api/v1/payments/bookings/{bookingId}/checkout` | BRAND | Tạo checkout — body `CheckoutRequest(provider)`. Trả `paymentUrl` để redirect. |
| GET  | `/api/v1/payments/bookings/{bookingId}` | BRAND/KOL | Trạng thái payment |
| POST | `/api/v1/payments/webhook/{provider}` | public | Webhook từ PSP (body `WebhookRequest`) |
| GET  | `/api/v1/payments/webhook/{provider}?externalRef=&amount=&status=PAID` | public | Mock webhook để dev tự xác nhận thanh toán |

### 4.8 Wallet (mọi role đã đăng nhập)

| Method | Path | Mô tả |
|--------|------|-------|
| GET | `/api/v1/wallet/me` | Wallet hiện tại (`balanceAvailable`, `balanceHeld`, `currency`) |
| GET | `/api/v1/wallet/me/transactions?page=&size=` | Lịch sử giao dịch ví |

### 4.9 Withdrawals

| Method | Path | Role | Mô tả |
|--------|------|------|-------|
| POST | `/api/v1/withdraws` | KOL | Tạo yêu cầu rút (body `WithdrawCreateRequest`) |
| GET  | `/api/v1/withdraws/me?page=&size=` | KOL | Lịch sử rút của mình |
| GET  | `/api/v1/withdraws/admin?status=PENDING&page=&size=` | ADMIN | Queue rút |
| POST | `/api/v1/withdraws/admin/{id}/approve` | ADMIN | Approve |
| POST | `/api/v1/withdraws/admin/{id}/paid` | ADMIN | Đánh dấu đã chuyển khoản |
| POST | `/api/v1/withdraws/admin/{id}/reject` | ADMIN | Reject (body `ReasonRequest`) |

### 4.10 Notifications

| Method | Path | Mô tả |
|--------|------|-------|
| GET   | `/api/v1/notifications/me?unreadOnly=&page=&size=` | List notification của user hiện tại |
| GET   | `/api/v1/notifications/me/unread-count` | Trả `{ "count": <long> }` |
| PATCH | `/api/v1/notifications/{id}/read` | Đánh dấu đã đọc |
| POST  | `/api/v1/notifications/me/read-all` | Trả `{ "updated": <int> }` |

### 4.11 Categories

| Method | Path | Mô tả |
|--------|------|-------|
| GET | `/api/v1/categories` | Tree categories (public) |

Admin CRUD:
| Method | Path | Mô tả |
|--------|------|-------|
| POST   | `/api/v1/admin/categories` | Tạo |
| PUT    | `/api/v1/admin/categories/{id}` | Sửa |
| DELETE | `/api/v1/admin/categories/{id}` | Xoá |

### 4.12 File upload

| Method | Path | Mô tả |
|--------|------|-------|
| POST | `/api/v1/files/upload` | Multipart, field `file`. Trả `{ url: "..." }`. Files được serve qua `/uploads/**`. |

### 4.13 Plans & Subscriptions

Plans (public):
| Method | Path | Mô tả |
|--------|------|-------|
| GET | `/api/v1/plans?targetRole=BRAND|KOL` | Danh sách gói active |
| GET | `/api/v1/plans/{code}` | Chi tiết gói theo code |

Subscriptions (authenticated):
| Method | Path | Mô tả |
|--------|------|-------|
| GET  | `/api/v1/subscriptions/me` | Subscription hiện tại (ưu tiên ACTIVE, nếu không thì latest) |
| GET  | `/api/v1/subscriptions/me/history` | Lịch sử subscription |
| POST | `/api/v1/subscriptions/checkout` | Checkout (body `SubscriptionCheckoutRequest{ planCode, provider, autoRenew }`) — plan free → ACTIVE ngay, paid → `PENDING_PAYMENT` + trả `paymentUrl` (mock webhook). |
| POST | `/api/v1/subscriptions/{id}/cancel?reason=` | Huỷ subscription (404 nếu không tồn tại, 403 nếu không phải owner, 409 nếu đã `CANCELLED`/`EXPIRED`) |

### 4.14 Admin — Profile review

KOL:
| Method | Path | Mô tả |
|--------|------|-------|
| GET  | `/api/v1/admin/kols?status=PENDING_REVIEW&page=&size=` | Queue KOL (status `PENDING_REVIEW` để duyệt) |
| POST | `/api/v1/admin/kols/{id}/approve` | Duyệt |
| POST | `/api/v1/admin/kols/{id}/reject` | Từ chối (body `RejectReasonRequest`) |

Brand:
| Method | Path | Mô tả |
|--------|------|-------|
| GET  | `/api/v1/admin/brands?status=PENDING_REVIEW&page=&size=` | Queue Brand |
| POST | `/api/v1/admin/brands/{id}/approve` | Duyệt |
| POST | `/api/v1/admin/brands/{id}/reject` | Từ chối |

List items trả `Map<String,Object>` với keys: `id`, `userId`, `displayName`/`companyName`, `slug`/`industry`, `status`, `avgRating`, `reviewCount`, `rejectReason`, `createdAt`.

### 4.15 Admin — Users

| Method | Path | Mô tả |
|--------|------|-------|
| GET  | `/api/v1/admin/users?q=&role=&page=&size=` | List users — trả `AdminUserResponse(id, email, role, status, emailVerified, createdAt)` |
| POST | `/api/v1/admin/users/{id}/ban` | Ban (status → `BANNED`) |
| POST | `/api/v1/admin/users/{id}/unban` | Unban (status → `ACTIVE`) |

### 4.16 Admin — Bookings & Disputes

| Method | Path | Mô tả |
|--------|------|-------|
| GET  | `/api/v1/admin/bookings?status=&page=&size=` | List bookings (filter optional) |
| POST | `/api/v1/admin/bookings/{id}/resolve-dispute` | Resolve dispute (body `DisputeResolutionRequest{ decision: REFUND_BRAND \| RELEASE_KOL, note }`) |

### 4.17 Admin — Stats & Audit

| Method | Path | Mô tả |
|--------|------|-------|
| GET | `/api/v1/admin/stats/overview` | Tổng quan (users, bookings, revenue, …) |
| GET | `/api/v1/admin/stats/bookings?from=&to=` | Time-series bookings (default 365d) |
| GET | `/api/v1/admin/stats/top-kols?limit=10` | Top KOLs theo doanh thu |
| GET | `/api/v1/admin/stats/revenue?from=&to=` | Time-series revenue |
| GET | `/api/v1/admin/audit-logs?page=&size=50` | Audit log |

### 4.18 Health

| Method | Path | Mô tả |
|--------|------|-------|
| GET | `/actuator/health` | Health check |
| GET | `/actuator/info` | Build info |

---

## 5. Business rules cần frontend lưu ý

- **Platform fee 10%**: budget Brand trả = `amount`, KOL nhận thực `amount * 0.9`, platform giữ `amount * 0.1` (ghi `FEE` trong wallet_transaction).
- **Escrow**: Khi BRAND thanh toán (`IN_PROGRESS`) tiền chuyển sang `balanceHeld` cả hai bên — release sau khi BRAND approve delivery (`COMPLETED`).
- **Booking submit chỉ 1 deliverable / lần**: khi KOL `POST /deliverables`, booking chuyển `IN_PROGRESS` → `DELIVERED`. BRAND `approve-delivery` → `COMPLETED`; nếu BRAND `dispute` → `DISPUTED`, chờ admin xử lý.
- **Subscription free plan**: `price = 0` → ACTIVE ngay, `expiresAt = now + durationDays`.
- **Subscription paid plan**: trả `paymentUrl` dạng `…/api/v1/subscriptions/webhook/<PROVIDER>?externalRef=&amount=&status=PAID`. Mock dev có thể GET URL này để confirm.
- **Profile submit flow**: KOL/Brand sửa profile ở status `DRAFT`/`REJECTED`, gọi `POST /me/submit` để chuyển sang `PENDING_REVIEW`. Admin approve/reject. Status `APPROVED` mới hiển thị public ở search.
- **Pagination**: tất cả endpoint paged dùng 0-based `page`, default `size=20` (booking messages default `size=50`).
- **Role guarding**: 401 nếu thiếu/sai token; 403 nếu role không khớp (vd KOL gọi endpoint BRAND).

---

## 6. Seed data (Flyway V8–V17)

Toàn bộ tài khoản seed dùng password chung: **`password123`** (bcrypt hash `$2a$10$ZJo6Pxi.C7ichgif9MD6DOZCjfn3AxLh2q18qP39T5qHtxF3u9d7C`).

### Admin

Không seed sẵn admin trong file (do an toàn). Để tạo admin dev:

```sql
UPDATE app_user SET role = 'ADMIN' WHERE email = '<user>@seed.local';
```

### Brand (4 tài khoản APPROVED)

| Email | Company |
|-------|---------|
| `brand.vinamilk@seed.local` | Vinamilk |
| `brand.shopee@seed.local`   | Shopee Vietnam |
| `brand.tch@seed.local`      | The Coffee House |
| `brand.bitis@seed.local`    | Biti's |

> Mỗi brand được nạp ví sẵn `150–200M VND` và có 1 subscription FREE đang `ACTIVE`.

### KOL APPROVED (8 tài khoản — seed V8)

| Email | Display name |
|-------|--------------|
| `thanhnha25091@seed.local` | Nguyễn Thị Thanh Nhã |
| `lambaongoc@seed.local`    | Lâm Bảo Ngọc |
| `tien.tien@seed.local`     | Tiên Tiên |
| `hoangsoi2809@seed.local`  | Hoàng Sói |
| `phuongdidau@seed.local`   | Phương Đi Đâu |
| `tebefood@seed.local`      | Tebe Food |
| `tuandidau@seed.local`     | Tuấn Đi Đâu |
| `hoangduyen.dreams@seed.local` | Hoàng Duyên |

### KOL PENDING_REVIEW (2 tài khoản — V17)

| Email | Display name |
|-------|--------------|
| `kol.submitted1@seed.local` | Nguyễn Đăng Khoa |
| `kol.submitted2@seed.local` | Trần Mỹ Anh |

### Booking states đã seed (V17)

Mỗi state ít nhất 1 record (truy theo `campaign_title`):

| State | Campaign |
|-------|----------|
| `PENDING` | Vinamilk Summer Sport 2026 |
| `ACCEPTED` | Shopee 5.5 Music Theme |
| `REJECTED` | TCH Acoustic Night |
| `CANCELLED` | Bitis Trekking Series |
| `IN_PROGRESS` | Vinamilk Probi Daily, Shopee Kitchen Reno |
| `DELIVERED` | TCH Hanoi Weekend |
| `COMPLETED` | Vinamilk Athlete Drive 2026 (seed sẵn từ V8/V15) |

### Withdrawals — đủ 4 state

| State | Email |
|-------|-------|
| `PENDING`  | `lambaongoc@seed.local` |
| `APPROVED` | `phuongdidau@seed.local` |
| `PAID`     | `hoangduyen.dreams@seed.local` |
| `REJECTED` | `tien.tien@seed.local` |

### Notifications

V17 phủ các `NotificationType` chính: `BOOKING_CREATED`, `BOOKING_ACCEPTED`, `BOOKING_REJECTED`, `BOOKING_CANCELLED`, `BOOKING_COMPLETED`, `DELIVERABLE_SUBMITTED`, `PAYMENT_SUCCESS`, `REVIEW_RECEIVED`, `WITHDRAW_REJECTED`, `NEW_MESSAGE` — đủ để frontend test list unread/paged.

---

## 7. Tài liệu sống (OpenAPI / Swagger)

- Swagger UI: `/swagger-ui.html`
- OpenAPI JSON: `/v3/api-docs`
- OpenAPI YAML: `/v3/api-docs.yaml`

Springdoc tự sinh từ controller + DTO. Khi backend thêm endpoint mới → Swagger luôn là nguồn cập nhật chính xác nhất; tài liệu này (`API_SUMMARY.md`) là bản tóm lược nghiệp vụ.

---

## 8. Ghi chú vận hành

- Dev chạy `gradlew bootRun` trên JVM host (không docker dev). Biến môi trường `SPRING_DATASOURCE_URL` / `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD` truyền qua launcher / `.env`.
- Prod (Render) dùng Supabase **session pooler**, giới hạn 15 client — đừng để local `bootRun` chạy đồng thời khi redeploy (Flyway sẽ EMAXCONNSESSION).
- Mọi thay đổi schema phải đi qua Flyway migration mới (`V<n>__*.sql`); không dùng `ddl-auto=update` ở prod.
