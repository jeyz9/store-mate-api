ALTER TABLE store_info ADD COLUMN IF NOT EXISTS zipcode_id BIGINT;

DO $$
    BEGIN
        IF NOT EXISTS(
            SELECT 1 FROM pg_constraint WHERE conname = 'fk_store_info_zipcode'
        )
        THEN
            ALTER TABLE store_info ADD CONSTRAINT fk_store_info_zipcode FOREIGN KEY (zipcode_id) REFERENCES zipcode(id);
    END IF;
END $$