DO $$
    BEGIN 
        IF NOT EXISTS(
            SELECT 1 FROM pg_constraint WHERE conname = 'fk_reviews_order_items'
        )
            THEN
                ALTER TABLE reviews ADD COLUMN order_item_id BIGINT DEFAULT NULL;
                ALTER TABLE reviews ADD CONSTRAINT fk_reviews_order_items FOREIGN KEY (order_item_id) REFERENCES order_items (id);
                ALTER TABLE reviews ADD CONSTRAINT reviews_order_items_check UNIQUE (order_item_id);
        END IF;
END $$;
