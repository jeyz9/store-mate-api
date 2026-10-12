package com.sm.jeyz9.storemateapi;

import com.sm.jeyz9.storemateapi.dto.NotificationTableDTO;
import com.sm.jeyz9.storemateapi.dto.NotifyRequestDTO;
import com.sm.jeyz9.storemateapi.exceptions.WebException;
import com.sm.jeyz9.storemateapi.models.Notification;
import com.sm.jeyz9.storemateapi.models.NotifyTypeName;
import com.sm.jeyz9.storemateapi.models.SendTo;
import com.sm.jeyz9.storemateapi.models.User;
import com.sm.jeyz9.storemateapi.repository.NotificationJDBCRepository;
import com.sm.jeyz9.storemateapi.repository.NotificationRepository;
import com.sm.jeyz9.storemateapi.repository.UserRepository;
import com.sm.jeyz9.storemateapi.services.MessagingService;
import com.sm.jeyz9.storemateapi.services.impl.NotificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NotificationServiceTest {

    private final User sender = User.builder().id(42L).email("owner@example.com").build();
    private RecordingUserRepository userRepository;
    private RecordingNotificationRepository notificationRepository;
    private RecordingNotificationJdbcRepository notificationJdbcRepository;
    private RecordingMessagingService messagingService;
    private NotificationServiceImpl notificationService;

    @BeforeEach
    void setUp() {
        userRepository = new RecordingUserRepository();
        userRepository.user = Optional.of(sender);
        notificationRepository = new RecordingNotificationRepository();
        notificationJdbcRepository = new RecordingNotificationJdbcRepository();
        messagingService = new RecordingMessagingService();
        notificationService = new NotificationServiceImpl(
                notificationRepository.asRepository(),
                userRepository.asRepository(),
                messagingService,
                new ModelMapper(),
                notificationJdbcRepository
        );
    }

    @Test
    void sendNotificationSavesAndBroadcastsNotification() {
        NotifyRequestDTO request = new NotifyRequestDTO("Store update", "New announcement", "ALL");

        String result = notificationService.sendNotification("owner@example.com", request);

        assertEquals("Send notification successfully", result);
        assertEquals("owner@example.com", userRepository.requestedEmail);
        assertNotNull(notificationRepository.savedNotification);
        assertEquals("Store update", notificationRepository.savedNotification.getTitle());
        assertEquals("New announcement", notificationRepository.savedNotification.getMessage());
        assertSame(sender, notificationRepository.savedNotification.getSender());
        assertEquals(SendTo.ALL, notificationRepository.savedNotification.getSendTo());
        assertEquals(NotifyTypeName.STORE, notificationRepository.savedNotification.getNotifyType());
        assertSame(notificationRepository.savedNotification, messagingService.broadcastNotification);
    }

    @Test
    void getAllNotifyUserTurnsAllTypeIntoNullFilter() {
        NotificationTableDTO expected = new NotificationTableDTO();
        notificationJdbcRepository.result = Optional.of(expected);

        Optional<NotificationTableDTO> result = notificationService.getAllNotifyUser(
                "owner@example.com", "ALL"
        );

        assertSame(expected, result.orElseThrow());
        assertEquals(42L, notificationJdbcRepository.requestedUserId);
        assertEquals(null, notificationJdbcRepository.requestedType);
    }

    @Test
    void markAsReadUsesTheCurrentUsersIdAndNotificationId() {
        String result = notificationService.markAsRead("owner@example.com", 15L);

        assertEquals("Notification marked as read successfully!", result);
        assertEquals(42L, notificationRepository.markedReadUserId);
        assertEquals(15L, notificationRepository.markedReadNotificationId);
    }

    @Test
    void markAllAsReadUsesTheCurrentUsersId() {
        String result = notificationService.markAllAsRead("owner@example.com");

        assertEquals("Notification marked all as read successfully!", result);
        assertEquals(42L, notificationRepository.markedAllReadUserId);
    }

    @Test
    void getAllNotifyUserRejectsUnknownUser() {
        userRepository.user = Optional.empty();

        WebException exception = assertThrows(WebException.class, () ->
                notificationService.getAllNotifyUser("missing@example.com", "ALL")
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertEquals("User not found.", exception.getMessage());
        assertEquals(0, notificationJdbcRepository.callCount);
    }

    private static class RecordingUserRepository implements InvocationHandler {
        private Optional<User> user = Optional.empty();
        private String requestedEmail;

        private UserRepository asRepository() {
            return proxy(UserRepository.class, this);
        }

        @Override
        public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) {
            if (method.getName().equals("findUserByEmail")) {
                requestedEmail = (String) args[0];
                return user;
            }
            return null;
        }
    }

    private static class RecordingNotificationRepository implements InvocationHandler {
        private Notification savedNotification;
        private Long markedReadUserId;
        private Long markedReadNotificationId;
        private Long markedAllReadUserId;

        private NotificationRepository asRepository() {
            return proxy(NotificationRepository.class, this);
        }

        @Override
        public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) {
            switch (method.getName()) {
                case "save" -> {
                    savedNotification = (Notification) args[0];
                    return savedNotification;
                }
                case "markAsRead" -> {
                    markedReadUserId = (Long) args[0];
                    markedReadNotificationId = (Long) args[1];
                    return 1;
                }
                case "markAllAsRead" -> {
                    markedAllReadUserId = (Long) args[0];
                    return 1;
                }
                default -> {
                    return null;
                }
            }
        }
    }

    private static class RecordingNotificationJdbcRepository extends NotificationJDBCRepository {
        private Optional<NotificationTableDTO> result = Optional.empty();
        private Long requestedUserId;
        private String requestedType;
        private int callCount;

        private RecordingNotificationJdbcRepository() {
            super(null, null);
        }

        @Override
        public Optional<NotificationTableDTO> getAllNotify(Long userId, String type) {
            requestedUserId = userId;
            requestedType = type;
            callCount++;
            return result;
        }
    }

    private static class RecordingMessagingService extends MessagingService {
        private Notification broadcastNotification;

        private RecordingMessagingService() {
            super(null, null, null, null);
        }

        @Override
        public void sendNotifyToUser(Notification notification) {
            broadcastNotification = notification;
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }
}
