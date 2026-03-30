package com.sm.jeyz9.storemateapi.repository;

import com.sm.jeyz9.storemateapi.models.UserAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserAddressRepository extends JpaRepository<UserAddress, Long> {
    @Query("SELECT a FROM UserAddress a WHERE a.user.id = :userId ORDER BY a.isDefault DESC, a.createdAt DESC")
    List<UserAddress> findByUserIdOrderByIsDefaultDescCreatedAtDesc(Long userId);
    
    @Modifying
    @Query("UPDATE UserAddress a SET a.isDefault = false WHERE a.user.id = :userId")
    void resetDefaultAddress(@Param("userId") Long userId);
    
    Optional<UserAddress> findByUserIdAndIsDefaultTrue(Long userId);
}
