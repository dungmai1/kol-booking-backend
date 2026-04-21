# Phase 01 — Nền tảng hạ tầng

**Mục tiêu**: Chuẩn bị nền móng kỹ thuật để các phase sau không phải đụng lại. Kết thúc phase này, project phải chạy được, kết nối DB được, có migration, có xử lý lỗi chung.

## Checklist

### 1.1. Cấu hình kết nối Database
- [ ] Cập nhật `src/main/resources/application.properties` (hoặc chuyển sang `application.yml`):
  ```properties
  spring.datasource.url=jdbc:postgresql://localhost:5432/kol_booking
  spring.datasource.username=kol_user
  spring.datasource.password=kol_secret
  spring.jpa.hibernate.ddl-auto=validate
  spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
  spring.jpa.open-in-view=false
  ```
- [ ] Tách profile: `application-dev.properties`, `application-prod.properties`.
- [ ] Dùng biến môi trường cho password ở `prod` (đừng commit secret).

### 1.2. Flyway migration
- [ ] Tạo file đầu tiên: `src/main/resources/db/migration/V1__init_schema.sql` với bảng baseline (có thể rỗng hoặc chứa bảng `app_user`).
- [ ] Quy ước đặt tên: `V<n>__<mô_tả_snake_case>.sql`.
- [ ] **Tuyệt đối không sửa** file migration đã merge vào main.

### 1.3. Cấu trúc package theo domain
Tạo sẵn các package rỗng để phase sau thả code vào:
```
kolbooking.datn
├── common/
│   ├── config/          # SecurityConfig, WebConfig, OpenApiConfig
│   ├── exception/       # GlobalExceptionHandler, custom exceptions
│   ├── dto/             # ApiResponse<T>, PageResponse<T>
│   └── util/
├── auth/
├── user/
├── kol/
├── brand/
├── booking/
└── payment/
```

### 1.4. Exception handler & Response wrapper
- [ ] Tạo `ApiResponse<T>` dạng `{ success, data, message, errorCode }` để mọi API trả về đồng nhất.
- [ ] `GlobalExceptionHandler` (`@RestControllerAdvice`) xử lý:
  - `MethodArgumentNotValidException` (validation) → 400
  - `ResourceNotFoundException` (custom) → 404
  - `AccessDeniedException` → 403
  - `Exception` fallback → 500 (log đầy đủ stacktrace)

### 1.5. Cấu hình bổ sung
- [ ] **CORS**: cho phép origin của frontend trong dev (`http://localhost:3000` chẳng hạn).
- [ ] **Swagger/OpenAPI**: thêm `springdoc-openapi-starter-webmvc-ui` vào `build.gradle`, truy cập `/swagger-ui.html`.
- [ ] **Logging**: format JSON cho prod (dùng `logback-spring.xml`).
- [ ] **Validation**: thêm `spring-boot-starter-validation`.
- [ ] **MapStruct** (khuyến nghị) để map Entity ↔ DTO.

### 1.6. Health check
- [ ] Thêm `spring-boot-starter-actuator`, expose endpoint `/actuator/health`.

## Definition of Done
- Chạy `./gradlew bootRun` → app start thành công, kết nối được PostgreSQL.
- `GET /actuator/health` trả về `UP`.
- Mở được Swagger UI.
- Có ít nhất 1 file migration V1 chạy thành công khi start app.
- Gọi API không tồn tại → nhận response có format `ApiResponse` chứ không phải Spring default.
