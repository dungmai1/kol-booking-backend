# Phase 05 — Luồng Booking

**Mục tiêu**: Brand gửi yêu cầu booking → KOL xác nhận/từ chối → thực hiện → bàn giao → hoàn tất. Đây là trung tâm của sản phẩm.

## Checklist

### 5.1. State machine

```
DRAFT → PENDING (Brand gửi yêu cầu)
  PENDING → ACCEPTED  (KOL đồng ý)
  PENDING → REJECTED  (KOL từ chối)
  PENDING → CANCELLED (Brand huỷ khi chưa ai xử lý)
  ACCEPTED → IN_PROGRESS (sau khi thanh toán thành công)
  IN_PROGRESS → DELIVERED (KOL submit kết quả)
  DELIVERED → COMPLETED (Brand xác nhận nghiệm thu)
  DELIVERED → DISPUTED  (Brand khiếu nại)
  * → CANCELLED_BY_ADMIN (admin huỷ trong trường hợp đặc biệt)
```

Chuyển trạng thái **chỉ qua service layer** — không cho phép controller set trực tiếp.

### 5.2. Entity & Migration

**`Booking`**:
- `id`
- `brandId`, `kolId`
- `campaignTitle`, `campaignBrief` (TEXT)
- `deliverables` (JSONB: loại content, số lượng, platform)
- `budget` (VND)
- `startDate`, `endDate`
- `status` (enum ở trên)
- `rejectReason`, `cancelReason`
- `createdAt`, `updatedAt`

**`BookingMessage`** — 1-n với Booking:
- `senderId`, `content`, `attachmentUrl`, `createdAt`
- Dùng cho trao đổi/thương lượng giữa Brand và KOL.

**`BookingDeliverable`** — 1-n:
- `type`, `platform`, `submittedUrl`, `submittedAt`, `status`

**`BookingStatusHistory`** — audit log:
- `bookingId`, `fromStatus`, `toStatus`, `changedBy`, `note`, `changedAt`

Migration: `V5__create_booking.sql`.

### 5.3. API endpoints

**Brand**
| Method | Path | Mô tả |
|--------|------|-------|
| POST | `/api/v1/bookings` | Tạo booking (trạng thái `PENDING`) |
| GET | `/api/v1/bookings/me` | Danh sách booking của Brand |
| GET | `/api/v1/bookings/{id}` | Chi tiết (chỉ brand/kol của booking xem được) |
| POST | `/api/v1/bookings/{id}/cancel` | Huỷ (chỉ khi `PENDING`) |
| POST | `/api/v1/bookings/{id}/approve-delivery` | Nghiệm thu → `COMPLETED` |
| POST | `/api/v1/bookings/{id}/dispute` | Khiếu nại → `DISPUTED` |

**KOL**
| Method | Path | Mô tả |
|--------|------|-------|
| GET | `/api/v1/bookings/incoming` | Các yêu cầu đến |
| POST | `/api/v1/bookings/{id}/accept` | Đồng ý → `ACCEPTED` |
| POST | `/api/v1/bookings/{id}/reject` | Từ chối (kèm lý do) |
| POST | `/api/v1/bookings/{id}/deliverables` | Nộp kết quả → `DELIVERED` |

**Chung**
| Method | Path | Mô tả |
|--------|------|-------|
| POST | `/api/v1/bookings/{id}/messages` | Gửi message trong booking |
| GET | `/api/v1/bookings/{id}/messages` | Lấy lịch sử chat |

### 5.4. Logic nghiệp vụ quan trọng
- **Không thể đặt booking khi KOL chưa APPROVED**.
- **Không thể accept/reject sau khi quá hạn** (auto-expire sau 7 ngày → `CANCELLED`).
- Khi chuyển trạng thái, ghi entry vào `BookingStatusHistory`.
- Khi Brand tạo booking → gửi notification cho KOL (phase 07).
- Mỗi lần chuyển trạng thái → emit domain event (Spring `ApplicationEventPublisher`) để phase 06/07 lắng nghe.

### 5.5. Scheduled jobs
- [ ] Job chạy mỗi giờ: tìm booking `PENDING` quá 7 ngày → `CANCELLED` + thông báo Brand.
- [ ] Job chạy mỗi ngày: tìm booking `DELIVERED` quá 3 ngày chưa nghiệm thu → tự động `COMPLETED`.

Dùng `@Scheduled` + `@EnableScheduling`.

## Definition of Done
- Toàn bộ state machine được bảo vệ (không thể nhảy trạng thái sai).
- Audit log `BookingStatusHistory` ghi đầy đủ.
- Không Brand/KOL nào xem được booking của người khác.
- Job tự động huỷ booking quá hạn hoạt động.
- Ít nhất 10 integration test cho các kịch bản chuyển trạng thái.
