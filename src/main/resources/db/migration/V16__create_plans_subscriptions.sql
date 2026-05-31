CREATE TABLE plan (
    id             BIGSERIAL PRIMARY KEY,
    code           VARCHAR(50)   NOT NULL UNIQUE,
    name           VARCHAR(150)  NOT NULL,
    description    TEXT,
    price          NUMERIC(18,2) NOT NULL DEFAULT 0,
    currency       VARCHAR(10)   NOT NULL DEFAULT 'VND',
    billing_cycle  VARCHAR(32)   NOT NULL DEFAULT 'MONTHLY',
    duration_days  INTEGER       NOT NULL DEFAULT 30,
    features       TEXT,
    target_role    VARCHAR(32)   NOT NULL DEFAULT 'BRAND',
    active         BOOLEAN       NOT NULL DEFAULT TRUE,
    sort_order     INTEGER       NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_plan_active      ON plan(active);
CREATE INDEX idx_plan_target_role ON plan(target_role);

CREATE TABLE subscription (
    id             BIGSERIAL PRIMARY KEY,
    user_id        BIGINT        NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    plan_id        BIGINT        NOT NULL REFERENCES plan(id),
    status         VARCHAR(32)   NOT NULL DEFAULT 'PENDING_PAYMENT',
    started_at     TIMESTAMPTZ,
    expires_at     TIMESTAMPTZ,
    auto_renew     BOOLEAN       NOT NULL DEFAULT FALSE,
    amount_paid    NUMERIC(18,2) NOT NULL DEFAULT 0,
    currency       VARCHAR(10)   NOT NULL DEFAULT 'VND',
    external_ref   VARCHAR(150),
    cancelled_at   TIMESTAMPTZ,
    cancel_reason  TEXT,
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_subscription_user        ON subscription(user_id);
CREATE INDEX idx_subscription_status      ON subscription(status);
CREATE INDEX idx_subscription_expires_at  ON subscription(expires_at);
CREATE UNIQUE INDEX uq_subscription_external_ref ON subscription(external_ref) WHERE external_ref IS NOT NULL;

INSERT INTO plan (code, name, description, price, currency, billing_cycle, duration_days, features, target_role, active, sort_order) VALUES
    ('FREE',
     'Free',
     'Gói miễn phí dành cho Brand mới bắt đầu khám phá nền tảng.',
     0,
     'VND',
     'MONTHLY',
     30,
     'Tìm kiếm KOL cơ bản;Xem hồ sơ KOL công khai;Tối đa 2 booking đang hoạt động',
     'BRAND',
     TRUE,
     1),
    ('PRO',
     'Pro',
     'Gói tiêu chuẩn cho Brand chạy chiến dịch thường xuyên.',
     499000,
     'VND',
     'MONTHLY',
     30,
     'Bộ lọc nâng cao;Xem báo cáo chiến dịch;Tối đa 20 booking đang hoạt động;Ưu tiên hỗ trợ',
     'BRAND',
     TRUE,
     2),
    ('ENTERPRISE',
     'Enterprise',
     'Gói cao cấp dành cho Agency và Brand lớn.',
     1990000,
     'VND',
     'MONTHLY',
     30,
     'Không giới hạn booking;Quản lý nhiều tài khoản phụ;Báo cáo nâng cao;Hỗ trợ chuyên gia 1-1',
     'BRAND',
     TRUE,
     3);
