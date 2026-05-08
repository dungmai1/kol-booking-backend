# KOL Booking — Frontend Integration Guide

> **Base URL:** `http://localhost:8080/api/v1`  
> **Auth:** JWT Bearer Token — `Authorization: Bearer <accessToken>`  
> **Platform Fee:** 10% | **Access Token TTL:** 15 min | **Refresh Token TTL:** 7 ngày

---

## Mục lục

1. [Common Response Format](#1-common-response-format)
2. [Authentication](#2-authentication)
3. [KOL Profile](#3-kol-profile)
4. [KOL Search & Discovery](#4-kol-search--discovery)
5. [Brand Profile](#5-brand-profile)
6. [Brand Favorites](#6-brand-favorites)
7. [Bookings](#7-bookings)
8. [Reviews](#8-reviews)
9. [Payments](#9-payments)
10. [Wallet](#10-wallet)
11. [Withdrawals](#11-withdrawals)
12. [Notifications](#12-notifications)
13. [Categories](#13-categories)
14. [File Upload](#14-file-upload)
15. [Admin](#15-admin)
16. [Enums Reference](#16-enums-reference)
17. [Business Logic & Flows](#17-business-logic--flows)

---

## 1. Common Response Format

Mọi response đều bọc trong `ApiResponse<T>`:

```json
{
  "success": true,
  "data": { ... },
  "message": null,
  "errorCode": null
}
```

**Error Response:**
```json
{
  "success": false,
  "data": null,
  "message": "Mô tả lỗi",
  "errorCode": "ERROR_CODE"
}
```

**Paginated Response (`PageResponse<T>`):**
```json
{
  "content": [ ... ],
  "totalElements": 100,
  "totalPages": 5,
  "currentPage": 0,
  "pageSize": 20,
  "hasNext": true,
  "hasPrevious": false
}
```

**HTTP Status Codes:**

| Code | Ý nghĩa |
|------|---------|
| 200 | Thành công (GET, PUT, PATCH) |
| 201 | Tạo mới thành công (POST) |
| 204 | Xóa thành công (DELETE) |
| 400 | Request không hợp lệ |
| 401 | Token thiếu hoặc hết hạn |
| 403 | Không có quyền truy cập |
| 404 | Không tìm thấy resource |
| 409 | Xung đột (email đã tồn tại...) |
| 500 | Lỗi server |

---

## 2. Authentication

Tất cả các endpoint `/auth/**` đều **không yêu cầu xác thực**.

### POST `/auth/register`

Đăng ký tài khoản mới.

**Request:**
```json
{
  "email": "user@example.com",
  "password": "12345678",
  "role": "BRAND"
}
```
> `role`: `"BRAND"` hoặc `"KOL"`  
> `password`: 8–100 ký tự

**Response:** `AuthTokens`
```json
{
  "userId": 1,
  "email": "user@example.com",
  "role": "BRAND",
  "accessToken": "eyJ...",
  "refreshToken": "abc123...",
  "accessTokenExpiresInSeconds": 900
}
```

---

### POST `/auth/login`

**Request:**
```json
{
  "email": "user@example.com",
  "password": "12345678"
}
```

**Response:** `AuthTokens` (giống register)

---

### POST `/auth/refresh`

Làm mới access token.

**Request:**
```json
{ "refreshToken": "abc123..." }
```

**Response:** `AuthTokens`

---

### POST `/auth/logout`

**Request:**
```json
{ "refreshToken": "abc123..." }
```

---

### POST `/auth/verify-email`

**Request:**
```json
{ "token": "email-verify-token" }
```

---

### POST `/auth/forgot-password`

**Request:**
```json
{ "email": "user@example.com" }
```

---

### POST `/auth/reset-password`

**Request:**
```json
{
  "token": "reset-token",
  "newPassword": "newpass123"
}
```

---

## 3. KOL Profile

> Yêu cầu role `KOL` (trừ endpoint public).

### GET `/kols/me` — Lấy profile của KOL đang đăng nhập

**Response:** `KolProfileResponse`
```json
{
  "id": 1,
  "userId": 10,
  "displayName": "Nguyen Van A",
  "slug": "nguyen-van-a",
  "avatarUrl": "https://...",
  "coverUrl": "https://...",
  "bio": "Lifestyle creator...",
  "gender": "MALE",
  "dateOfBirth": "1995-05-10",
  "city": "Ho Chi Minh",
  "country": "VN",
  "status": "APPROVED",
  "avgRating": 4.75,
  "reviewCount": 12,
  "rejectReason": null,
  "categoryIds": [1, 3],
  "channels": [ ... ],
  "pricingPackages": [ ... ],
  "portfolio": [ ... ],
  "createdAt": "2025-01-01T00:00:00Z",
  "updatedAt": "2025-06-01T00:00:00Z"
}
```

---

### PUT `/kols/me` — Cập nhật profile

**Request:**
```json
{
  "displayName": "Nguyen Van A",
  "slug": "nguyen-van-a",
  "avatarUrl": "https://...",
  "coverUrl": "https://...",
  "bio": "...",
  "gender": "MALE",
  "dateOfBirth": "1995-05-10",
  "city": "Ho Chi Minh",
  "country": "VN",
  "categoryIds": [1, 3]
}
```
> Tất cả field đều optional (chỉ gửi field cần thay đổi).  
> `slug`: chỉ chữ thường, số, dấu gạch ngang, tối đa 150 ký tự.

**Response:** `KolProfileResponse`

---

### POST `/kols/me/submit` — Nộp profile để Admin duyệt

Không cần request body.

**Response:** `KolProfileResponse` (status → `SUBMITTED`)

---

### GET `/kols/{slug}` — Xem profile KOL công khai *(public)*

**Response:** `KolPublicResponse` (tương tự `KolProfileResponse`)

---

### Quản lý kênh mạng xã hội

#### POST `/kols/me/channels`
```json
{
  "platform": "TIKTOK",
  "url": "https://tiktok.com/@username",
  "username": "username",
  "followerCount": 100000,
  "engagementRate": 5.5,
  "verified": false
}
```
> `platform`: `TIKTOK` | `INSTAGRAM` | `YOUTUBE` | `FACEBOOK`

**Response:** `KolSocialChannelResponse`
```json
{
  "id": 1,
  "platform": "TIKTOK",
  "url": "https://tiktok.com/@username",
  "username": "username",
  "followerCount": 100000,
  "engagementRate": 5.5,
  "verified": false
}
```

#### DELETE `/kols/me/channels/{id}`

---

### Quản lý gói dịch vụ

#### POST `/kols/me/packages`
```json
{
  "type": "VIDEO",
  "platform": "YOUTUBE",
  "price": 5000000,
  "description": "Video review 5-10 phút"
}
```
> `type`: `POST` | `STORY` | `VIDEO` | `SHOUTOUT` | `LONG_FORM` | `CUSTOM`

**Response:** `KolPricingPackageResponse`
```json
{
  "id": 1,
  "type": "VIDEO",
  "platform": "YOUTUBE",
  "price": 5000000,
  "description": "Video review 5-10 phút"
}
```

#### DELETE `/kols/me/packages/{id}`

---

### Quản lý portfolio

#### POST `/kols/me/portfolio`
```json
{
  "title": "Campaign ABC 2024",
  "mediaUrl": "https://...",
  "mediaType": "VIDEO",
  "campaignName": "ABC Brand Campaign"
}
```
> `mediaType`: `IMAGE` | `VIDEO`

**Response:** `KolPortfolioItemResponse`
```json
{
  "id": 1,
  "title": "Campaign ABC 2024",
  "mediaUrl": "https://...",
  "mediaType": "VIDEO",
  "campaignName": "ABC Brand Campaign"
}
```

#### DELETE `/kols/me/portfolio/{id}`

---

## 4. KOL Search & Discovery

*(Public — không cần đăng nhập)*

### GET `/kols/search`

**Query Parameters:**

| Param | Type | Mô tả |
|-------|------|-------|
| `q` | string | Tìm theo tên |
| `categoryIds` | `Long[]` | Lọc theo danh mục |
| `platforms` | `string[]` | `TIKTOK`, `INSTAGRAM`, `YOUTUBE`, `FACEBOOK` |
| `minFollower` | Long | Số follower tối thiểu |
| `maxFollower` | Long | Số follower tối đa |
| `minPrice` | BigDecimal | Giá tối thiểu |
| `maxPrice` | BigDecimal | Giá tối đa |
| `city` | string | Thành phố |
| `country` | string | Quốc gia |
| `gender` | string | `MALE` \| `FEMALE` \| `OTHER` |
| `minRating` | BigDecimal | Rating tối thiểu |
| `sort` | string | `featured` (default) \| `rating` \| `price_asc` \| `price_desc` \| `followers` |
| `page` | int | Trang (default 0) |
| `size` | int | Số phần tử/trang (default 20) |

**Response:** `PageResponse<KolSummaryResponse>`
```json
{
  "id": 1,
  "displayName": "Nguyen Van A",
  "slug": "nguyen-van-a",
  "avatarUrl": "https://...",
  "city": "Ho Chi Minh",
  "country": "VN",
  "avgRating": 4.75,
  "reviewCount": 12,
  "maxFollowerCount": 500000,
  "minPrice": 1000000
}
```

---

### GET `/kols/featured?limit=10`

**Response:** `List<KolSummaryResponse>`

---

## 5. Brand Profile

> Yêu cầu role `BRAND`.

### GET `/brands/me`

**Response:** `BrandProfileResponse`
```json
{
  "id": 1,
  "userId": 5,
  "companyName": "ABC Company",
  "taxCode": "0123456789",
  "industry": "Fashion",
  "logoUrl": "https://...",
  "website": "https://abc.com",
  "contactName": "Nguyen Thi B",
  "contactPhone": "0901234567",
  "address": "123 Nguyen Hue, Q1, TPHCM",
  "status": "APPROVED",
  "rejectReason": null,
  "createdAt": "2025-01-01T00:00:00Z",
  "updatedAt": "2025-06-01T00:00:00Z"
}
```

---

### PUT `/brands/me`

**Request:**
```json
{
  "companyName": "ABC Company",
  "taxCode": "0123456789",
  "industry": "Fashion",
  "logoUrl": "https://...",
  "website": "https://abc.com",
  "contactName": "Nguyen Thi B",
  "contactPhone": "0901234567",
  "address": "123 Nguyen Hue, Q1, TPHCM"
}
```

---

### POST `/brands/me/submit` — Nộp profile để Admin duyệt

Không cần request body.

---

## 6. Brand Favorites

> Yêu cầu role `BRAND`.

### POST `/brands/me/favorites/{kolId}` — Thêm vào yêu thích

### DELETE `/brands/me/favorites/{kolId}` — Xóa khỏi yêu thích

### GET `/brands/me/favorites` — Danh sách KOL yêu thích

**Query:** `page`, `size`

**Response:** `PageResponse<KolSummaryResponse>`

---

## 7. Bookings

### POST `/bookings` — Tạo booking *(BRAND)*

**Request:**
```json
{
  "kolProfileId": 1,
  "campaignTitle": "Chiến dịch hè 2025",
  "campaignBrief": "Mô tả chi tiết chiến dịch...",
  "deliverables": "[{\"type\":\"VIDEO\",\"platform\":\"TIKTOK\",\"quantity\":3}]",
  "budget": 10000000,
  "startDate": "2025-07-01",
  "endDate": "2025-07-31"
}
```

**Response:** `BookingResponse`
```json
{
  "id": 1,
  "brandProfileId": 1,
  "kolProfileId": 2,
  "campaignTitle": "Chiến dịch hè 2025",
  "campaignBrief": "...",
  "deliverables": "...",
  "budget": 10000000,
  "startDate": "2025-07-01",
  "endDate": "2025-07-31",
  "status": "PENDING",
  "rejectReason": null,
  "cancelReason": null,
  "invoiceUrl": null,
  "createdAt": "2025-06-01T08:00:00Z",
  "updatedAt": "2025-06-01T08:00:00Z"
}
```

---

### GET `/bookings/me` — Danh sách booking đã tạo *(BRAND)*

**Query:** `page`, `size`

**Response:** `PageResponse<BookingResponse>`

---

### GET `/bookings/incoming` — Booking được gửi đến *(KOL)*

**Query:** `page`, `size`

**Response:** `PageResponse<BookingResponse>`

---

### GET `/bookings/{id}` — Chi tiết booking

---

### POST `/bookings/{id}/cancel` — Huỷ booking *(BRAND)*

**Request (optional):**
```json
{ "reason": "Thay đổi kế hoạch" }
```

---

### POST `/bookings/{id}/accept` — Chấp nhận booking *(KOL)*

Không cần request body.

---

### POST `/bookings/{id}/reject` — Từ chối booking *(KOL)*

**Request (optional):**
```json
{ "reason": "Không phù hợp lĩnh vực" }
```

---

### POST `/bookings/{id}/deliverables` — Nộp deliverable *(KOL)*

**Request:**
```json
{
  "deliverableId": 1,
  "submittedUrl": "https://tiktok.com/video/...",
  "note": "Đã đăng video theo brief"
}
```

---

### POST `/bookings/{id}/approve-delivery` — Xác nhận nhận hàng *(BRAND)*

Không cần request body. Booking → `COMPLETED`.

---

### POST `/bookings/{id}/dispute` — Tranh chấp *(BRAND)*

**Request (optional):**
```json
{ "reason": "KOL không thực hiện đúng brief" }
```

---

### Tin nhắn booking

#### POST `/bookings/{id}/messages` — Gửi tin nhắn

**Request:**
```json
{
  "content": "Nội dung tin nhắn...",
  "attachmentUrl": "https://..."
}
```
> `content`: 1–4000 ký tự

**Response:** `BookingMessageResponse`
```json
{
  "id": 1,
  "bookingId": 1,
  "senderUserId": 5,
  "content": "Nội dung tin nhắn...",
  "attachmentUrl": null,
  "createdAt": "2025-06-01T09:00:00Z"
}
```

#### GET `/bookings/{id}/messages` — Lấy lịch sử tin nhắn

**Query:** `page`, `size`

**Response:** `PageResponse<BookingMessageResponse>`

---

## 8. Reviews

### POST `/bookings/{bookingId}/reviews` — Viết đánh giá *(BRAND hoặc KOL)*

Chỉ thực hiện được sau khi booking `COMPLETED`.

**Request:**
```json
{
  "rating": 5,
  "comment": "Rất chuyên nghiệp, đúng deadline!"
}
```
> `rating`: 1–5

**Response:** `ReviewResponse`
```json
{
  "id": 1,
  "bookingId": 1,
  "authorId": 5,
  "targetId": 10,
  "direction": "TO_KOL",
  "rating": 5,
  "comment": "Rất chuyên nghiệp!",
  "createdAt": "2025-07-01T10:00:00Z",
  "updatedAt": "2025-07-01T10:00:00Z"
}
```

---

### PUT `/reviews/{reviewId}` — Chỉnh sửa đánh giá

**Request:** Giống `ReviewCreateRequest`

---

### GET `/users/{userId}/reviews` *(public)*

**Query:** `page`, `size`

**Response:** `PageResponse<ReviewResponse>`

---

## 9. Payments

### POST `/payments/bookings/{bookingId}/checkout` — Tạo thanh toán *(BRAND)*

**Request (optional):**
```json
{ "provider": "MOCK" }
```
> `provider`: `VNPAY` | `MOMO` | `STRIPE` | `MOCK`

**Response:** `CheckoutResponse`
```json
{
  "paymentOrderId": 1,
  "bookingId": 1,
  "amount": 10000000,
  "provider": "MOCK",
  "status": "PENDING",
  "paymentUrl": "https://payment-gateway.com/pay?ref=abc",
  "externalRef": "abc123"
}
```

---

### GET `/payments/bookings/{bookingId}` — Trạng thái thanh toán *(public)*

**Response:** `CheckoutResponse`

---

### POST `/payments/webhook/{provider}` *(public)*

Webhook nhận từ cổng thanh toán. Frontend không cần gọi trực tiếp.

---

## 10. Wallet

### GET `/wallet/me` — Số dư ví

**Response:** `WalletResponse`
```json
{
  "id": 1,
  "userId": 5,
  "balanceAvailable": 8500000,
  "balanceHeld": 0,
  "currency": "VND",
  "createdAt": "2025-01-01T00:00:00Z",
  "updatedAt": "2025-07-01T00:00:00Z"
}
```

---

### GET `/wallet/me/transactions` — Lịch sử giao dịch

**Query:** `page`, `size`

**Response:** `Page<WalletTransactionResponse>`
```json
{
  "id": 1,
  "walletId": 1,
  "type": "BOOKING_PAYMENT",
  "amount": 10000000,
  "balanceAfter": 8500000,
  "bookingId": 1,
  "externalRef": "abc123",
  "status": "SUCCESS",
  "note": "Thanh toán booking #1",
  "createdAt": "2025-07-01T10:00:00Z"
}
```

---

## 11. Withdrawals

### POST `/withdraws` — Yêu cầu rút tiền *(KOL)*

**Request:**
```json
{
  "amount": 5000000,
  "bankName": "Vietcombank",
  "bankAccount": "1234567890",
  "accountName": "NGUYEN VAN A"
}
```
> `amount` >= 1

**Response:** `WithdrawResponse`
```json
{
  "id": 1,
  "kolUserId": 10,
  "amount": 5000000,
  "bankName": "Vietcombank",
  "bankAccount": "1234567890",
  "accountName": "NGUYEN VAN A",
  "status": "PENDING",
  "rejectReason": null,
  "createdAt": "2025-07-01T10:00:00Z",
  "processedAt": null
}
```

---

### GET `/withdraws/me` — Lịch sử rút tiền *(KOL)*

**Query:** `page`, `size`

---

## 12. Notifications

### GET `/notifications/me` — Danh sách thông báo

**Query:** `page`, `size`, `unreadOnly` (boolean)

**Response:** `Page<NotificationResponse>`
```json
{
  "id": 1,
  "userId": 5,
  "type": "BOOKING_ACCEPTED",
  "title": "Booking được chấp nhận",
  "message": "KOL Nguyen Van A đã chấp nhận booking của bạn.",
  "link": "/bookings/1",
  "readAt": null,
  "createdAt": "2025-07-01T10:00:00Z"
}
```

---

### GET `/notifications/me/unread-count` — Số thông báo chưa đọc

**Response:**
```json
{ "count": 3 }
```

---

### PATCH `/notifications/{id}/read` — Đánh dấu đã đọc

**Response:** `NotificationResponse`

---

### POST `/notifications/me/read-all` — Đọc tất cả

**Response:**
```json
{ "updated": 5 }
```

---

## 13. Categories

### GET `/categories` *(public)*

**Response:** `List<CategoryResponse>`
```json
[
  {
    "id": 1,
    "name": "Thời trang",
    "slug": "thoi-trang",
    "parentId": null,
    "children": [
      {
        "id": 5,
        "name": "Thời trang nữ",
        "slug": "thoi-trang-nu",
        "parentId": 1,
        "children": []
      }
    ]
  }
]
```

---

## 14. File Upload

### POST `/files/upload` *(public)*

Upload file ảnh/video. Gửi dưới dạng `multipart/form-data`.

**Form field:** `file` (tối đa 20MB/file, 100MB/request)

**Response:**
```json
{ "url": "/uploads/2025/07/abc123.jpg" }
```

> URL trả về dùng trực tiếp cho các field như `avatarUrl`, `mediaUrl`, `attachmentUrl`...

---

## 15. Admin

> Tất cả endpoint `/admin/**` yêu cầu role `ADMIN`.

### Quản lý Users

#### GET `/admin/users?q=&role=&page=&size=` — Tìm kiếm users

**Response:** `Page<AdminUserResponse>`

#### POST `/admin/users/{id}/ban` — Khoá tài khoản

#### POST `/admin/users/{id}/unban` — Mở khoá

---

### Duyệt KOL Profile

#### GET `/admin/kols?status=SUBMITTED&page=&size=`

#### POST `/admin/kols/{id}/approve`

#### POST `/admin/kols/{id}/reject`
```json
{ "reason": "Thiếu thông tin kênh mạng xã hội" }
```

---

### Duyệt Brand Profile

#### GET `/admin/brands?status=SUBMITTED&page=&size=`

#### POST `/admin/brands/{id}/approve`

#### POST `/admin/brands/{id}/reject`
```json
{ "reason": "Mã số thuế không hợp lệ" }
```

---

### Quản lý Bookings

#### GET `/admin/bookings?status=DISPUTED&page=&size=`

#### POST `/admin/bookings/{id}/resolve-dispute`
```json
{
  "resolution": "REFUND_TO_BRAND",
  "amount": 5000000,
  "note": "KOL không thực hiện đúng brief"
}
```
> `resolution`: `REFUND_TO_BRAND` | `PAY_KOL` | `PARTIAL_REFUND`

---

### Quản lý Danh mục

#### POST `/admin/categories`
```json
{
  "name": "Làm đẹp",
  "slug": "lam-dep",
  "parentId": null
}
```

#### PUT `/admin/categories/{id}`

#### DELETE `/admin/categories/{id}`

---

### Thống kê

#### GET `/admin/stats/overview`
```json
{
  "totalUsers": 500,
  "totalKols": 200,
  "totalBrands": 300,
  "totalBookings": 1200,
  "totalRevenue": 500000000,
  "activeBookings": 45
}
```

#### GET `/admin/stats/bookings` — Booking theo tháng (365 ngày gần nhất)
```json
[{ "month": "2025-06", "count": 42, "revenue": 120000000 }]
```

#### GET `/admin/stats/top-kols` — Top KOL
```json
[{ "id": 1, "displayName": "...", "earnings": 50000000, "bookingCount": 15, "avgRating": 4.9 }]
```

#### GET `/admin/stats/revenue` — Doanh thu theo tháng
```json
[{ "month": "2025-06", "platformFee": 12000000, "totalPayments": 120000000 }]
```

---

### Withdrawals (Admin)

#### GET `/withdraws/admin?status=PENDING&page=&size=`

#### POST `/withdraws/admin/{id}/approve`

#### POST `/withdraws/admin/{id}/paid`

#### POST `/withdraws/admin/{id}/reject`
```json
{ "reason": "Thông tin ngân hàng không hợp lệ" }
```

---

## 16. Enums Reference

### Role
`ADMIN` | `BRAND` | `KOL`

### UserStatus
`PENDING_VERIFICATION` | `ACTIVE` | `BANNED`

### ProfileStatus (KOL & Brand)
`DRAFT` | `SUBMITTED` | `APPROVED` | `REJECTED`

### BookingStatus
| Status | Mô tả |
|--------|-------|
| `PENDING` | Brand vừa tạo, chờ KOL phản hồi |
| `ACCEPTED` | KOL chấp nhận |
| `REJECTED` | KOL từ chối |
| `CANCELLED` | Brand huỷ |
| `IN_PROGRESS` | Đang thực hiện (sau khi thanh toán) |
| `DELIVERED` | KOL đã nộp deliverable |
| `COMPLETED` | Brand xác nhận, hoàn thành |
| `DISPUTED` | Brand tranh chấp |
| `CANCELLED_BY_ADMIN` | Admin huỷ |

### Platform
`TIKTOK` | `INSTAGRAM` | `YOUTUBE` | `FACEBOOK`

### PricingPackageType
`POST` | `STORY` | `VIDEO` | `SHOUTOUT` | `LONG_FORM` | `CUSTOM`

### MediaType
`IMAGE` | `VIDEO`

### Gender
`MALE` | `FEMALE` | `OTHER`

### PaymentProvider
`VNPAY` | `MOMO` | `STRIPE` | `MOCK`

### PaymentOrderStatus
`PENDING` | `SUCCESS` | `FAILED` | `CANCELLED`

### TransactionType
`DEPOSIT` | `WITHDRAWAL` | `BOOKING_PAYMENT` | `REFUND` | `PLATFORM_FEE`

### WithdrawStatus
`PENDING` | `APPROVED` | `PAID` | `REJECTED`

### ReviewDirection
`TO_KOL` | `TO_BRAND`

### NotificationType
`BOOKING_CREATED` | `BOOKING_ACCEPTED` | `BOOKING_REJECTED` | `BOOKING_CANCELLED` | `BOOKING_IN_PROGRESS` | `DELIVERABLE_SUBMITTED` | `BOOKING_COMPLETED` | `BOOKING_DISPUTED` | `PAYMENT_SUCCESS` | `REVIEW_RECEIVED` | `WITHDRAW_APPROVED` | `WITHDRAW_REJECTED` | `PROFILE_APPROVED` | `PROFILE_REJECTED` | `NEW_MESSAGE`

---

## 17. Business Logic & Flows

### Authentication Flow

```
Register → [verify email] → Login → dùng accessToken
             ↓
     accessToken hết hạn (15 phút)
             ↓
     POST /auth/refresh → accessToken mới
             ↓
     Logout → refreshToken bị thu hồi
```

### KOL/Brand Profile Flow

```
Đăng ký → Tạo/chỉnh sửa profile → Submit → Admin duyệt → APPROVED
                                                        ↓
                                                     REJECTED (có lý do)
                                                        ↓
                                                 Sửa và submit lại
```

> Brand và KOL phải có profile `APPROVED` mới tạo/nhận booking được.

### Booking Flow

```
Brand tạo booking (PENDING)
    ↓
KOL accept (ACCEPTED) hoặc reject (REJECTED)
    ↓ [Brand thanh toán]
IN_PROGRESS
    ↓
KOL nộp deliverable (DELIVERED)
    ↓
Brand approve (COMPLETED) hoặc dispute (DISPUTED)
    ↓ [nếu COMPLETED]
Brand + KOL viết review cho nhau
```

> **Booking tự động hết hạn** (PENDING → huỷ) sau 7 ngày nếu KOL không phản hồi.  
> **Booking tự động hoàn thành** sau 3 ngày nếu Brand không approve/dispute sau khi KOL nộp hàng.

### Payment & Wallet Flow

```
Brand checkout → Tạo PaymentOrder + URL
    ↓
Brand thanh toán trên cổng (VNPAY/MOMO/STRIPE/MOCK)
    ↓
Webhook → PaymentOrder SUCCESS → Booking → IN_PROGRESS
    ↓
Booking COMPLETED → Platform giữ 10% phí
                  → KOL nhận 90% vào wallet (balanceAvailable)
```

### Withdraw Flow (KOL)

```
KOL tạo withdraw request (PENDING)
    ↓ [amount trừ vào balanceAvailable → balanceHeld]
Admin APPROVED
    ↓
Admin chuyển khoản xong → PAID
                        ↓
                    KOL nhận tiền thực
```

---

*Tài liệu được tạo tự động từ source code. Cập nhật lần cuối: 2026-04-21.*
