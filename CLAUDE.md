# KOL Booking Backend

## Tổng quan dự án

Đây là hệ thống backend quản lý **KOL (Key Opinion Leader)**, phục vụ các **Brand** trong việc tìm kiếm, đánh giá và **booking** KOL phù hợp với thương hiệu của mình.

### Đối tượng người dùng
- **Brand**: doanh nghiệp/nhãn hàng có nhu cầu tìm KOL để quảng bá. Có thể tìm kiếm, lọc, xem hồ sơ và đặt booking với KOL.
- **KOL**: người có tầm ảnh hưởng, tạo hồ sơ để nhận booking từ các Brand.
- **Admin**: quản trị hệ thống, duyệt KOL/Brand, quản lý booking, xử lý tranh chấp.

### Luồng nghiệp vụ chính
1. KOL đăng ký, tạo hồ sơ (lĩnh vực, kênh mạng xã hội, số lượng follower, mức giá, ảnh, portfolio...).
2. Brand đăng ký, tìm kiếm KOL theo tiêu chí (ngành hàng, ngân sách, nền tảng, độ phủ...).
3. Brand gửi yêu cầu booking → KOL xác nhận/từ chối → thực hiện chiến dịch → thanh toán → đánh giá.

## Tech stack

- **Ngôn ngữ**: Java 21
- **Framework**: Spring Boot 3.5.13
  - `spring-boot-starter-web` — REST API
  - `spring-boot-starter-data-jpa` — ORM
  - `spring-boot-starter-security` — xác thực & phân quyền
- **Database**: PostgreSQL 16 (cài trực tiếp trên máy hoặc dùng Postgres remote như Supabase)
- **Migration**: Flyway (`flyway-core` + `flyway-database-postgresql`)
- **Build tool**: Gradle (Groovy DSL)
- **Tiện ích**: Lombok

## Cấu trúc thư mục

```
kol-booking-backend/
├── build.gradle                  # Cấu hình Gradle, dependencies
├── settings.gradle               # Tên project: datn
├── src/main/
│   ├── java/kolbooking/datn/
│   │   └── DatnApplication.java  # Entry point Spring Boot
│   └── resources/
│       ├── application.properties
│       ├── db/migration/         # Flyway migration scripts (V1__*.sql, V2__*.sql...)
│       ├── static/
│       └── templates/
└── src/test/                     # Unit & integration tests
```

Package gốc: `kolbooking.datn`. Khi thêm module mới (controller, service, entity, repository) nên đặt theo domain:
- `kolbooking.datn.kol` — quản lý KOL
- `kolbooking.datn.brand` — quản lý Brand
- `kolbooking.datn.booking` — đơn booking
- `kolbooking.datn.product` — sản phẩm/đăng tin của Brand + ứng tuyển của KOL (top-5, duyệt → tạo booking)
- `kolbooking.datn.payment` — ví, thanh toán (VNPay + mock có chữ ký), hoa hồng nền tảng, rút tiền
- `kolbooking.datn.auth` — xác thực, user/role, email
- `kolbooking.datn.notification` — thông báo in-app + email
- `kolbooking.datn.common` — shared utilities, exception, config

## Chạy dự án (dev)

Yêu cầu: Postgres đã chạy sẵn (cục bộ hoặc remote như Supabase) và các biến `SPRING_DATASOURCE_URL` / `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD` đã được set (qua `.env` hoặc launcher).

```bash
# Chạy Spring Boot
./gradlew bootRun

# Build
./gradlew build

# Test
./gradlew test
```

Trên Windows dùng `gradlew.bat` thay cho `./gradlew`. Tham khảo `.env.example` cho danh sách biến môi trường.

## Database & Migration

- Dev: dùng Postgres cục bộ của máy hoặc Supabase remote. Tham số kết nối truyền qua env (`SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`) — **không hardcode** trong properties.
- **Mọi thay đổi schema phải đi qua Flyway migration** trong `src/main/resources/db/migration/`. Không dùng `ddl-auto=update` ở production.
- Đặt tên file theo quy ước: `V<version>__<description>.sql`, ví dụ `V1__create_kol_table.sql`.

## Quy ước code

- Java 21 — tận dụng `record`, pattern matching, text blocks khi phù hợp.
- Dùng Lombok (`@Getter`, `@Builder`, `@RequiredArgsConstructor`) để giảm boilerplate, **không** dùng `@Data` cho JPA entity (dễ gây vòng lặp `equals`/`hashCode`).
- Controller trả về DTO, không expose entity trực tiếp.
- Validate input bằng `jakarta.validation` annotations ở DTO.
- Xử lý exception tập trung qua `@RestControllerAdvice`.

## Xác thực email (bắt buộc)

- Đăng ký vẫn trả token, nhưng tài khoản `PENDING_VERIFICATION` bị **khoá tính năng** bởi `EmailVerificationInterceptor` (fail-closed): mọi endpoint nghiệp vụ trả `403 EMAIL_NOT_VERIFIED`, chỉ cho phép `/api/v1/auth/**`, `/api/v1/users/me`, các endpoint đọc công khai (catalog KOL/category/plan/product) và callback thanh toán.
- Link xác nhận trong email gọi `GET /api/v1/auth/verify-email?token=...` (trả trang HTML). Có thêm `POST /api/v1/auth/resend-verification`.
- `EmailService` gửi HTML qua `JavaMailSender` ở prod (`spring.mail.*`); ở dev (`app.mail.dev-mode=true`) chỉ log link.

## Thanh toán & hoa hồng

- Hoa hồng nền tảng (`app.platform.fee-percent`, mặc định 10%) được **snapshot vào từng booking** lúc tạo (`booking.platform_fee_percent`); khi booking `COMPLETED`, tiền được giải ngân: KOL nhận net, ví nền tảng (tài khoản hệ thống `user_id=0`, role `SYSTEM`) nhận phí. Admin xem ở `GET /api/v1/admin/stats/commission`.
- Cổng VNPay (`app.payment.vnpay.*`): URL ký HMAC-SHA512, verify chữ ký ở IPN `GET /api/v1/payments/vnpay/ipn` (authoritative) và Return `GET /api/v1/payments/vnpay/return`. Provider `MOCK` (dev) dùng webhook ký HMAC-SHA256 (`app.payment.mock.secret`) — **webhook luôn yêu cầu chữ ký hợp lệ**.

### Biến môi trường bổ sung (prod)
`APP_FRONTEND_URL`, `VNPAY_TMN_CODE`, `VNPAY_HASH_SECRET`, `VNPAY_PAY_URL`, `VNPAY_RETURN_URL`, `PAYMENT_MOCK_SECRET`, `SPRING_MAIL_HOST/PORT/USERNAME/PASSWORD`.

## Ghi chú

- Tên project trong Gradle là `datn` (đồ án tốt nghiệp) nhưng repository đã đổi thành `kol-booking-backend` — đây là tên nghiệp vụ chính thức.
- Frontend nằm ở repository khác (nếu có).
