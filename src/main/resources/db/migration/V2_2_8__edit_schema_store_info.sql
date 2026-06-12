ALTER TABLE store_info ADD COLUMN IF NOT EXISTS promotion_image VARCHAR(255) DEFAULT NULL;

ALTER TABLE store_info DROP COLUMN IF EXISTS open_time;
ALTER TABLE store_info DROP COLUMN IF EXISTS close_time;

DO $$
    BEGIN
        IF EXISTS(
            SELECT 1 FROM pg_constraint WHERE conname = 'fkfjcpitq7xp94ov0o1grdahdtk'
        )
        THEN
            ALTER TABLE store_info DROP CONSTRAINT fkfjcpitq7xp94ov0o1grdahdtk;
        END IF;
END $$;

ALTER TABLE store_info DROP COLUMN IF EXISTS owner_id;