"# KOL Booking Backend

## 📖 Tổng quan dự án

Đây là hệ thống backend quản lý **KOL (Key Opinion Leader)** - một nền tảng kết nối giữa các **Brand** và **KOL** trong việc tìm kiếm, đánh giá và booking các influencer phù hợp với thương hiệu.

### 🎯 Đối tượng người dùng
- **Brand**: Doanh nghiệp/Nhãn hàng có nhu cầu tìm KOL để quảng bá sản phẩm. Có thể tìm kiếm, lọc, xem hồ sơ chi tiết và gửi yêu cầu booking.
- **KOL**: Người có tầm ảnh hưởng cao trên mạng xã hội. Tạo và quản lý hồ sơ chuyên nghiệp, tiếp nhận và xử lý các yêu cầu booking từ Brand.
- **Admin**: Quản trị viên hệ thống, duyệt hồ sơ KOL/Brand, quản lý các booking, xử lý tranh chấp và thống kê.

### 🔄 Luồng nghiệp vụ chính

1. **Đăng ký & Tạo hồ sơ**
   - KOL đăng ký tài khoản, cấp thông tin cá nhân và hồ sơ chuyên nghiệp (lĩnh vực, kênh mạng xã hội, số lượng follower, mức giá, ảnh, portfolio, vv).
   - Brand đăng ký tài khoản, bổ sung thông tin công ty.

2. **Tìm kiếm & Khám phá**
   - Brand tìm kiếm KOL theo các tiêu chí: ngành hàng, ngân sách, nền tảng, mức độ phủ sóng.
   - Xem danh sách KOL phù hợp, đọc hồ sơ chi tiết, xem các bài viết liên quan.

3. **Booking & Thỏa thuận**
   - Brand gửi yêu cầu booking với chi tiết chiến dịch, ngân sách, thời hạn.
   - KOL xác nhận (chấp nhận/từ chối) yêu cầu booking.
   - Cập nhật trạng thái thực hiện chiến dịch.

4. **Thanh toán & Đánh giá**
   - Xử lý thanh toán an toàn giữa Brand và KOL.
   - Brand và KOL có thể đánh giá và để lại nhận xét sau khi hoàn thành.

## 🛠 Tech Stack

| Thành phần | Công nghệ |
|-----------|----------|
| **Ngôn ngữ** | Java 21 |
| **Framework** | Spring Boot 3.5.13 |
| **ORM** | Spring Data JPA, Hibernate |
| **Security** | Spring Security + JWT |
| **Database** | PostgreSQL 16 |
| **Migration** | Flyway |
| **Build Tool** | Gradle (Groovy DSL) |
| **Utility** | Lombok |

### Dependencies chính
- `spring-boot-starter-web` — REST API
- `spring-boot-starter-data-jpa` — ORM/Database access
- `spring-boot-starter-security` — Authentication & Authorization
- `flyway-core` + `flyway-database-postgresql` — Database migration
- `lombok` — Giảm boilerplate code

## 📁 Cấu trúc thư mục

```
kol-booking-backend/
├── build.gradle                          # Gradle configuration & dependencies
├── settings.gradle                       # Project settings (name: datn)
├── compose.yaml                          # Docker Compose (PostgreSQL service)
├── Dockerfile                            # Docker image configuration
├── .env.example                          # Environment variables template
├── src/
│   ├── main/
│   │   ├── java/kolbooking/datn/         # Main source code
│   │   │   ├── admin/                    # Admin module
│   │   │   ├── auth/                     # Authentication & authorization
│   │   │   ├── booking/                  # Booking management
│   │   │   ├── brand/                    # Brand profiles & management
│   │   │   ├── kol/                      # KOL profiles & management
│   │   │   ├── payment/                  # Payment processing
│   │   │   ├── review/                   # Reviews & ratings
│   │   │   ├── notification/             # Notifications
│   │   │   └── common/                   # Shared utilities, config, exceptions
│   │   └── resources/
│   │       ├── application.properties    # Default config
│   │       ├── application-dev.properties
│   │       ├── application-prod.properties
│   │       ├── db/migration/             # Flyway migrations (V*.sql)
│   │       ├── static/                   # Static files
│   │       └── templates/                # Email templates, etc.
│   └── test/
│       └── java/kolbooking/datn/         # Unit & integration tests
├── docs/                                 # Project documentation
└── gradle/wrapper/                       # Gradle wrapper

```

## 🚀 Hướng dẫn chạy dự án

### Yêu cầu
- **Java 21** (JDK)
- **Docker** & **Docker Compose**
- **Gradle** (hoặc dùng `gradlew` có sẵn)

