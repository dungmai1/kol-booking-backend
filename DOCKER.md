# Docker Deployment Guide

## Cấu trúc Docker

Dự án có:
- **Dockerfile**: Tạo image cho Spring Boot backend (multi-stage build)
- **docker-compose.yaml**: Orchestrate backend + PostgreSQL
- **.dockerignore**: Loại trừ file không cần khi build
- **.env.example**: Mẫu biến môi trường

## Chạy trên Docker (Development)

```bash
# Build và start các services
docker compose up -d

# Xem logs
docker compose logs -f backend

# Stop services
docker compose down

# Stop và xóa volumes (DB)
docker compose down -v
```

Ứng dụng sẽ chạy tại: **http://localhost:8080**

## Chạy trên Docker (Production)

### 1. Chuẩn bị biến môi trường
```bash
# Copy từ mẫu
cp .env.example .env

# Chỉnh sửa .env với các giá trị thực tế
# - SPRING_DATASOURCE_PASSWORD
# - SPRING_MAIL_* (nếu cần gửi email)
# - APP_URL (domain của bạn)
```

### 2. Build image
```bash
docker build -t kol-booking-backend:latest .
```

### 3. Push to Registry (Optional)
```bash
# Với Docker Hub
docker tag kol-booking-backend:latest your-registry/kol-booking-backend:latest
docker push your-registry/kol-booking-backend:latest
```

### 4. Chạy với docker-compose
```bash
docker compose up -d
```

## Dockerfile Details

Multi-stage build để tối ưu:
- **Builder stage**: Java 21 JDK, build app với Gradle
- **Runtime stage**: Java 21 JRE (nhẹ hơn JDK), chỉ copy artifacts

## Cấu hình Docker Compose

### Backend Service
- **Port**: 8080 (public) → 8080 (container)
- **Profile**: `prod` (sử dụng `application-prod.properties`)
- **Health check**: Kiểm tra `/actuator/health`
- **Depends on**: PostgreSQL (chờ DB sẵn sàng)

### PostgreSQL Service
- **Port**: 5432
- **Volume**: `postgres_data` (lưu persistent)
- **Health check**: `pg_isready`

## Biến môi trường quan trọng

| Biến | Default | Mô tả |
|------|---------|-------|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://postgres:5432/kol_booking` | DB connection |
| `SPRING_DATASOURCE_USERNAME` | `kol_user` | DB user |
| `SPRING_DATASOURCE_PASSWORD` | `kol_secret` | DB password |
| `SPRING_PROFILES_ACTIVE` | `prod` | Spring profile |
| `JAVA_OPTS` | `-Xmx512m -Xms256m` | JVM memory |

## Troubleshooting

### Backend không kết nối PostgreSQL
```bash
# Kiểm tra logs
docker compose logs backend

# Kiểm tra PostgreSQL status
docker compose logs postgres

# Restart services
docker compose restart
```

### Rebuild image (khi code thay đổi)
```bash
docker compose up -d --build
```

### Clean up tất cả
```bash
docker compose down -v --remove-orphans
docker system prune -a
```

## API Documentation

Khi backend chạy:
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

## Performance tuning

Chỉnh `JAVA_OPTS` trong `docker-compose.yaml`:
- `-Xmx1024m` → Tăng max heap nếu xử lý nhiều request
- `-Xms512m` → Khởi tạo heap size

## Database Backup/Restore

### Backup
```bash
docker compose exec postgres pg_dump -U kol_user kol_booking > backup.sql
```

### Restore
```bash
docker compose exec -T postgres psql -U kol_user kol_booking < backup.sql
```
