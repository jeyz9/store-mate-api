package com.sm.jeyz9.storemateapi.repository;

import com.sm.jeyz9.storemateapi.models.ProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductStatusRepository extends JpaRepository<ProductStatus, Long> {
    
    @Query(value = """
        SELECT * FROM product_status WHERE status = :status
    """, nativeQuery = true)
    Optional<ProductStatus> findByStatus(@Param("status") String status);
}
