# Phase 04 — Tìm kiếm & khám phá KOL

**Mục tiêu**: Brand có thể lọc, sắp xếp, tìm kiếm KOL phù hợp với chiến dịch theo nhiều tiêu chí. Đây là tính năng **cốt lõi** tạo giá trị sản phẩm.

## Checklist

### 4.1. Bộ lọc cần hỗ trợ
- **Từ khoá**: theo `displayName`, `bio`, tên kênh.
- **Category**: 1 hoặc nhiều (KOL có trong category nào đều match).
- **Platform**: `TIKTOK`, `INSTAGRAM`, `YOUTUBE`, `FACEBOOK`.
- **Follower range**: min–max (trên bất kỳ kênh nào).
- **Price range**: min–max (dựa trên `KolPricingPackage`).
- **Location**: `city`, `country`.
- **Gender**, **age range**.
- **Rating tối thiểu** (sau khi phase 07 có).
- Chỉ trả về KOL `status = APPROVED`.

### 4.2. Sắp xếp
- Mặc định: `featured` (rating DESC + reviewCount DESC).
- Frontend alias (ưu tiên dùng trên FE):
  - `followers` → sort theo `maxFollowerCount` DESC
  - `rating` → sort theo `avgRating` DESC
  - `featured`, `price_asc`, `price_desc`, `newest` — giữ nguyên tên
- Legacy (vẫn hỗ trợ): `follower_desc`, `rating_desc`, `price_asc`, `price_desc`, `newest`, `featured`.

### 4.2.1. minPrice trong search
- Cột denormalized `kol_profile.min_price` = MIN(price) từ `kol_pricing_package`.
- Response map `null` → `0`; FE coi `minPrice <= 0` là "Liên hệ".
- Backfill: migration `V19`, `V25`.

### 4.3. Pagination
- Dùng `Pageable` của Spring Data.
- Response format:
  ```json
  {
    "content": [...],
    "page": 0,
    "size": 20,
    "totalElements": 123,
    "totalPages": 7
  }
  ```

### 4.4. Triển khai kỹ thuật
- [ ] Dùng **JPA Specification** (hoặc Querydsl nếu muốn gọn hơn) để build query động — **không** nối string SQL bằng tay.
- [ ] Thêm index ở DB cho các cột filter nhiều: `kol_profile.status`, `kol_social_channel.platform`, `kol_social_channel.follower_count`, `kol_pricing_package.price`.
- [ ] Full-text search trên `displayName` + `bio`: dùng PostgreSQL `tsvector` + GIN index (migration riêng).

### 4.5. API endpoints
| Method | Path | Role | Mô tả |
|--------|------|------|-------|
| GET | `/api/v1/kols/search` | public/BRAND | Tìm kiếm có filter + sort + paging |
| GET | `/api/v1/kols/featured` | public | Top KOL nổi bật (cache 5 phút) |
| POST | `/api/v1/brands/me/favorites/{kolId}` | BRAND | Lưu KOL yêu thích |
| DELETE | `/api/v1/brands/me/favorites/{kolId}` | BRAND | Bỏ yêu thích |
| GET | `/api/v1/brands/me/favorites` | BRAND | Danh sách yêu thích |

Ví dụ query:
```
GET /api/v1/kols/search
    ?q=beauty
    &categoryIds=1,3
    &platforms=TIKTOK,INSTAGRAM
    &minFollower=10000&maxFollower=500000
    &minPrice=1000000&maxPrice=20000000
    &city=Hanoi
    &sort=rating
    &page=0&size=20
```

> Alias `rating` tương đương `rating_desc`. Alias `followers` tương đương `follower_desc`.

### 4.2.2. KOL profile lifecycle (liên quan search)
- Chỉ KOL `APPROVED` xuất hiện trong `/kols/search` và `/kols/featured`.
- Register role KOL → tạo profile `DRAFT` (slug `kol-{userId}`).
- Public `/kols/{slug}`: anonymous chỉ thấy `APPROVED`; owner KOL xem preview profile của mình (response có `status`).

### 4.6. Caching (tuỳ chọn)
- Endpoint `featured` cache bằng `@Cacheable` (Caffeine) — refresh mỗi 5 phút.
- Tránh cache endpoint `search` tổng hợp vì nhiều tổ hợp filter.

### 4.7. Gợi ý nâng cao (không bắt buộc MVP)
- **Matching score**: tính điểm phù hợp giữa chiến dịch (ngành hàng, ngân sách, target audience) và KOL.
- **Similar KOLs**: "KOL tương tự" dựa trên overlap category + follower tier.

## Definition of Done
- Brand gọi `/kols/search` với ít nhất 3 filter cùng lúc → kết quả chính xác.
- Pagination đúng `totalElements`.
- Sort theo mọi option không lỗi.
- Test integration với Testcontainers (Postgres thật) đảm bảo query chạy đúng.
- KOL chưa duyệt **không** xuất hiện trong kết quả search.
