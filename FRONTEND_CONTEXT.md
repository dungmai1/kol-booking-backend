# KOL Booking — Frontend Integration Context

> File này là nguồn sự thật cho frontend. Đọc trước khi hỏi AI hoặc bắt đầu bất kỳ màn hình nào.
> Backend: Spring Boot 3.5.13 · PostgreSQL 16 · Java 21

---

## 1. Base URL & Môi trường

| Môi trường | URL |
|------------|-----|
| Dev local  | `http://localhost:8080` |
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| OpenAPI JSON | `http://localhost:8080/v3/api-docs` |

**Base path API:** `/api/v1` (prefix tất cả endpoint nghiệp vụ)

---

## 2. Response Envelope

**Mọi response đều bọc trong `ApiResponse<T>`** — không bao giờ trả naked object.

```ts
interface ApiResponse<T> {
  success: boolean       // true = thành công, false = lỗi nghiệp vụ
  data: T | null         // null khi lỗi
  message: string | null // mô tả kết quả hoặc lỗi
  errorCode: string | null // xem danh sách ErrorCode bên dưới
}
```

**Paged response** — `data` là `PageResponse<T>`:

```ts
interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  currentPage: number  // 0-based (trang đầu = 0)
  pageSize: number
  hasNext: boolean
  hasPrevious: boolean
}
// => ApiResponse<PageResponse<T>>
```

**Ví dụ thành công:**
```json
{ "success": true, "data": { ... }, "message": null, "errorCode": null }
```

**Ví dụ lỗi:**
```json
{ "success": false, "data": null, "message": "Authentication required", "errorCode": "UNAUTHORIZED" }
```

---

## 3. Authentication & Authorization

- JWT Bearer token: header `Authorization: Bearer <accessToken>`
- Không dùng cookie (stateless).
- `accessToken` có thời hạn ngắn → dùng `POST /api/v1/auth/refresh` với `refreshToken` để lấy token mới.

### Xử lý lỗi auth (bắt buộc implement)

| HTTP | errorCode | Xử lý |
|------|-----------|-------|
| 401 | `UNAUTHORIZED` | Redirect về `/login` |
| 403 | `FORBIDDEN` | Hiện thông báo "Bạn không có quyền thực hiện hành động này" |

### CORS đã bật cho dev

Allowed origins: `http://localhost:3000` · `http://localhost:3001` · `http://localhost:5173`

---

## 4. Public Endpoints (không cần JWT)

| Method | Path |
|--------|------|
| POST | `/api/v1/auth/register` |
| POST | `/api/v1/auth/login` |
| POST | `/api/v1/auth/refresh` |
| POST | `/api/v1/auth/logout` |
| POST | `/api/v1/auth/verify-email` |
| POST | `/api/v1/auth/forgot-password` |
| POST | `/api/v1/auth/reset-password` |
| GET | `/api/v1/categories/**` |
| GET | `/api/v1/kols/search` |
| GET | `/api/v1/kols/featured` |
| GET | `/api/v1/kols/{slug}` |
| GET | `/api/v1/kols/{id}/reviews` |
| GET | `/api/v1/users/{id}/reviews` |
| GET | `/api/v1/plans` |
| GET | `/api/v1/plans/{code}` |
| ANY | `/api/v1/payments/webhook/**` |

---

## 5. Enums

Tất cả enum truyền dưới dạng **string đúng case** (Java `EnumType.STRING`).

