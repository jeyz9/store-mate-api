DO $$
BEGIN
    IF NOT EXISTS (
       SELECT 1 FROM pg_constraint WHERE conname = 'uq_cart_product'
    ) THEN
        ALTER TABLE cart_items
            ADD CONSTRAINT uq_cart_product
              UNIQUE(cart_id, product_id);
    END IF;
END $$;