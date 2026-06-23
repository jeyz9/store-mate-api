ALTER TABLE notifications ADD COLUMN IF NOT EXISTS notify_type VARCHAR(50);
DO $$
    BEGIN
        IF NOT EXISTS(
           SELECT 1 FROM pg_constraint WHERE conname = 'notify_type_check'
        )
           THEN
                ALTER TABLE notifications ADD CONSTRAINT notify_type_check CHECK ( notify_type IN ('STORE', 'ORDERED', 'REFUNDED') );
        END IF;
END $$
