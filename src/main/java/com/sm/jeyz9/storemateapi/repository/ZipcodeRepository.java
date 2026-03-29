package com.sm.jeyz9.storemateapi.repository;

import com.sm.jeyz9.storemateapi.models.Zipcode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ZipcodeRepository extends JpaRepository<Zipcode, Long> {
    Optional<Zipcode> findBySubdistrictId(Long subdistrictId);
}