package com.sm.jeyz9.storemateapi.repository;

import com.sm.jeyz9.storemateapi.models.RefundRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RefundRequestRepository extends JpaRepository<RefundRequest, Long> {
    
    @Query(value = """
        SELECT COUNT(*) > 0 FROM refund_requests WHERE status = :status AND order_id = :orderId;
    """, nativeQuery = true)
    boolean existsByStatusAndOrderId(@Param("status") String status, @Param("orderId") Long orderId);
}
