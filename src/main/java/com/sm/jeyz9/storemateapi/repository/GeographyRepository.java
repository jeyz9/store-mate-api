package com.sm.jeyz9.storemateapi.repository;

import com.sm.jeyz9.storemateapi.models.Geography;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GeographyRepository extends JpaRepository<Geography, Integer> {
}
