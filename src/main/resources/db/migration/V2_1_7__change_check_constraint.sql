ALTER TABLE order_status_history DROP CONSTRAINT IF EXISTS order_status_history_status_check;

DO $$
    BEGIN IF NOT EXISTS(
        SELECT 1 FROM pg_constraint WHERE conname = 'order_status_history_check'
    ) THEN
        ALTER TABLE order_status_history
            ADD CONSTRAINT order_status_history_check
                CHECK (
                    status IN (
                               'COMPLETED', 'PENDING', 'PROCESSING', 'RECEIVED'
                        )
                    );  
    END IF;
END$$;