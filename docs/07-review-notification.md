# Phase 07 — Đánh giá & Thông báo

**Mục tiêu**: Sau khi booking hoàn tất, hai bên đánh giá lẫn nhau → ảnh hưởng đến ranking ở phase 04. Hệ thống notification realtime + email cho các sự kiện quan trọng.

## Phần A — Review / Rating

### 7.1. Entity
**`Review`**:
- `id`, `bookingId` (unique → mỗi booking chỉ review 1 lần cho mỗi hướng)
- `authorId`, `targetId`
- `direction` (`BRAND_TO_KOL` | `KOL_TO_BRAND`)
- `rating` (1–5)
- `comment` (TEXT)
- `createdAt`

Migration: `V7__create_review.sql`.

### 7.2. Ràng buộc
- Chỉ được review khi booking `COMPLETED`.
- Mỗi hướng 1 review duy nhất cho cùng booking (unique constraint `(bookingId, direction)`).
- Không sửa được sau 7 ngày.

### 7.3. API
| Method | Path | Role | Mô tả |
|--------|------|------|-------|
| POST | `/api/v1/bookings/{id}/reviews` | BRAND/KOL | Tạo review |
| PUT | `/api/v1/reviews/{id}` | owner | Sửa (trong 7 ngày) |
| GET | `/api/v1/kols/{id}/reviews` | public | Review của KOL (có paging) |

### 7.4. Tổng hợp rating
- Thêm 2 cột denormalized vào `KolProfile`: `avgRating`, `reviewCount`.
- Cập nhật mỗi khi có review mới (trong transaction).
- Integration với phase 04: cho phép sort/filter theo rating.

---

## Phần B — Notification

### 7.5. Kiến trúc
- **In-app**: lưu vào bảng `Notification`.
- **Email**: gửi qua SMTP/SendGrid cho các sự kiện quan trọng.
- **Realtime**: WebSocket (`spring-boot-starter-websocket` + STOMP) để push in-app notification tức thì.
- **Push mobile** (tương lai): Firebase FCM.

### 7.6. Entity
**`Notification`**:
- `id`, `userId`
- `type` (`BOOKING_CREATED`, `BOOKING_ACCEPTED`, `PAYMENT_SUCCESS`, `DELIVERABLE_SUBMITTED`, `REVIEW_RECEIVED`, `WITHDRAW_APPROVED`, ...)
- `title`, `message`, `link` (deep link)
- `readAt`, `createdAt`

### 7.7. Danh sách sự kiện bắt buộc
| Event | Người nhận | In-app | Email |
|-------|-----------|:------:|:-----:|
| Brand tạo booking | KOL | ✓ | ✓ |
| KOL accept/reject | Brand | ✓ | ✓ |
| Brand thanh toán OK | KOL | ✓ | ✓ |
| KOL submit deliverable | Brand | ✓ | ✓ |
| Brand nghiệm thu | KOL | ✓ | ✓ |
| Có tranh chấp | Admin | ✓ | ✓ |
| Admin duyệt KOL/Brand | User | ✓ | ✓ |
| Yêu cầu rút tiền được duyệt | KOL | ✓ | ✓ |
| Có tin nhắn mới trong booking | Đối phương | ✓ | — |

### 7.8. Triển khai
- [ ] Tạo `NotificationService` + `@EventListener` lắng nghe domain events từ phase 05/06.
- [ ] Gửi email async (`@Async`) để không block main flow.
- [ ] WebSocket endpoint `/ws`, topic `/user/queue/notifications` (per-user).
- [ ] Template email: Thymeleaf hoặc FreeMarker.

### 7.9. API
| Method | Path | Mô tả |
|--------|------|-------|
| GET | `/api/v1/notifications/me` | Danh sách, có filter `unreadOnly=true` |
| PATCH | `/api/v1/notifications/{id}/read` | Đánh dấu đã đọc |
| POST | `/api/v1/notifications/me/read-all` | Đánh dấu tất cả đã đọc |

## Definition of Done
- Khi booking chuyển trạng thái → notification được tạo đúng người nhận.
- Email gửi không block response API (async).
- WebSocket đẩy notification real-time khi user đang online.
- Rating trung bình của KOL được cập nhật chính xác sau khi có review mới.
- Test: Brand review KOL 5 sao → KOL đó có `avgRating` đúng.
