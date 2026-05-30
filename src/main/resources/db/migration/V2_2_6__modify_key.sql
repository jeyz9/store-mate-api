DO $$
    BEGIN

        IF EXISTS (
            SELECT 1
            FROM pg_constraint
            WHERE conname = 'fk_reviews_order_items'
        ) THEN

            ALTER TABLE reviews
                DROP CONSTRAINT fk_reviews_order_items;

        END IF;

        ALTER TABLE reviews
            ADD CONSTRAINT fk_reviews_order_items
                FOREIGN KEY (order_item_id)
                    REFERENCES order_items(id)
                    ON DELETE CASCADE;
END $$;