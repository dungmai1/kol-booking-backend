# Phase 06 — Thanh toán

**Mục tiêu**: Brand thanh toán trước (escrow), hệ thống giữ tiền, giải ngân cho KOL sau khi hoàn tất booking. Có hoá đơn, lịch sử giao dịch.

## Checklist

### 6.1. Mô hình escrow
```
Brand thanh toán → tiền vào ví hệ thống (HELD)
   → Booking COMPLETED → chuyển tiền sang ví KOL (AVAILABLE)
   → KOL yêu cầu rút → chuyển về bank account KOL (WITHDRAWN)
```

Nếu `DISPUTED` → tiền giữ nguyên trạng thái `HELD` cho đến khi admin xử lý.

### 6.2. Entity & Migration

**`Wallet`** (1-1 với AppUser):
- `balanceAvailable`, `balanceHeld`
- `currency` (mặc định `VND`)

**`Transaction`**:
- `id`, `walletId`
- `type` (`DEPOSIT` | `HOLD` | `RELEASE` | `WITHDRAW` | `REFUND` | `FEE`)
- `amount`, `balanceAfter`
- `bookingId` (nullable)
- `externalRef` (mã giao dịch từ cổng thanh toán)
- `status` (`PENDING` | `SUCCESS` | `FAILED`)
- `createdAt`

**`PaymentOrder`**:
- `id`, `bookingId`, `brandId`
- `amount`, `provider` (`VNPAY` | `MOMO` | `STRIPE`)
- `status`, `paymentUrl`, `paidAt`

**`WithdrawRequest`**:
- `id`, `kolId`, `amount`
- `bankName`, `bankAccount`, `accountName`
- `status` (`PENDING` | `APPROVED` | `REJECTED` | `PAID`)

Migration: `V6__create_payment.sql`.

### 6.3. Tích hợp cổng thanh toán
Chọn tối thiểu 1 cổng cho MVP. Thứ tự ưu tiên cho thị trường VN:
1. **VNPay** — phổ biến, hỗ trợ đầy đủ thẻ/QR.
2. **MoMo** — phổ biến với end user.
3. **Stripe** — nếu có khách hàng quốc tế.

Flow chung:
- [ ] Tạo `PaymentOrder` → gọi API cổng → nhận `paymentUrl` → redirect user.
- [ ] Endpoint callback (webhook) từ cổng → verify signature → cập nhật `PaymentOrder`, tạo `Transaction` `HOLD`, chuyển booking sang `IN_PROGRESS`.
- [ ] Idempotency: nếu webhook gọi lại cùng `externalRef` → không tạo transaction trùng.

### 6.4. API endpoints

**Brand**
| Method | Path | Mô tả |
|--------|------|-------|
| POST | `/api/v1/payments/bookings/{id}/checkout` | Tạo payment order, nhận URL thanh toán |
| POST | `/api/v1/payments/webhook/{provider}` | Webhook từ cổng (không auth, verify bằng signature) |
| GET | `/api/v1/payments/bookings/{id}` | Trạng thái thanh toán |

**KOL**
| Method | Path | Mô tả |
|--------|------|-------|
| GET | `/api/v1/wallet/me` | Xem số dư |
| GET | `/api/v1/wallet/me/transactions` | Lịch sử |
| POST | `/api/v1/wallet/me/withdraw` | Tạo yêu cầu rút tiền |

**Admin**
| Method | Path | Mô tả |
|--------|------|-------|
| GET | `/api/v1/admin/withdrawals` | Danh sách yêu cầu rút |
| POST | `/api/v1/admin/withdrawals/{id}/approve` | Duyệt và đánh dấu đã chi |
| POST | `/api/v1/admin/withdrawals/{id}/reject` | Từ chối |

### 6.5. Phí nền tảng (platform fee)
- Khi `RELEASE` từ booking → trích x% (VD 10%) vào ví hệ thống dưới dạng `Transaction` type `FEE`.
- Cấu hình qua `app.platform.fee-percent`.

### 6.6. Hoá đơn
- [ ] Sinh PDF hoá đơn khi booking `COMPLETED` (dùng iText hoặc OpenPDF).
- [ ] Lưu URL vào `Booking` để tải về.

### 6.7. Bảo mật & đúng đắn
- **Mọi cập nhật số dư phải trong transaction DB** (`@Transactional`).
- Dùng `SELECT ... FOR UPDATE` khi update ví, tránh race condition.
- **Không bao giờ** cập nhật ví trực tiếp — phải qua service `WalletService.record(transaction)`.
- Amount dùng `BigDecimal`, **không** dùng `double`/`float`.

## Definition of Done
- Brand thanh toán sandbox thành công → booking tự chuyển sang `IN_PROGRESS`.
- Webhook nhận 2 lần cùng mã → chỉ ghi 1 transaction.
- Booking `COMPLETED` → KOL thấy tiền trong ví; phí nền tảng được trích đúng.
- KOL yêu cầu rút → admin duyệt → trạng thái đúng, ví trừ chính xác.
- Không có trường hợp âm `balanceAvailable`.
