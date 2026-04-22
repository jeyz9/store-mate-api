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
        SELECT o.status FROM orders o WHERE o.order_id = :orderId;
    """, nativeQuery = true)
    String findOrderStatusById(Long orderId);
}
