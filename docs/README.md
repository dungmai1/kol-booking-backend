# Lộ trình phát triển KOL Booking Backend

Thư mục này chứa kế hoạch triển khai dự án theo **thứ tự ưu tiên**. Mỗi file mô tả một giai đoạn (phase), bao gồm mục tiêu, các task cần làm, API endpoint chính và tiêu chí hoàn thành (Definition of Done).

## Thứ tự thực hiện

| # | Giai đoạn | File | Mục tiêu ngắn gọn |
|---|-----------|------|-------------------|
| 01 | Nền tảng hạ tầng | [01-foundation.md](./01-foundation.md) | Setup project, DB, Flyway, cấu hình chung |
| 02 | Xác thực & phân quyền | [02-authentication.md](./02-authentication.md) | Đăng ký, đăng nhập, JWT, role |
| 03 | Hồ sơ User/KOL/Brand | [03-user-profile.md](./03-user-profile.md) | Quản lý profile 3 loại user |
| 04 | Tìm kiếm KOL | [04-search-discovery.md](./04-search-discovery.md) | Filter, sort, gợi ý KOL cho Brand |
| 05 | Luồng booking | [05-booking-flow.md](./05-booking-flow.md) | Brand đặt KOL, xác nhận, thực hiện |
| 06 | Thanh toán | [06-payment.md](./06-payment.md) | Escrow, cổng thanh toán, hoá đơn |
| 07 | Đánh giá & thông báo | [07-review-notification.md](./07-review-notification.md) | Rating sau booking, notification |
| 08 | Trang quản trị | [08-admin.md](./08-admin.md) | Duyệt KOL, xử lý tranh chấp, analytics |
| 09 | Kiểm thử & triển khai | [09-testing-deployment.md](./09-testing-deployment.md) | Test, CI/CD, deploy production |

## Nguyên tắc chung khi triển khai

1. **Đi từ dưới lên trên**: hoàn thiện phase trước rồi mới bắt đầu phase sau. Phase 1 và 2 là bắt buộc trước mọi thứ khác.
2. **Mỗi phase phải có migration Flyway riêng** — không sửa migration đã commit.
3. **Luôn viết test trước khi mark phase là DONE** — ít nhất là test cho service layer quan trọng.
4. **DTO tách khỏi Entity** — không expose entity ra controller.
5. **Ghi log có cấu trúc** (JSON) cho các action quan trọng: đăng ký, booking, thanh toán.

## Mức độ ưu tiên tính năng

- **MVP (bắt buộc)**: Phase 01 → 05, cộng Phase 07 (chỉ rating) và Phase 09 (test + deploy cơ bản).
- **Nâng cao**: Phase 06 (thanh toán thật), 07 (notification real-time), 08 (admin đầy đủ).
- **Mở rộng tương lai**: AI matching KOL-Brand, chat real-time, analytics nâng cao, app mobile.
