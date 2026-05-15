package com.sm.jeyz9.storemateapi.repository;

import com.sm.jeyz9.storemateapi.models.Order;
import com.sm.jeyz9.storemateapi.models.OrderStatusName;
import com.sm.jeyz9.storemateapi.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByStripePaymentIntent(String stripePaymentIntent);

    List<Order> findAllByUser(User user);
    
    @Query(value = """
        SELECT o.status FROM orders o WHERE o.order_no = :orderNo;
    """, nativeQuery = true)
    String findOrderStatusByOrderNo(String orderNo);
    
    @Query(value = """
        SELECT * FROM orders o WHERE o.order_no = :orderNo AND o.user_id = :userId;
    """, nativeQuery = true)
    Optional<Order> findOrderByOrderNoAndUserId(String orderNo, Long userId);

    @Query(value = """
        SELECT * FROM orders o WHERE o.order_no = :orderNo;
    """, nativeQuery = true)
    Optional<Order> findOneByOrderNo(String orderNo);
}
