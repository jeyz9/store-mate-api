ALTER TABLE notifications ADD COLUMN IF NOT EXISTS send_to VARCHAR(50) DEFAULT NULL;

DO $$
    BEGIN 
        IF NOT EXISTS (
            SELECT 1 FROM pg_constraint WHERE conname = 'notification_send_to_check'
        )
        THEN 
            ALTER TABLE notifications ADD CONSTRAINT notification_send_to_check CHECK (send_to IN ('ALL', 'MODERATOR', 'CUSTOMER'));
        END IF;
END $$;
