package com.sm.jeyz9.storemateapi.repository;

import com.sm.jeyz9.storemateapi.models.Subdistrict;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubdistrictRepository extends JpaRepository<Subdistrict, Long> {
    List<Subdistrict> findByDistrictId(Long districtId);
}
