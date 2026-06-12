package com.sm.jeyz9.storemateapi.repository;

import com.sm.jeyz9.storemateapi.models.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    @Query(value = """
        SELECT * FROM reviews WHERE product_id = :productId;
    """, nativeQuery = true)
    List<Review> findAllByProductId(@Param("productId") Long productId);
    
    @Query(value = """
        SELECT COALESCE(ROUND(AVG(review_score)::numeric, 2), 0)
        FROM reviews
        WHERE product_id = :productId;
    """, nativeQuery = true)
    Float findRatingByProductId(@Param("productId") Long productId);

    @Query(value = """
        SELECT EXISTS (
            SELECT 1 FROM reviews WHERE order_item_id = :orderItemId
        )
    """, nativeQuery = true)
    boolean existsByOrderItemId(@Param("orderItemId") Long orderItemId);

    @Query(value = """
    SELECT * FROM reviews WHERE order_item_id = :orderItemId
    LIMIT 1
    """, nativeQuery = true)
    Optional<Review> findByOrderItemId(@Param("orderItemId") Long orderItemId);
}