```ts
// User & Auth
type Role = 'ADMIN' | 'BRAND' | 'KOL'
type UserStatus = 'PENDING_VERIFICATION' | 'ACTIVE' | 'INACTIVE' | 'BANNED'

// Profile
type KolProfileStatus   = 'DRAFT' | 'PENDING_REVIEW' | 'APPROVED' | 'REJECTED'
type BrandProfileStatus = 'DRAFT' | 'PENDING_REVIEW' | 'APPROVED' | 'REJECTED'

// Booking
type BookingStatus =
  | 'PENDING'
  | 'ACCEPTED'
  | 'REJECTED'
  | 'CANCELLED'
  | 'IN_PROGRESS'
  | 'DELIVERED'
  | 'COMPLETED'
  | 'DISPUTED'
  | 'CANCELLED_BY_ADMIN'

type DeliverableStatus = 'PENDING' | 'SUBMITTED' | 'ACCEPTED' | 'REJECTED'

// Thanh toán & Ví
type TransactionType = 'DEPOSIT' | 'HOLD' | 'RELEASE' | 'WITHDRAW' | 'REFUND' | 'FEE'
type WithdrawStatus  = 'PENDING' | 'APPROVED' | 'REJECTED' | 'PAID'
type PaymentProvider = 'VNPAY' | 'MOMO' | 'STRIPE' | 'MOCK'

// Subscription
type SubscriptionStatus = 'PENDING_PAYMENT' | 'ACTIVE' | 'EXPIRED' | 'CANCELLED'

// KOL
type Platform          = 'TIKTOK' | 'INSTAGRAM' | 'YOUTUBE' | 'FACEBOOK'
type Gender            = 'MALE' | 'FEMALE' | 'OTHER'
type PricingPackageType = 'POST' | 'VIDEO' | 'LIVESTREAM' | 'STORY' | 'COMBO'
type MediaType         = 'IMAGE' | 'VIDEO' | 'LINK'

// Notification
type NotificationType =
  | 'BOOKING_CREATED'    | 'BOOKING_ACCEPTED'   | 'BOOKING_REJECTED'
  | 'BOOKING_CANCELLED'  | 'BOOKING_IN_PROGRESS'| 'DELIVERABLE_SUBMITTED'
  | 'BOOKING_COMPLETED'  | 'BOOKING_DISPUTED'   | 'PAYMENT_SUCCESS'
  | 'REVIEW_RECEIVED'    | 'WITHDRAW_APPROVED'  | 'WITHDRAW_REJECTED'
  | 'PROFILE_APPROVED'   | 'PROFILE_REJECTED'   | 'NEW_MESSAGE'
```

**Category slugs (8 canonical):**
`beauty` · `fashion` · `food` · `lifestyle` · `travel` · `fitness` · `tech` · `entertainment`

---

## 6. Error Codes

```ts
type ErrorCode =
  | 'VALIDATION_FAILED'    // 400 — input không hợp lệ
  | 'RESOURCE_NOT_FOUND'   // 404 — không tìm thấy
  | 'BUSINESS_ERROR'       // 409/422 — vi phạm nghiệp vụ
  | 'UNAUTHORIZED'         // 401 — chưa đăng nhập / token sai
  | 'FORBIDDEN'            // 403 — không đủ quyền
  | 'INTERNAL_ERROR'       // 500 — lỗi server
  | 'EMAIL_ALREADY_EXISTS' // đăng ký email đã tồn tại
  | 'INVALID_CREDENTIALS'  // sai email/password
  | 'ACCOUNT_INACTIVE'     // tài khoản chưa kích hoạt
  | 'ACCOUNT_BANNED'       // tài khoản bị ban
  | 'TOKEN_INVALID'        // token không hợp lệ
  | 'TOKEN_EXPIRED'        // token hết hạn
  | 'TOKEN_USED'           // token đã dùng rồi
```

---

## 7. Endpoints Đầy Đủ

### 7.1 Auth

```
POST   /api/v1/auth/register         body: { email, password, role: Role }  → AuthTokens
POST   /api/v1/auth/login            body: { email, password }              → AuthTokens
POST   /api/v1/auth/refresh          body: { refreshToken }                 → AuthTokens
POST   /api/v1/auth/logout           body: { refreshToken }
POST   /api/v1/auth/verify-email     body: { token }
POST   /api/v1/auth/forgot-password  body: { email }
POST   /api/v1/auth/reset-password   body: { token, newPassword }
```

### 7.2 User Self-service

```
GET    /api/v1/users/me                → UserResponse
PATCH  /api/v1/users/me/deactivate     → status chuyển INACTIVE
DELETE /api/v1/users/me                → soft delete
```

### 7.3 KOL Discovery (public)

```
GET  /api/v1/kols/search          → ApiResponse<PageResponse<KolSummaryResponse>>
GET  /api/v1/kols/featured        query: limit=10
GET  /api/v1/kols/{slug}          → KolDetailResponse
```

**Params của `/kols/search`:**

| Param | Type | Ghi chú |
|-------|------|---------|
| `q` | string | Tìm theo tên / slug / bio |
| `categoryIds` | Long[] | Multi-select |
| `platforms` | Platform[] | Multi-select |
| `minFollower` / `maxFollower` | number | |
| `minPrice` / `maxPrice` | number | VND |
| `city` / `country` | string | |
| `gender` | Gender | |
| `minRating` | 1–5 | |
| `sort` | string | `featured`(default) · `rating` · `priceAsc` · `priceDesc` · `newest` |
| `page` | number | 0-based |
| `size` | number | Default 20 |

> Nếu gửi kèm JWT của BRAND, mỗi item trả thêm `isFavorite: boolean`.

### 7.4 KOL Profile Management (role: KOL)

