ALTER TABLE product
    ADD COLUMN attachment_url VARCHAR(500);

COMMENT ON COLUMN product.attachment_url IS 'Optional contract / terms document URL for KOLs to review before applying';
