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
  - `spring-boot-docker-compose` — tự động khởi động DB khi chạy dev
- **Database**: PostgreSQL 16
- **Migration**: Flyway (`flyway-core` + `flyway-database-postgresql`)
- **Build tool**: Gradle (Groovy DSL)
- **Tiện ích**: Lombok
- **Container**: Docker Compose (`compose.yaml`)

## Cấu trúc thư mục

```
kol-booking-backend/
├── build.gradle                  # Cấu hình Gradle, dependencies
├── settings.gradle               # Tên project: datn
├── compose.yaml                  # PostgreSQL service cho dev
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
- `kolbooking.datn.auth` — xác thực, user/role
- `kolbooking.datn.common` — shared utilities, exception, config

## Chạy dự án (dev)

```bash
# Chạy PostgreSQL qua Docker Compose
docker compose up -d

# Chạy Spring Boot (Spring Boot Docker Compose plugin sẽ tự detect compose.yaml)
./gradlew bootRun

# Build
./gradlew build

# Test
./gradlew test
```

Trên Windows dùng `gradlew.bat` thay cho `./gradlew`.

## Database & Migration

- DB name: `kol_booking`, user: `kol_user`, port: `5432` (xem `compose.yaml`).
- **Mọi thay đổi schema phải đi qua Flyway migration** trong `src/main/resources/db/migration/`. Không dùng `ddl-auto=update` ở production.
- Đặt tên file theo quy ước: `V<version>__<description>.sql`, ví dụ `V1__create_kol_table.sql`.

## Quy ước code

- Java 21 — tận dụng `record`, pattern matching, text blocks khi phù hợp.
- Dùng Lombok (`@Getter`, `@Builder`, `@RequiredArgsConstructor`) để giảm boilerplate, **không** dùng `@Data` cho JPA entity (dễ gây vòng lặp `equals`/`hashCode`).
- Controller trả về DTO, không expose entity trực tiếp.
- Validate input bằng `jakarta.validation` annotations ở DTO.
- Xử lý exception tập trung qua `@RestControllerAdvice`.

## Ghi chú

- Tên project trong Gradle là `datn` (đồ án tốt nghiệp) nhưng repository đã đổi thành `kol-booking-backend` — đây là tên nghiệp vụ chính thức.
- Frontend nằm ở repository khác (nếu có).
