CREATE INDEX IF NOT EXISTS idx_orders_user_updated_at
    ON orders (user_id, updated_at DESC);

CREATE INDEX IF NOT EXISTS idx_orders_user_status_updated_at
    ON orders (user_id, status, updated_at DESC);

CREATE INDEX IF NOT EXISTS idx_orders_created_at_desc
    ON orders (created_at DESC);

CREATE INDEX IF NOT EXISTS idx_order_items_order_id
    ON order_items (order_id);

CREATE INDEX IF NOT EXISTS idx_order_address_order_id
    ON order_address (order_id);

CREATE INDEX IF NOT EXISTS idx_refund_requests_order_id
    ON refund_requests (order_id);

CREATE INDEX IF NOT EXISTS idx_product_images_product_id
    ON product_images (product_id);
