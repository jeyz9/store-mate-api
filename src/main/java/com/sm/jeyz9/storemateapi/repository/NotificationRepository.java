package com.sm.jeyz9.storemateapi.repository;

import com.sm.jeyz9.storemateapi.dto.NotifyOwnerResponseDTO;
import com.sm.jeyz9.storemateapi.dto.NotifyResponseDTO;
import com.sm.jeyz9.storemateapi.models.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    @Query(value = """
        SELECT DISTINCT n.id, n.title, n.message, n.created_at FROM notifications n
        LEFT JOIN notification_recipients nr ON nr.notify_id = n.id
        LEFT JOIN users u ON u.id = nr.recipient_id
        LEFT JOIN user_role ur ON ur.user_id = u.id
        LEFT JOIN roles r ON ur.role_id = r.id
        WHERE nr.recipient_id = :userId OR n.send_to IN ('ALL', r.role_name)
        ORDER BY n.created_at DESC;
    """, nativeQuery = true)
    List<NotifyResponseDTO> getAllNotifyByUserId(@Param("userId") Long userId);

    @Query(value = """
        SELECT DISTINCT n.id, n.title, n.message, n.send_to, n.created_at FROM notifications n WHERE n.send_to IN ('ALL', 'CUSTOMER', 'MODERATOR')
    """, nativeQuery = true)
    List<NotifyOwnerResponseDTO> getAllNotify();
    
}
