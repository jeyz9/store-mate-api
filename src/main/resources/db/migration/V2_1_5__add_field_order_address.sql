DO $$
    BEGIN
        IF EXISTS (
           SELECT 1 FROM information_schema.columns WHERE table_name = 'order_address' AND column_name = 'recipientname'
        ) THEN
           ALTER TABLE order_address RENAME COLUMN recipientname TO recipient_name;
        END IF;
END $$;


ALTER TABLE order_address ADD COLUMN IF NOT EXISTS recipient_name VARCHAR(255);
ALTER TABLE order_address ADD COLUMN IF NOT EXISTS phone VARCHAR(10);