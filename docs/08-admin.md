# Phase 08 — Trang Quản trị (Admin)

**Mục tiêu**: Admin có đủ công cụ để vận hành nền tảng: duyệt KOL/Brand, quản lý danh mục, xử lý tranh chấp, xem thống kê.

## Checklist

### 8.1. Duyệt hồ sơ
- [ ] API xem danh sách KOL/Brand theo `status` (`PENDING_REVIEW`, `APPROVED`, `REJECTED`).
- [ ] API duyệt / từ chối (kèm lý do).
- [ ] Khi từ chối → notification + email cho user, chuyển status `REJECTED` nhưng giữ data để user sửa và submit lại.

| Method | Path | Mô tả |
|--------|------|-------|
| GET | `/api/v1/admin/kols?status=PENDING_REVIEW` | Danh sách KOL chờ duyệt |
| POST | `/api/v1/admin/kols/{id}/approve` | Duyệt |
| POST | `/api/v1/admin/kols/{id}/reject` | Từ chối (kèm `reason`) |
| (tương tự cho `brands`) | | |

### 8.2. Quản lý category
| Method | Path | Mô tả |
|--------|------|-------|
| POST | `/api/v1/admin/categories` | Tạo |
| PUT | `/api/v1/admin/categories/{id}` | Sửa |
| DELETE | `/api/v1/admin/categories/{id}` | Xoá (chỉ khi không có KOL nào dùng) |

### 8.3. Quản lý user
- [ ] Ban / unban user (`status = BANNED`).
- [ ] Reset password (force).
- [ ] Xem lịch sử đăng nhập (nếu có log).

| Method | Path | |
|--------|------|-|
| GET | `/api/v1/admin/users?q=...&role=...` | Tìm user |
| POST | `/api/v1/admin/users/{id}/ban` | Cấm |
| POST | `/api/v1/admin/users/{id}/unban` | Mở |

### 8.4. Giám sát booking & xử lý tranh chấp
- [ ] Xem mọi booking, filter theo status.
- [ ] Xem lịch sử trạng thái (`BookingStatusHistory`), tin nhắn giữa 2 bên.
- [ ] Xử lý tranh chấp `DISPUTED`:
  - Hoàn tiền Brand (release về Brand wallet → refund về cổng thanh toán nếu có thể)
  - Hoặc giải ngân cho KOL
  - Hoặc chia tỉ lệ (partial refund)
- [ ] Cưỡng bức huỷ/hoàn booking với ghi chú.

| Method | Path | |
|--------|------|-|
| GET | `/api/v1/admin/bookings` | |
| POST | `/api/v1/admin/bookings/{id}/resolve-dispute` | Body: `action=REFUND_BRAND\|PAY_KOL\|SPLIT`, `note`, `splitPercent?` |

### 8.5. Analytics / Dashboard
Các chỉ số cần có endpoint:
- Tổng số user theo role
- Booking theo tháng (count + tổng giá trị)
- Top 10 KOL theo doanh thu
- Top 10 Brand theo chi tiêu
- Conversion funnel: đăng ký → submit profile → được duyệt → có booking đầu tiên
- Platform revenue theo tháng (tổng phí nền tảng)

| Method | Path | |
|--------|------|-|
| GET | `/api/v1/admin/stats/overview` | Số liệu tổng hợp hiện tại |
| GET | `/api/v1/admin/stats/bookings?from=&to=&groupBy=month` | Biểu đồ booking |
| GET | `/api/v1/admin/stats/top-kols?limit=10` | |
| GET | `/api/v1/admin/stats/revenue?from=&to=` | |

### 8.6. Audit log
- [ ] Bảng `AdminAuditLog`: mỗi hành động admin (duyệt, ban, giải ngân, ...) được ghi lại (`adminId`, `action`, `targetType`, `targetId`, `payload`, `createdAt`).
- [ ] API `GET /api/v1/admin/audit-logs` để truy cứu.

### 8.7. Bảo mật
- Tất cả endpoint phải có `@PreAuthorize("hasRole('ADMIN')")`.
- Cần tạo user admin đầu tiên qua migration (hoặc CLI) — **không** cho đăng ký role ADMIN qua API public.

## Definition of Done
- Admin đăng nhập được và chỉ admin mới gọi được API `/admin/**`.
- Duyệt KOL → KOL đó xuất hiện trong search (phase 04).
- Từ chối KOL → notification + email gửi đúng.
- Xử lý tranh chấp tạo đúng các transaction trong ví (phase 06).
- Dashboard trả số liệu khớp với DB thực tế.
- Mọi hành động admin được ghi audit log.
