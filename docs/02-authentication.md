# Phase 02 — Xác thực & phân quyền

**Mục tiêu**: User có thể đăng ký, đăng nhập, nhận JWT token; hệ thống phân biệt được role `ADMIN`, `BRAND`, `KOL` để bảo vệ các endpoint ở phase sau.

## Checklist

### 2.1. Entity & Migration
- [ ] Tạo entity `AppUser` (tránh dùng tên `User` vì trùng keyword PostgreSQL):
  - `id` (UUID hoặc BIGINT)
  - `email` (unique, not null)
  - `passwordHash`
  - `role` (`ADMIN` | `BRAND` | `KOL`) — enum
  - `status` (`PENDING_VERIFICATION` | `ACTIVE` | `BANNED`)
  - `createdAt`, `updatedAt`
- [ ] Migration `V2__create_app_user.sql`.

### 2.2. Dependency
Thêm vào `build.gradle`:
```gradle
implementation 'io.jsonwebtoken:jjwt-api:0.12.6'
runtimeOnly   'io.jsonwebtoken:jjwt-impl:0.12.6'
runtimeOnly   'io.jsonwebtoken:jjwt-jackson:0.12.6'
```

### 2.3. Security Config
- [ ] `SecurityFilterChain` với:
  - `/api/v1/auth/**`, `/swagger-ui/**`, `/v3/api-docs/**`, `/actuator/health` → public
  - còn lại → `authenticated()`
- [ ] `PasswordEncoder` = `BCryptPasswordEncoder`.
- [ ] Stateless session (`SessionCreationPolicy.STATELESS`).
- [ ] Custom `JwtAuthenticationFilter` đặt trước `UsernamePasswordAuthenticationFilter`.

### 2.4. JWT
- [ ] `JwtService`:
  - `generateAccessToken(user)` — TTL 15 phút
  - `generateRefreshToken(user)` — TTL 7 ngày, lưu vào DB (bảng `refresh_token`)
  - `parseAndValidate(token)` → trả `Authentication`
- [ ] Secret key đọc từ env: `app.jwt.secret`.

### 2.5. API endpoints
| Method | Path | Mô tả |
|--------|------|-------|
| POST | `/api/v1/auth/register` | Đăng ký (kèm role: BRAND hoặc KOL) |
| POST | `/api/v1/auth/login` | Đăng nhập, trả access + refresh token |
| POST | `/api/v1/auth/refresh` | Cấp access mới từ refresh token |
| POST | `/api/v1/auth/logout` | Thu hồi refresh token |
| POST | `/api/v1/auth/verify-email` | Xác thực email qua token gửi mail |
| POST | `/api/v1/auth/forgot-password` | Gửi mail reset password |
| POST | `/api/v1/auth/reset-password` | Đặt lại password bằng token |

### 2.6. Email service
- [ ] Dùng `spring-boot-starter-mail`.
- [ ] Bước dev: dùng MailHog hoặc in-memory log, đừng gọi SMTP thật.
- [ ] Template: xác thực email, reset password.

### 2.7. Role-based authorization
- [ ] Bật `@EnableMethodSecurity`.
- [ ] Sử dụng `@PreAuthorize("hasRole('BRAND')")` ở controller sau này.

## Definition of Done
- Đăng ký user mới → lưu vào DB, mật khẩu đã hash.
- Login thành công → trả JWT, decode được role.
- Gọi endpoint protected không kèm token → 401.
- Gọi endpoint yêu cầu role `ADMIN` bằng token `BRAND` → 403.
- Refresh token hoạt động; logout thu hồi refresh token.
- Có ít nhất 3 test cho AuthService (register thành công, login sai password, duplicate email).
