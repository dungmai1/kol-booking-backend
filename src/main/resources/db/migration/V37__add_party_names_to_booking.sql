ALTER TABLE booking
    ADD COLUMN IF NOT EXISTS brand_company_name VARCHAR(200),
    ADD COLUMN IF NOT EXISTS kol_display_name VARCHAR(150);
