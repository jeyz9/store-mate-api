DO $$
    BEGIN
        IF EXISTS (
           SELECT 1 FROM pg_constraint WHERE conname = 'orders_order_channel_check'
        )
           THEN
                ALTER TABLE orders DROP CONSTRAINT orders_order_channel_check;
                ALTER TABLE orders ADD CONSTRAINT orders_order_channel_check CHECK ( order_channel IN ('WEBSITE', 'LINE_OA', 'OTHER') );
    END IF;
END $$