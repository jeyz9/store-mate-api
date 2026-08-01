package com.sm.jeyz9.storemateapi.repository;

import com.sm.jeyz9.storemateapi.models.Notification;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    @Query(value = """
        SELECT n
        FROM Notification n
        WHERE n.sendTo IN ('ALL', 'CUSTOMER', 'MODERATOR')
            AND
                (
                :keyword IS NULL
                OR LOWER(n.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
            )
        ORDER BY n.createdAt DESC
    """)
    Page<Notification> findNotification(@Param("keyword") String keyword, Pageable pageable);

    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO notification_recipients(notify_id, recipient_id, is_read)
            VALUES (:notifyId, :userId, TRUE)
            ON CONFLICT (notify_id, recipient_id)
            DO UPDATE SET is_read = TRUE
        """, nativeQuery = true)
    int markAsRead(@Param("userId") Long userId, @Param("notifyId") Long notifyId);

    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO notification_recipients(notify_id, recipient_id, is_read)
        SELECT n.id, :userId, TRUE
        FROM notifications n
        WHERE NOT EXISTS (
            SELECT 1
            FROM notification_recipients nr
            WHERE nr.notify_id = n.id
            AND nr.recipient_id = :userId
        )
        ON CONFLICT (notify_id, recipient_id)
        DO UPDATE SET is_read = TRUE
    """, nativeQuery = true)
    int markAllAsRead(@Param("userId") Long userId);
}
