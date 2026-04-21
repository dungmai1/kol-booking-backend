-- =====================================================================
-- Phase 07 — Reviews & notifications
-- =====================================================================

CREATE TABLE review (
    id          BIGSERIAL     PRIMARY KEY,
    booking_id  BIGINT        NOT NULL REFERENCES booking(id) ON DELETE CASCADE,
    author_id   BIGINT        NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    target_id   BIGINT        NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    direction   VARCHAR(32)   NOT NULL,
    rating      SMALLINT      NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment     TEXT,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_review_booking_direction UNIQUE (booking_id, direction)
);
CREATE INDEX idx_review_target ON review(target_id);
CREATE INDEX idx_review_author ON review(author_id);

-- =====================================================================

CREATE TABLE notification (
    id         BIGSERIAL    PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    type       VARCHAR(64)  NOT NULL,
    title      VARCHAR(255) NOT NULL,
    message    TEXT         NOT NULL,
    link       VARCHAR(500),
    read_at    TIMESTAMPTZ,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_notification_user    ON notification(user_id);
CREATE INDEX idx_notification_user_unread ON notification(user_id) WHERE read_at IS NULL;

-- =====================================================================
-- Admin audit log
-- =====================================================================

CREATE TABLE admin_audit_log (
    id          BIGSERIAL    PRIMARY KEY,
    admin_id    BIGINT       NOT NULL REFERENCES app_user(id) ON DELETE SET NULL,
    action      VARCHAR(100) NOT NULL,
    target_type VARCHAR(100),
    target_id   BIGINT,
    payload     TEXT,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_admin_audit_admin  ON admin_audit_log(admin_id);
CREATE INDEX idx_admin_audit_action ON admin_audit_log(action);
CREATE INDEX idx_admin_audit_target ON admin_audit_log(target_type, target_id);