```
GET    /api/v1/kols/me                       → KolProfileResponse
PUT    /api/v1/kols/me                       Cập nhật profile
POST   /api/v1/kols/me/submit                DRAFT → PENDING_REVIEW
POST   /api/v1/kols/me/channels              Thêm social channel
DELETE /api/v1/kols/me/channels/{id}
POST   /api/v1/kols/me/packages              Thêm pricing package
DELETE /api/v1/kols/me/packages/{id}
POST   /api/v1/kols/me/portfolio             Thêm portfolio item
DELETE /api/v1/kols/me/portfolio/{id}
```

### 7.5 Brand Profile & Favorites (role: BRAND)

```
GET    /api/v1/brands/me                         → BrandProfileResponse
PUT    /api/v1/brands/me                         Cập nhật profile
POST   /api/v1/brands/me/submit                  DRAFT → PENDING_REVIEW
POST   /api/v1/brands/me/favorites/{kolId}        Add favorite
DELETE /api/v1/brands/me/favorites/{kolId}        Remove favorite
GET    /api/v1/brands/me/favorites               → PageResponse<KolSummaryResponse>
```

### 7.6 Bookings

```
POST   /api/v1/bookings                           BRAND — tạo booking
GET    /api/v1/bookings/me?status=&page=&size=    BRAND — bookings của mình
GET    /api/v1/bookings/incoming?status=&page=    KOL   — bookings tới mình
GET    /api/v1/bookings/{id}                      BRAND/KOL — chi tiết
POST   /api/v1/bookings/{id}/cancel               BRAND — huỷ   body: { reason }
POST   /api/v1/bookings/{id}/accept               KOL   — accept
POST   /api/v1/bookings/{id}/reject               KOL   — reject body: { reason }
POST   /api/v1/bookings/{id}/deliverables         KOL   — submit deliverable
POST   /api/v1/bookings/{id}/approve-delivery     BRAND — approve → COMPLETED
POST   /api/v1/bookings/{id}/dispute              BRAND — mở tranh chấp body: { reason }
POST   /api/v1/bookings/{id}/messages             BRAND/KOL — gửi tin nhắn
GET    /api/v1/bookings/{id}/messages?page=&size= BRAND/KOL — lịch sử chat (default size=50)
```

### 7.7 Reviews

```
POST  /api/v1/bookings/{bookingId}/reviews          BRAND/KOL — tạo review (booking phải COMPLETED)
PUT   /api/v1/reviews/{reviewId}                    author    — sửa review
GET   /api/v1/users/{userId}/reviews?page=&size=    public    — reviews của user
GET   /api/v1/kols/{kolId}/reviews?page=&size=      public    — reviews của KOL (alias)
```

### 7.8 Payments

```
POST  /api/v1/payments/bookings/{bookingId}/checkout  BRAND  body: { provider: PaymentProvider }
                                                             → { paymentUrl } để redirect
GET   /api/v1/payments/bookings/{bookingId}           BRAND/KOL — trạng thái payment
GET   /api/v1/payments/webhook/{provider}             public — mock webhook dev
                                                       query: externalRef=&amount=&status=PAID
```

### 7.9 Wallet

```
GET  /api/v1/wallet/me                              → { balanceAvailable, balanceHeld, currency }
GET  /api/v1/wallet/me/transactions?page=&size=     → PageResponse<WalletTransactionResponse>
```

### 7.10 Withdrawals

```
POST  /api/v1/withdraws                             KOL   — tạo yêu cầu rút
GET   /api/v1/withdraws/me?page=&size=              KOL   — lịch sử rút
GET   /api/v1/withdraws/admin?status=&page=&size=   ADMIN — queue rút
POST  /api/v1/withdraws/admin/{id}/approve          ADMIN
POST  /api/v1/withdraws/admin/{id}/paid             ADMIN — đánh dấu đã chuyển khoản
POST  /api/v1/withdraws/admin/{id}/reject           ADMIN body: { reason }
```

### 7.11 Notifications

```
GET   /api/v1/notifications/me?unreadOnly=&page=&size=   → PageResponse<NotificationResponse>
GET   /api/v1/notifications/me/unread-count              → { count: number }
PATCH /api/v1/notifications/{id}/read
POST  /api/v1/notifications/me/read-all                  → { updated: number }
```

### 7.12 Categories (public)

```
GET    /api/v1/categories                 → tree categories
POST   /api/v1/admin/categories           ADMIN — tạo
PUT    /api/v1/admin/categories/{id}      ADMIN — sửa
DELETE /api/v1/admin/categories/{id}      ADMIN — xoá
```

### 7.13 File Upload

