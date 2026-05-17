package com.sm.jeyz9.storemateapi.repository;

import com.sm.jeyz9.storemateapi.models.OrderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, Long> {
    
    @Query(value = """
        SELECT * FROM order_status_history WHERE order_id = :orderId;
    """, nativeQuery = true)
    List<OrderStatusHistory> findByOrderId(@Param("orderId") Long orderId);
}
