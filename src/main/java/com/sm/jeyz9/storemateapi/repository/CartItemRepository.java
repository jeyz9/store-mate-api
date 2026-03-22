package com.sm.jeyz9.storemateapi.repository;


import com.sm.jeyz9.storemateapi.models.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    
    @Query(value = """
        SELECT * FROM cart_items WHERE cart_id = :cartId AND product_id = :productId;
    """, nativeQuery = true)
    Optional<CartItem> findCartItemByIdAndCartId(@Param("cartId") Long cartId, @Param("productId") Long productId);

    List<CartItem> findByCartId(Long cartId);
}
