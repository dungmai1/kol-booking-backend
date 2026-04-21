# Phase 03 — Hồ sơ User / KOL / Brand

**Mục tiêu**: Sau khi đăng ký, user cần hoàn thiện hồ sơ tương ứng với role. Brand nhập thông tin doanh nghiệp; KOL nhập thông tin cá nhân, kênh social, bảng giá, portfolio.

## Checklist

### 3.1. Entity & Migration

**`KolProfile`** — 1-1 với `AppUser` có role = `KOL`:
- `userId` (FK → app_user)
- `displayName`, `slug` (unique, dùng cho URL)
- `avatarUrl`, `coverUrl`
- `bio`, `gender`, `dateOfBirth`
- `city`, `country`
- `categories` — nhiều-nhiều với bảng `category` (VD: Beauty, Fashion, Tech, Food, Lifestyle…)
- `status` (`DRAFT` | `PENDING_REVIEW` | `APPROVED` | `REJECTED`)

**`KolSocialChannel`** — 1-n với KolProfile:
- `platform` (`TIKTOK` | `INSTAGRAM` | `YOUTUBE` | `FACEBOOK`)
- `url`, `username`
- `followerCount`, `engagementRate`
- `verified` (đã xác minh sở hữu kênh hay chưa)

**`KolPricingPackage`** — 1-n với KolProfile:
- `type` (`POST` | `VIDEO` | `LIVESTREAM` | `STORY` | `COMBO`)
- `platform`
- `price` (VND)
- `description`

**`KolPortfolioItem`** — 1-n:
- `title`, `mediaUrl`, `mediaType` (`IMAGE` | `VIDEO` | `LINK`), `campaignName`

**`BrandProfile`** — 1-1 với user role `BRAND`:
- `companyName`, `taxCode`, `industry`
- `logoUrl`, `website`
- `contactName`, `contactPhone`, `address`
- `status` (`PENDING_REVIEW` | `APPROVED` | `REJECTED`)

**`Category`**: `id`, `name`, `slug`, `parentId` (hỗ trợ danh mục cha-con).

Migration: `V3__create_profiles.sql`, `V4__create_categories.sql`.

### 3.2. Upload ảnh/video
- [ ] Chọn 1 phương án lưu trữ:
  - **Dev**: ghi vào thư mục `uploads/` local.
  - **Production**: S3/Cloudinary/Firebase Storage.
- [ ] Endpoint `POST /api/v1/files/upload` trả về URL.
- [ ] Giới hạn dung lượng, kiểm tra MIME type (chỉ chấp nhận `image/*`, `video/mp4`).

### 3.3. API endpoints

**KOL**
| Method | Path | Role | Mô tả |
|--------|------|------|-------|
| GET | `/api/v1/kols/me` | KOL | Lấy profile của chính mình |
| PUT | `/api/v1/kols/me` | KOL | Cập nhật profile |
| POST | `/api/v1/kols/me/submit` | KOL | Gửi duyệt (chuyển sang `PENDING_REVIEW`) |
| POST | `/api/v1/kols/me/channels` | KOL | Thêm kênh social |
| DELETE | `/api/v1/kols/me/channels/{id}` | KOL | Xoá kênh |
| POST | `/api/v1/kols/me/packages` | KOL | Thêm gói giá |
| POST | `/api/v1/kols/me/portfolio` | KOL | Thêm portfolio item |
| GET | `/api/v1/kols/{slug}` | public | Xem trang công khai (chỉ khi APPROVED) |

**Brand**
| Method | Path | Role | Mô tả |
|--------|------|------|-------|
| GET | `/api/v1/brands/me` | BRAND | Lấy profile |
| PUT | `/api/v1/brands/me` | BRAND | Cập nhật |
| POST | `/api/v1/brands/me/submit` | BRAND | Gửi duyệt |

**Category**
| Method | Path | Role | |
|--------|------|------|-|
| GET | `/api/v1/categories` | public | Danh sách có phân cấp |

### 3.4. Validation quan trọng
- `slug` phải viết thường, không dấu, không khoảng trắng.
- `followerCount` ≥ 0; `engagementRate` 0–100.
- Email/phone đúng format.
- File upload ≤ 10MB ảnh, ≤ 100MB video.

## Definition of Done
- KOL có thể tạo profile → submit → trạng thái chuyển đúng.
- Brand có thể tạo profile → submit.
- Upload ảnh nhận về URL truy cập được.
- Trang public `/api/v1/kols/{slug}` **không** trả về KOL chưa duyệt.
- Có seed data: tối thiểu 10 category mặc định qua migration.
