package za.co.urbaneye.reporthole.notification.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.urbaneye.reporthole.incident.service.impl.IncidentSseService;
import za.co.urbaneye.reporthole.notification.dto.NotificationResponse;
import za.co.urbaneye.reporthole.notification.entity.Notification;
import za.co.urbaneye.reporthole.notification.repository.INotificationRepository;
import za.co.urbaneye.reporthole.notification.service.interfaces.INotificationService;
import za.co.urbaneye.reporthole.user.entity.User;
import za.co.urbaneye.reporthole.user.exception.UserServiceException;
import za.co.urbaneye.reporthole.user.repository.IUserRepository;

import java.util.List;
import java.util.UUID;

/**
 * Persists in-app notifications and delivers them via SSE for users who are currently connected.
 *
 * <p>Offline users pick up their notifications on next page load through
 * {@link #getMyNotifications()}. No emails are sent — this is a purely in-app channel.</p>
 *
 * @author Refentse
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements INotificationService {

    private static final int MAX_RECENT = 50;

    private final INotificationRepository notificationRepository;
    private final IUserRepository userRepository;
    private final IncidentSseService sseService;

    @Override
    @Transactional
    public void notify(User recipient, String message) {
        Notification notification = Notification.builder()
                .user(recipient)
                .message(message)
                .build();
        notificationRepository.save(notification);
        sseService.pushNotification(recipient.getUserId(), message);
        log.debug("[NOTIFY] Persisted + pushed notification to user {}: {}", recipient.getUserId(), message);
    }

    @Override
    public List<NotificationResponse> getMyNotifications() {
        UUID userId = currentUserId();
        return notificationRepository
                .findByUser_UserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, MAX_RECENT))
                .stream()
                .map(n -> new NotificationResponse(n.getNotificationId(), n.getMessage(), n.isRead(), n.getCreatedAt()))
                .toList();
    }

    @Override
    public long getUnreadCount() {
        return notificationRepository.countByUser_UserIdAndReadFalse(currentUserId());
    }

    @Override
    @Transactional
    public void markRead(UUID notificationId) {
        UUID userId = currentUserId();
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new UserServiceException("Notification not found"));
        if (!notification.getUser().getUserId().equals(userId)) {
            throw new UserServiceException("Not your notification");
        }
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void markAllRead() {
        notificationRepository.markAllReadByUserId(currentUserId());
    }

    private UUID currentUserId() {
        return UUID.fromString(
                (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal()
        );
    }
}
