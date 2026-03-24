package com.sm.jeyz9.storemateapi.repository;

import com.sm.jeyz9.storemateapi.models.Cart;
import com.sm.jeyz9.storemateapi.models.CartStatusName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {
    @Query("SELECT c FROM Cart c WHERE c.user.id = :userId AND c.status = :status")
    Optional<Cart> findCartByStatusAndUserId(@Param("status") CartStatusName status, @Param("userId") Long userId);
}
