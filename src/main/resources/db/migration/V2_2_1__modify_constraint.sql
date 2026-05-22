DO $$
    BEGIN
        IF EXISTS (
            SELECT 1
            FROM pg_constraint
            WHERE conname = 'fkbioxgbv59vetrxe0ejfubep1w'
        ) THEN
            ALTER TABLE order_items
                DROP CONSTRAINT fkbioxgbv59vetrxe0ejfubep1w;
        END IF;

        ALTER TABLE order_items
            ADD CONSTRAINT fkbioxgbv59vetrxe0ejfubep1w
                FOREIGN KEY (order_id)
                    REFERENCES orders(id)
                    ON DELETE CASCADE;

        IF EXISTS (
            SELECT 1
            FROM pg_constraint
            WHERE conname = 'fkru9uj5vrdjaarx7l5oa19fvt2'
        ) THEN
            ALTER TABLE order_address
                DROP CONSTRAINT fkru9uj5vrdjaarx7l5oa19fvt2;
        END IF;

        ALTER TABLE order_address
            ADD CONSTRAINT fkru9uj5vrdjaarx7l5oa19fvt2
                FOREIGN KEY (order_id)
                    REFERENCES orders(id)
                    ON DELETE CASCADE;

END $$;