-- Platform/system account that owns the commission wallet.
--
-- WalletService credits the platform fee to a wallet with user_id = 0 (by convention). Because
-- wallet.user_id has a FK to app_user(id), that wallet row cannot be created unless an app_user
-- with id = 0 exists — without this row, every booking settlement fails with a
-- wallet_user_id_fkey violation and no commission is ever recorded. This seeds that account.
--
-- The password hash is intentionally invalid (not BCrypt) so the account can never authenticate.
INSERT INTO app_user (id, email, password_hash, role, status, email_verified, created_at, updated_at)
VALUES (0, 'platform@system.local', 'DISABLED-SYSTEM-ACCOUNT', 'SYSTEM', 'ACTIVE', TRUE, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- Pre-create the platform wallet row so the first settlement does not race on lazy creation.
INSERT INTO wallet (user_id, balance_available, balance_held, currency, created_at, updated_at)
VALUES (0, 0, 0, 'VND', NOW(), NOW())
ON CONFLICT (user_id) DO NOTHING;
