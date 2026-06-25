package com.sm.jeyz9.storemateapi.repository;

import com.sm.jeyz9.storemateapi.models.Province;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProvinceRepository extends JpaRepository<Province, Long> {
    List<Province> findByGeography_Id(Integer geoId);
}
