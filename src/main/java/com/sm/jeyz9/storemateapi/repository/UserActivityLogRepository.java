package com.sm.jeyz9.storemateapi.repository;

import com.sm.jeyz9.storemateapi.models.UserActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface UserActivityLogRepository extends JpaRepository<UserActivityLog, Long> {
    
    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO user_activity_logs(user_id, activity_date, created_at)
        VALUES (:userId, CURRENT_DATE, NOW())
        ON CONFLICT (user_id, activity_date)
        DO NOTHING
    """, nativeQuery = true)
    void saveDailyActivity(@Param("userId") Long userId);
}
