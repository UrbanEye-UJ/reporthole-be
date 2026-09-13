package za.co.urbaneye.reporthole.notification.service.interfaces;

import za.co.urbaneye.reporthole.notification.dto.NotificationResponse;
import za.co.urbaneye.reporthole.user.entity.User;

import java.util.List;
import java.util.UUID;

/**
 * In-app notification service.
 *
 * <p>Persists a {@link za.co.urbaneye.reporthole.notification.entity.Notification} record for each
 * relevant status transition and immediately pushes a {@code notification} SSE event to the
 * recipient if they are currently connected. Recipients who are offline pick up their notifications
 * on next page load via {@link #getMyNotifications()}.</p>
 *
 * @author Refentse
 * @since 1.0
 */
public interface INotificationService {

    /**
     * Persists a notification for the given recipient and pushes an SSE event if they are connected.
     *
     * @param recipient the user who should receive the notification
     * @param message   the human-readable message to display in the notification drawer
     */
    void notify(User recipient, String message);

    /**
     * Returns the calling user's 50 most recent notifications, newest first.
     */
    List<NotificationResponse> getMyNotifications();

    /**
     * Returns the number of unread notifications for the calling user.
     */
    long getUnreadCount();

    /**
     * Marks a single notification as read. Validates that it belongs to the calling user.
     *
     * @param notificationId the notification to mark read
     */
    void markRead(UUID notificationId);

    /**
     * Marks all of the calling user's unread notifications as read in one operation.
     */
    void markAllRead();
}
