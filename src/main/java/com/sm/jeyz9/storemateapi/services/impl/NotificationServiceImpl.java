package com.sm.jeyz9.storemateapi.services.impl;

import com.sm.jeyz9.storemateapi.dto.NotifyOwnerResponseDTO;
import com.sm.jeyz9.storemateapi.dto.NotifyRequestDTO;
import com.sm.jeyz9.storemateapi.dto.NotifyResponseDTO;
import com.sm.jeyz9.storemateapi.exceptions.WebException;
import com.sm.jeyz9.storemateapi.models.Notification;
import com.sm.jeyz9.storemateapi.models.SendTo;
import com.sm.jeyz9.storemateapi.models.User;
import com.sm.jeyz9.storemateapi.repository.NotificationRepository;
import com.sm.jeyz9.storemateapi.repository.UserRepository;
import com.sm.jeyz9.storemateapi.services.MessagingService;
import com.sm.jeyz9.storemateapi.services.NotificationService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationService {
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final MessagingService messagingService;
    private final ModelMapper modelMapper;

    @Autowired
    public NotificationServiceImpl(NotificationRepository notificationRepository, UserRepository userRepository, MessagingService messagingService, ModelMapper modelMapper) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.messagingService = messagingService;
        this.modelMapper = modelMapper;
    }
    
    @Override
    public String sendNotification(String email, NotifyRequestDTO request) {
        User sender = userRepository.findUserByEmail(email).orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "User not found."));
        Notification notify = Notification.builder()
                .id(null)
                .title(request.getTitle())
                .message(request.getMessage())
                .sender(sender)
                .sendTo(SendTo.valueOf(request.getSendTo()))
                .createdAt(LocalDateTime.now())
                .build();
        notificationRepository.save(notify);
        messagingService.sendNotifyToUser(notify);
        return "Send notification successfully";
    }

    @Override
    public List<NotifyResponseDTO> getAllNotifyUser(String email) {
        User user = userRepository.findUserByEmail(email).orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "User not found."));
        return notificationRepository.getAllNotifyByUserId(user.getId());
    }

    @Override
    public Page<NotifyOwnerResponseDTO> getAllNotify(String keyword, Integer page, Integer size) {
        Pageable pageable = PageRequest.of(
                page,
                size
        );
        return notificationRepository.findNotification(keyword, pageable).map(this::mapToNotifyOwner);
    }

    @Override
    public String removeNotify(Long notifyId) {
        notificationRepository.deleteById(notifyId);
        return "Remove notification successfully";
    }
    
    private NotifyOwnerResponseDTO mapToNotifyOwner(Notification notification) {
        return modelMapper.map(notification, NotifyOwnerResponseDTO.class);
    }
}
