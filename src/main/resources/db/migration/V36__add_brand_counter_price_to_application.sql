ALTER TABLE product_application
    ADD COLUMN IF NOT EXISTS brand_counter_price DECIMAL(15, 2);
