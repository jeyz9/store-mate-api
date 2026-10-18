package com.sm.jeyz9.storemateapi.repository;

import com.sm.jeyz9.storemateapi.models.Order;
import com.sm.jeyz9.storemateapi.models.OrderStatusName;
import com.sm.jeyz9.storemateapi.models.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByStripePaymentIntent(String stripePaymentIntent);

    List<Order> findAllByUser(User user);

    @Query(value = """
        SELECT COALESCE(json_agg(json_build_object(
            'id', o.id,
            'orderNo', o.order_no,
            'status', o.status,
            'checkoutType', o.checkout_type,
            'total', o.total_price,
            'createdAt', o.created_at,
            'reason', (
                SELECT rr.reason
                FROM refund_requests rr
                WHERE rr.order_id = o.id
                ORDER BY rr.requested_at
                LIMIT 1
            ),
            'orderItems', COALESCE((
                SELECT json_agg(json_build_object(
                    'id', oi.id,
                    'productName', p.name,
                    'imageUrl', (
                        SELECT pi.image_url
                        FROM product_images pi
                        WHERE pi.product_id = p.id
                        ORDER BY pi.id
                        LIMIT 1
                    ),
                    'price', p.price,
                    'quantity', oi.quantity,
                    'subTotal', p.price * oi.quantity,
                    'is_review', EXISTS (
                        SELECT 1 FROM reviews r WHERE r.order_item_id = oi.id
                    )
                ) ORDER BY oi.id)
                FROM order_items oi
                JOIN products p ON p.id = oi.product_id
                WHERE oi.order_id = o.id
            ), '[]'::json),
            'orderAddress', COALESCE((
                SELECT json_agg(json_build_object(
                    'id', oa.id,
                    'recipientName', oa.recipient_name,
                    'phone', oa.phone,
                    'streetAddress', oa.street_address,
                    'subdistrict', sd.name,
                    'district', d.name,
                    'province', pr.name,
                    'zipcode', z.zipcode
                ) ORDER BY oa.id)
                FROM order_address oa
                JOIN zipcode z ON z.id = oa.zipcode_id
                JOIN subdistricts sd ON sd.id = z.subdistrict_id
                JOIN districts d ON d.id = z.district_id
                JOIN provinces pr ON pr.id = z.province_id
                WHERE oa.order_id = o.id
            ), '[]'::json)
        ) ORDER BY o.updated_at DESC), '[]'::json)::text
        FROM orders o
        WHERE o.user_id = :userId
          AND (:status = 'ALL' OR o.status = :status)
        """, nativeQuery = true)
    String findUserOrdersJson(@Param("userId") Long userId, @Param("status") String status);

    interface ModeratorOrderProjection {
        Long getId();
        String getOrderNo();
        String getRecipientName();
        String getPhone();
        LocalDateTime getCreatedAt();
        Double getTotal();
        String getShippingFrom();
        String getStatus();
        Boolean getPrinted();
    }

    @EntityGraph(attributePaths = {
            "orderItems",
            "orderItems.product",
            "orderAddresses",
            "orderAddresses.zipcode"
    })
    Optional<Order> findById(Long id);
    
    @Query(value = """
        SELECT o.status FROM orders o WHERE o.order_no = :orderNo;
    """, nativeQuery = true)
    String findOrderStatusByOrderNo(String orderNo);

    @EntityGraph(attributePaths = {
            "orderItems",
            "orderItems.product",
            "orderAddresses",
            "orderAddresses.zipcode"
    })
    Optional<Order> findOrderByOrderNoAndUserId(String orderNo, Long userId);

    @EntityGraph(attributePaths = {
            "orderItems",
            "orderItems.product",
            "orderAddresses",
            "orderAddresses.zipcode"
    })
    Optional<Order> findOneByOrderNo(String orderNo);

    @Query(value = """
        SELECT o.id AS "id",
               o.order_no AS "orderNo",
               (SELECT oa.recipient_name FROM order_address oa
                WHERE oa.order_id = o.id ORDER BY oa.id LIMIT 1) AS "recipientName",
               (SELECT oa.phone FROM order_address oa
                WHERE oa.order_id = o.id ORDER BY oa.id LIMIT 1) AS "phone",
               o.created_at AS "createdAt",
               COALESCE((SELECT SUM(oi.quantity * p.price)
                         FROM order_items oi
                         JOIN products p ON p.id = oi.product_id
                         WHERE oi.order_id = o.id), 0)::double precision AS "total",
               o.order_channel AS "shippingFrom",
               o.status AS "status",
               o.printed AS "printed"
        FROM orders o
        WHERE (:keyword IS NULL OR EXISTS (
                  SELECT 1 FROM order_address oa
                  WHERE oa.order_id = o.id
                    AND (LOWER(oa.recipient_name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                         OR oa.phone = :keyword)
              ))
          AND (CAST(:start AS timestamp) IS NULL OR o.created_at >= :start)
          AND (CAST(:end AS timestamp) IS NULL OR o.created_at <= :end)
          AND NOT EXISTS (
                  SELECT 1
                  FROM order_items oi
                  JOIN products p ON p.id = oi.product_id
                  JOIN product_status ps ON ps.id = p.status_id
                  WHERE oi.order_id = o.id AND ps.status = 'DELETED'
              )
        ORDER BY o.created_at DESC
        """, countQuery = """
        SELECT COUNT(*)
        FROM orders o
        WHERE (:keyword IS NULL OR EXISTS (
                  SELECT 1 FROM order_address oa
                  WHERE oa.order_id = o.id
                    AND (LOWER(oa.recipient_name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                         OR oa.phone = :keyword)
              ))
          AND (CAST(:start AS timestamp) IS NULL OR o.created_at >= :start)
          AND (CAST(:end AS timestamp) IS NULL OR o.created_at <= :end)
          AND NOT EXISTS (
                  SELECT 1
                  FROM order_items oi
                  JOIN products p ON p.id = oi.product_id
                  JOIN product_status ps ON ps.id = p.status_id
                  WHERE oi.order_id = o.id AND ps.status = 'DELETED'
              )
        """, nativeQuery = true)
    Page<ModeratorOrderProjection> findOrders(
            @Param("keyword") String keyword,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            Pageable pageable
    );
}