### Chạy trên môi trường dev

1. **Khởi động PostgreSQL qua Docker Compose**
   ```bash
   docker compose up -d
   ```

2. **Chạy Spring Boot ứng dụng**
   ```bash
   ./gradlew bootRun
   ```
   Hoặc trên Windows:
   ```bash
   gradlew.bat bootRun
   ```

   Spring Boot sẽ tự detect `compose.yaml` và khởi động PostgreSQL nếu chưa chạy.

3. **Ứng dụng sẽ chạy tại**
   ```
   http://localhost:8080
   ```

### Build & Package

```bash
# Build project
./gradlew build

# Build Docker image
docker build -t kol-booking-backend .

# Run container
docker run -p 8080:8080 --env-file .env kol-booking-backend
```

### Chạy tests
```bash
./gradlew test
```

## 🗄️ Database & Migration

- **Database**: `kol_booking`
- **User**: `kol_user`
- **Port**: `5432`
- **Password**: (xem `compose.yaml`)

### Quy ước Migration
- **Mọi thay đổi schema phải đi qua Flyway migration** trong `src/main/resources/db/migration/`.
- **Không dùng** `ddl-auto=update` ở production - chỉ sử dụng Flyway.
- **Đặt tên file** theo quy ước: `V<version>__<description>.sql`
  - Ví dụ: `V1__init_schema.sql`, `V2__create_app_user.sql`, vv.

## 📋 Quy ước Code

### Java
- Sử dụng **Java 21** - tận dụng `record`, pattern matching, text blocks khi phù hợp.
- Không dùng `@Data` cho JPA entity (dễ gây vòng lặp `equals`/`hashCode`).
- Sử dụng Lombok cẩn thận: `@Getter`, `@Builder`, `@RequiredArgsConstructor`.

### Architecture
- **Controller** trả về **DTO**, không expose entity trực tiếp.
- **Validate input** bằng `jakarta.validation` annotations ở DTO.
- **Xử lý exception** tập trung qua `@RestControllerAdvice` + custom exception classes.

### Package structure
Package gốc: `kolbooking.datn`. Khi thêm feature/module mới, đặt theo domain:
- `kolbooking.datn.admin` — Admin features
- `kolbooking.datn.auth` — Authentication, user/role
- `kolbooking.datn.booking` — Booking management
- `kolbooking.datn.brand` — Brand profiles
- `kolbooking.datn.kol` — KOL profiles
- `kolbooking.datn.payment` — Payment processing
- `kolbooking.datn.review` — Reviews & ratings
- `kolbooking.datn.notification` — Notifications
- `kolbooking.datn.common` — Shared utilities, exceptions, configs

## 📧 Environment Variables

Tham khảo `.env.example` để cấu hình:

```env
# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/kol_booking
SPRING_DATASOURCE_USERNAME=kol_user
SPRING_DATASOURCE_PASSWORD=kol_secret

# Spring Profiles
SPRING_PROFILES_ACTIVE=prod

# Mail Configuration (Optional - production only)
SPRING_MAIL_HOST=smtp.gmail.com
SPRING_MAIL_PORT=587
SPRING_MAIL_USERNAME=your-email@gmail.com
SPRING_MAIL_PASSWORD=your-app-password
APP_URL=https://your-domain.com

# Java Options
JAVA_OPTS=-Xmx512m -Xms256m
```

## 📚 Tài liệu

Chi tiết của từng module xem trong thư mục `docs/`:
- [01 - Foundation](docs/01-foundation.md)
- [02 - Authentication](docs/02-authentication.md)
- [03 - User Profile](docs/03-user-profile.md)
- [04 - Search & Discovery](docs/04-search-discovery.md)
- [05 - Booking Flow](docs/05-booking-flow.md)
- [06 - Payment](docs/06-payment.md)
- [07 - Review & Notification](docs/07-review-notification.md)
- [08 - Admin Module](docs/08-admin.md)
- [09 - Testing & Deployment](docs/09-testing-deployment.md)

## 🔐 API Documentation

API endpoints sẽ có Swagger/OpenAPI documentation tại:
```
http://localhost:8080/swagger-ui.html
```

## 📝 Ghi chú

- **Project name** trong Gradle là `datn` (đồ án tốt nghiệp) nhưng repository dùng tên `kol-booking-backend` - đây là tên nghiệp vụ chính thức.
- **Frontend** nằm ở repository khác.
- Hỗ trợ cả môi trường **development** và **production** với cấu hình riêng biệt.

## 📞 Support & Contact

Để báo cáo issue hoặc đề xuất tính năng, vui lòng tạo GitHub issue." 
