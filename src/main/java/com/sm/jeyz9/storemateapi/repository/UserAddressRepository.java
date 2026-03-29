package com.sm.jeyz9.storemateapi.repository;

import com.sm.jeyz9.storemateapi.models.UserAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserAddressRepository extends JpaRepository<UserAddress, Long> {
    UserAddress findByIsDefault(Boolean isDefault);
}
