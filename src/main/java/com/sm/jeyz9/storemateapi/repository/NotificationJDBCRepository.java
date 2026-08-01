package com.sm.jeyz9.storemateapi.repository;

import com.sm.jeyz9.storemateapi.dto.NotificationTableDTO;
import com.sm.jeyz9.storemateapi.dto.NotifyResponseDTO;
import com.sm.jeyz9.storemateapi.models.NotifyTypeName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class NotificationJDBCRepository {
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    
    @Autowired
    public NotificationJDBCRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }
    
    public Optional<NotificationTableDTO> getAllNotify(Long userId, String type) {
        String sql = """
            SELECT
                (
                    SELECT COALESCE(json_agg(t), '[]') FROM (
                        SELECT DISTINCT n2.id, n2.title, n2.message, n2.created_at,
                            (
                                CASE WHEN n2.id IN (SELECT nr2.notify_id FROM notification_recipients nr2 WHERE nr2.recipient_id = :userId AND n2.id = nr2.notify_id LIMIT 1) THEN nr2.is_read ELSE FALSE END
                            ) AS "is_read"
                        FROM notifications n2
                                 LEFT JOIN notification_recipients nr2 ON nr2.notify_id = n2.id
                                 LEFT JOIN users u2 ON u2.id = nr2.recipient_id
                                 LEFT JOIN user_role ur2 ON ur2.user_id = u2.id
                                 LEFT JOIN roles r2 ON ur2.role_id = r2.id
                        WHERE (nr2.recipient_id = :userId OR n2.send_to IN ('ALL', r2.role_name))
                          AND (COALESCE(:type, NULL) IS NULL OR n2.notify_type IN (:type))
                        ORDER BY n2.created_at DESC
                    ) t
                ) AS notifications,
                count(*) AS totalUnread,
                (
                    SELECT(COALESCE(json_agg(t), '[]')) FROM (SELECT DISTINCT n2.notify_type, COUNT(*)AS "unread" FROM notifications n2
                    LEFT JOIN notification_recipients nr2 ON n2.id = nr2.notify_id
                    LEFT JOIN users u2 ON u2.id = nr2.recipient_id
                    LEFT JOIN user_role ur2 ON ur2.user_id = u2.id
                    LEFT JOIN roles r2 ON ur2.role_id = r2.id
                    WHERE (nr2.recipient_id = :userId OR n2.send_to IN ('ALL', r2.role_name)) AND (nr2.is_read IS FALSE OR nr2.is_read IS NULL)
                    GROUP BY n2.notify_type) t
                ) AS unreadByCategory
            
            FROM notifications n
            LEFT JOIN notification_recipients nr ON n.id = nr.notify_id
            LEFT JOIN users u ON u.id = nr.recipient_id
            LEFT JOIN user_role ur ON ur.user_id = u.id
            LEFT JOIN roles r ON ur.role_id = r.id
            WHERE (nr.recipient_id = :userId OR n.send_to IN ('ALL', r.role_name)) AND (nr.is_read IS FALSE OR nr.is_read IS NULL)
            LIMIT 1;
        """;
        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
            String notifications = rs.getString("notifications");
            int totalUnread = rs.getInt("totalUnread");
            String unreadByCategory = rs.getString("unreadByCategory");
            
            try {
                List<NotifyResponseDTO> notify = objectMapper.readValue(notifications, new TypeReference<List<NotifyResponseDTO>>() {});
                Map<NotifyTypeName, Integer> unread = objectMapper.readValue(unreadByCategory, new TypeReference<Map<NotifyTypeName, Integer>>() {});
                return Optional.of(NotificationTableDTO.builder()
                        .notifyList(notify)
                        .totalUnread(totalUnread)
                        .unreadByCategory(unread)
                        .build());
            } catch (JacksonException e) {
                throw new RuntimeException(e);
            }
        }, userId, type);
    }
}
