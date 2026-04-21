# Phase 09 — Kiểm thử & Triển khai

**Mục tiêu**: Đảm bảo chất lượng qua test tự động, có pipeline CI, deploy được lên production ổn định với logging và monitoring.

## Phần A — Testing

### 9.1. Chiến lược test
- **Unit test**: service layer, utility. Mock repository. Dùng JUnit 5 + Mockito.
- **Integration test**: test repository + flow end-to-end trong 1 module. Dùng **Testcontainers** (Postgres thật, không H2).
- **Contract test**: dùng Spring REST Docs hoặc snapshot để đảm bảo response API không breaking change.
- **Load test** (trước go-live): k6 hoặc JMeter cho các endpoint hot: search KOL, tạo booking.

### 9.2. Coverage tối thiểu
- Service layer: **≥ 80%**.
- Các flow quan trọng (auth, booking state machine, payment): **≥ 90%**, có test cho mọi nhánh.
- Không block build vì coverage ở phase đầu, nhưng nên thêm Jacoco để theo dõi.

### 9.3. Cần test bắt buộc
- [ ] Đăng ký trùng email → 409
- [ ] Login sai password → 401
- [ ] JWT hết hạn → 401
- [ ] Brand không thể xem booking của brand khác → 403
- [ ] Booking state machine: mọi chuyển trạng thái hợp lệ + các chuyển không hợp lệ đều bị reject
- [ ] Thanh toán webhook gọi 2 lần → chỉ ghi 1 transaction
- [ ] Race condition update ví (bắn đồng thời 2 request)
- [ ] KOL chưa duyệt không xuất hiện trong search
- [ ] Review chỉ tạo được khi booking `COMPLETED`

---

## Phần B — Deployment

### 9.4. Docker hoá
- [ ] Tạo `Dockerfile` multi-stage (build bằng gradle image, runtime bằng JRE slim):
  ```dockerfile
  FROM eclipse-temurin:21-jdk-alpine AS build
  WORKDIR /app
  COPY . .
  RUN ./gradlew bootJar --no-daemon

  FROM eclipse-temurin:21-jre-alpine
  WORKDIR /app
  COPY --from=build /app/build/libs/*.jar app.jar
  EXPOSE 8080
  ENTRYPOINT ["java","-jar","/app/app.jar"]
  ```
- [ ] Thêm service `app` vào `compose.yaml` (hoặc `compose.prod.yaml`) để chạy kèm Postgres.

### 9.5. Biến môi trường production
Không hardcode, đọc từ env:
- `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`
- `APP_JWT_SECRET`
- `APP_MAIL_HOST`, `APP_MAIL_USERNAME`, `APP_MAIL_PASSWORD`
- `APP_PAYMENT_VNPAY_TMN_CODE`, `APP_PAYMENT_VNPAY_HASH_SECRET`
- `APP_STORAGE_PROVIDER`, `APP_STORAGE_BUCKET`, credentials tương ứng
- `SPRING_PROFILES_ACTIVE=prod`

### 9.6. CI/CD (GitHub Actions gợi ý)
File `.github/workflows/ci.yml`:
- [ ] Trigger: push & pull_request
- [ ] Steps: checkout → setup JDK 21 → cache gradle → `./gradlew build` (bao gồm test)
- [ ] Upload test report
- [ ] (optional) build & push Docker image lên registry trên branch `main`

File `.github/workflows/deploy.yml`:
- [ ] Trigger: push tag `v*`
- [ ] SSH lên server → pull image → `docker compose up -d`

### 9.7. Logging
- [ ] Prod: JSON structured log (timestamp, level, logger, traceId, userId).
- [ ] Dùng MDC để gắn `traceId` vào mọi request (filter servlet).

### 9.8. Monitoring
- [ ] Expose Prometheus metrics (`spring-boot-starter-actuator` + `micrometer-registry-prometheus`).
- [ ] Gợi ý stack: **Prometheus + Grafana** hoặc **Uptime Kuma** cho start nhỏ.
- [ ] Alert khi:
  - Error rate > 1% trong 5 phút
  - Response time p95 > 1s
  - Số job scheduled fail > 0
  - DB connection pool cạn

### 9.9. Backup & disaster recovery
- [ ] Cron backup Postgres hàng ngày, lưu 7 bản gần nhất off-site (S3).
- [ ] Thử restore ít nhất 1 lần trước khi go-live.

### 9.10. Go-live checklist
- [ ] Tất cả migration chạy xong trên prod DB
- [ ] Tạo user admin đầu tiên
- [ ] Seed category
- [ ] Chạy smoke test end-to-end (đăng ký → tạo profile → search → booking → thanh toán sandbox)
- [ ] Cấu hình domain + HTTPS (Caddy/Nginx + Let's Encrypt)
- [ ] Rate limit: dùng bucket4j hoặc trước cổng Nginx
- [ ] Backup script đã chạy tự động

## Definition of Done
- Pipeline CI xanh trên mọi PR.
- Build Docker image chạy được trên máy khác không lỗi.
- Có dashboard Grafana xem được metrics cơ bản (request rate, latency, error rate).
- Có 1 lần restore DB từ backup thành công.
- Bài test smoke end-to-end trên môi trường staging pass.