```
POST  /api/v1/files/upload   multipart/form-data, field: "file"
                             → { url: string }
```

Files được serve tại `/uploads/**` (no auth).

### 7.14 Plans & Subscriptions

```
GET  /api/v1/plans?targetRole=KOL|BRAND  public — danh sách gói active
GET  /api/v1/plans/{code}                public — chi tiết gói

GET  /api/v1/subscriptions/me                                 → SubscriptionResponse
GET  /api/v1/subscriptions/me/history                         → SubscriptionResponse[]
POST /api/v1/subscriptions/checkout   body: { planCode, provider, autoRenew }
                                      free plan → ACTIVE ngay
                                      paid plan → PENDING_PAYMENT + { paymentUrl }
POST /api/v1/subscriptions/{id}/cancel?reason=
     404 nếu không tồn tại | 403 nếu không phải owner | 409 nếu đã CANCELLED/EXPIRED
```

### 7.15 Admin — Profile Review

```
GET  /api/v1/admin/kols?status=PENDING_REVIEW&page=&size=     → paged list
POST /api/v1/admin/kols/{id}/approve
POST /api/v1/admin/kols/{id}/reject    body: { reason }

GET  /api/v1/admin/brands?status=PENDING_REVIEW&page=&size=
POST /api/v1/admin/brands/{id}/approve
POST /api/v1/admin/brands/{id}/reject  body: { reason }
```

Response item gồm: `id`, `userId`, `displayName` / `companyName`, `slug` / `industry`,
`status`, `avgRating`, `reviewCount`, `rejectReason`, `createdAt`.

### 7.16 Admin — Users

```
GET  /api/v1/admin/users?q=&role=&page=&size=   → AdminUserResponse[]
     fields: id, email, role, status, emailVerified, createdAt
POST /api/v1/admin/users/{id}/ban               → status: BANNED
POST /api/v1/admin/users/{id}/unban             → status: ACTIVE
```

### 7.17 Admin — Bookings & Disputes

```
GET  /api/v1/admin/bookings?status=&page=&size=
POST /api/v1/admin/bookings/{id}/resolve-dispute
     body: { decision: 'REFUND_BRAND' | 'RELEASE_KOL', note }
```

### 7.18 Admin — Stats & Audit

```
GET  /api/v1/admin/stats/overview
GET  /api/v1/admin/stats/bookings?from=&to=      default 365 ngày
GET  /api/v1/admin/stats/top-kols?limit=10
GET  /api/v1/admin/stats/revenue?from=&to=
GET  /api/v1/admin/audit-logs?page=&size=50
```

---

## 8. Booking State Machine

```
                    KOL reject
                ┌──────────────→ REJECTED
                │
PENDING ────────┤   KOL accept
                └──────────────→ ACCEPTED ──── BRAND thanh toán ──→ IN_PROGRESS
    │                                                                      │
    │ BRAND huỷ                                                   KOL submit deliverable
    ↓                                                                      │
CANCELLED                                                                  ↓
                                                                      DELIVERED
                                                                      │        │
                                                             BRAND approve   BRAND dispute
                                                                      │        │
                                                                 COMPLETED  DISPUTED
                                                                              │
                                                                        Admin resolve
                                                                      ↙          ↘
                                                               COMPLETED       CANCELLED_BY_ADMIN
```

**Hành động hợp lệ theo state:**

| State | BRAND có thể | KOL có thể |
|-------|-------------|-----------|
| `PENDING` | Huỷ (`cancel`) | Accept / Reject |
| `ACCEPTED` | Thanh toán (`checkout`) | — |
| `IN_PROGRESS` | — | Submit deliverable |
| `DELIVERED` | Approve / Dispute | — |
| `DISPUTED` | — | — (chờ admin) |

---

## 9. Business Rules Quan Trọng

### Phí platform (10%)

- Budget BRAND trả = `amount`
- KOL nhận thực tế = `amount × 0.9`
- Platform giữ = `amount × 0.1` (ghi vào `wallet_transaction` với type `FEE`)

### Escrow wallet

- Khi BRAND thanh toán → tiền chuyển vào `balanceHeld` (không dùng được)
- Khi booking `COMPLETED` → tiền release vào `balanceAvailable` của KOL
- `balanceHeld` = tổng tiền đang bị giữ bởi các booking đang chạy

### Profile submit flow

```
DRAFT / REJECTED → (submit) → PENDING_REVIEW → (admin approve) → APPROVED
                                              → (admin reject)  → REJECTED
```

Chỉ profile `APPROVED` mới hiện trên `/kols/search`.

### Subscription

