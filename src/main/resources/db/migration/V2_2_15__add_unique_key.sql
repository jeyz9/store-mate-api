DO $$
    BEGIN
        IF NOT EXISTS (
           SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'uk_notification_recipient';
        ) 
           THEN ALTER TABLE notification_recipients ADD CONSTRAINT uk_notification_recipient UNIQUE (notify_id, recipient_id);
        END IF;
END $$