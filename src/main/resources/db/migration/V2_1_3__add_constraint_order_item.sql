ALTER TABLE order_items
    ADD CONSTRAINT uq_order_product
      UNIQUE(order_id, product_id);