- **Free plan** (`price = 0`): ACTIVE ngay, `expiresAt = now + durationDays`
- **Paid plan**: trả về `paymentUrl` → redirect để thanh toán
  - Dev: GET `paymentUrl` (mock webhook) để tự confirm thanh toán
  - Status sau thanh toán thành công: `ACTIVE`

### Pagination

- Tất cả endpoint paged dùng `page` **0-based**, default `size=20`
- Booking messages: default `size=50`
- Dùng `hasNext` / `hasPrevious` để điều khiển nút prev/next

---

## 10. Seed Accounts

> **Password chung:** `password123`

### Brand (4 tài khoản — profile APPROVED, ví ~150–200M VND)

| Email | Công ty |
|-------|---------|
| `brand.vinamilk@seed.local` | Vinamilk |
| `brand.shopee@seed.local` | Shopee Vietnam |
| `brand.tch@seed.local` | The Coffee House |
| `brand.bitis@seed.local` | Biti's |

### KOL APPROVED (8 tài khoản)

| Email | Tên | Ghi chú |
|-------|-----|---------|
| `thanhnha25091@seed.local` | Nguyễn Thị Thanh Nhã | |
| `lambaongoc@seed.local` | Lâm Bảo Ngọc | Withdraw PENDING |
| `tien.tien@seed.local` | Tiên Tiên | Withdraw REJECTED |
| `hoangsoi2809@seed.local` | Hoàng Sói | |
| `phuongdidau@seed.local` | Phương Đi Đâu | Withdraw APPROVED |
| `tebefood@seed.local` | Tebe Food | |
| `tuandidau@seed.local` | Tuấn Đi Đâu | |
| `hoangduyen.dreams@seed.local` | Hoàng Duyên | Withdraw PAID |

### KOL PENDING_REVIEW (2 tài khoản — để test màn hình admin duyệt)

| Email | Tên |
|-------|-----|
| `kol.submitted1@seed.local` | Nguyễn Đăng Khoa |
| `kol.submitted2@seed.local` | Trần Mỹ Anh |

### Admin

Không seed sẵn. Tạo thủ công:
```sql
UPDATE app_user SET role = 'ADMIN' WHERE email = '<email>';
```

### Booking states đã seed

| State | Campaign mẫu |
|-------|--------------|
| `PENDING` | Vinamilk Summer Sport 2026 |
| `ACCEPTED` | Shopee 5.5 Music Theme |
| `REJECTED` | TCH Acoustic Night |
| `CANCELLED` | Bitis Trekking Series |
| `IN_PROGRESS` | Vinamilk Probi Daily · Shopee Kitchen Reno |
| `DELIVERED` | TCH Hanoi Weekend |
| `COMPLETED` | Vinamilk Athlete Drive 2026 |

---

## 11. Gợi Ý Cấu Trúc API Client (TypeScript)

```ts
// lib/api.ts
import axios from 'axios'

export const api = axios.create({
  baseURL: 'http://localhost:8080/api/v1',
})

// Tự gắn token
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// Xử lý lỗi tập trung
api.interceptors.response.use(
  (res) => res,
  async (error) => {
    const { errorCode } = error.response?.data ?? {}
    if (errorCode === 'UNAUTHORIZED' || errorCode === 'TOKEN_EXPIRED') {
      // TODO: thử refresh token, nếu thất bại → redirect /login
    }
    return Promise.reject(error)
  }
)

// Helper lấy data an toàn
export const unwrap = <T>(res: { data: ApiResponse<T> }): T => {
  if (!res.data.success) throw new Error(res.data.message ?? 'Unknown error')
  return res.data.data as T
}
```

---

## 12. Checklist Tích Hợp

- [ ] Wrap mọi API call trong `try/catch`, đọc `errorCode` trước khi đọc `message`
- [ ] Hiển thị `message` từ server thay vì hardcode thông báo lỗi
- [ ] `currentPage` truyền vào API là 0-based; hiển thị cho user là 1-based (`currentPage + 1`)
- [ ] Endpoint `/kols/search` gửi kèm token nếu user là BRAND để nhận `isFavorite`
- [ ] Sau `POST /me/submit` (KOL/Brand): disable nút, hiện trạng thái "Đang chờ duyệt"
- [ ] Booking state DELIVERED: hiện đồng thời nút "Approve" và "Dispute" cho BRAND
- [ ] Wallet: `balanceAvailable` mới là số tiền có thể rút; `balanceHeld` chỉ hiển thị informational
- [ ] Dev payment: sau `checkout` nhận `paymentUrl` → GET URL đó trong browser để mock confirm
