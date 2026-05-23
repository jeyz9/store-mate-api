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

    @Query(value = """
        SELECT DISTINCT o FROM Order o
        LEFT JOIN FETCH o.orderItems oi
        LEFT JOIN FETCH oi.product p
        LEFT JOIN FETCH p.productImage
        LEFT JOIN FETCH o.orderAddresses oa
        LEFT JOIN FETCH oa.zipcode z
        LEFT JOIN FETCH z.subdistrict
        LEFT JOIN FETCH z.district
        LEFT JOIN FETCH z.province
        WHERE o.user = :user AND o.status = :status
    """)
    List<Order> findAllByUserAndStatus(@Param("user") User user, @Param("status") OrderStatusName status);

    @Query(value = """
        SELECT DISTINCT o FROM Order o
        LEFT JOIN FETCH o.orderItems oi
        LEFT JOIN FETCH oi.product p
        LEFT JOIN FETCH p.productImage
        LEFT JOIN FETCH o.orderAddresses oa
        LEFT JOIN FETCH oa.zipcode z
        LEFT JOIN FETCH z.subdistrict
        LEFT JOIN FETCH z.district
        LEFT JOIN FETCH z.province
        WHERE o.user = :user
    """)
    List<Order> findAllByUser(@Param("user") User user);

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

    @Query("""
        SELECT o
        FROM Order o
        LEFT JOIN o.orderAddresses oa
        WHERE
            (
                :keyword IS NULL
                OR LOWER(oa.recipientName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR oa.phone = :keyword
            )
        AND (
                CAST(:start AS timestamp) IS NULL OR o.createdAt >= :start
            )
        AND (
                CAST(:end AS timestamp) IS NULL OR o.createdAt <= :end
            )
        ORDER BY o.createdAt DESC
    """)
    Page<Order> findOrders(
            @Param("keyword") String keyword,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            Pageable pageable
    );
}
