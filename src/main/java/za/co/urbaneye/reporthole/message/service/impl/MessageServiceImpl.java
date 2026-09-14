package za.co.urbaneye.reporthole.message.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.urbaneye.reporthole.admin.security.service.interfaces.IAuditLogService;
import za.co.urbaneye.reporthole.message.dto.ContactMessageRequest;
import za.co.urbaneye.reporthole.message.dto.MessageResponse;
import za.co.urbaneye.reporthole.message.dto.SendMessageRequest;
import za.co.urbaneye.reporthole.message.entity.Message;
import za.co.urbaneye.reporthole.message.entity.MessageCategory;
import za.co.urbaneye.reporthole.message.repository.MessageRepository;
import za.co.urbaneye.reporthole.message.service.interfaces.IMessageService;
import za.co.urbaneye.reporthole.user.entity.User;
import za.co.urbaneye.reporthole.user.entity.UserAuth;
import za.co.urbaneye.reporthole.user.repository.IUserAuthRepository;
import za.co.urbaneye.reporthole.user.repository.IUserRepository;

import java.util.List;
import java.util.UUID;

/**
 * Default {@link IMessageService}.
 *
 * <p>For authenticated senders the sender's name and email are resolved from the {@code User}
 * and {@code UserAuth} tables (which store them AES-encrypted) so we never read PII from the
 * request body for authenticated callers — only the message content is accepted.</p>
 *
 * @author Refentse
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MessageServiceImpl implements IMessageService {

    private final MessageRepository messageRepository;
    private final IUserRepository userRepository;
    private final IUserAuthRepository userAuthRepository;
    private final IAuditLogService auditLogService;

    @Override
    @Transactional
    public void submitContact(ContactMessageRequest request) {
        Message saved = messageRepository.save(Message.builder()
                .senderName(request.name())
                .senderEmail(request.email())
                .subject(request.subject())
                .content(request.content())
                .category(MessageCategory.CONTACT_US)
                .build());
        auditLogService.record(null, "CONTACT_FORM_SUBMITTED", "MESSAGE", saved.getId(),
                "Public contact-form submission from " + request.email());
        log.info("Contact message stored from {}", request.email());
    }

    @Override
    @Transactional
    public void sendMessage(SendMessageRequest request, UUID senderUserId) {
        User user = userRepository.findById(senderUserId)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
        UserAuth auth = userAuthRepository.findById(senderUserId)
                .orElseThrow(() -> new IllegalStateException("Authenticated user auth not found"));

        Message saved = messageRepository.save(Message.builder()
                .senderUserId(senderUserId)
                .senderName(user.getFirstName() + " " + user.getLastName())
                .senderEmail(auth.getEmail())
                .senderRole(user.getRole())
                .subject(request.subject())
                .content(request.content())
                .category(MessageCategory.USER_MESSAGE)
                .build());
        auditLogService.record(user, "MESSAGE_SENT", "MESSAGE", saved.getId(),
                "Sent a message to the admin team (" + user.getRole() + ")");
        log.info("Message stored from user {}", senderUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MessageResponse> getUserMessages() {
        return messageRepository
                .findByCategoryOrderByCreatedAtDesc(MessageCategory.USER_MESSAGE)
                .stream()
                .map(MessageResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MessageResponse> getContactMessages() {
        return messageRepository
                .findByCategoryOrderByCreatedAtDesc(MessageCategory.CONTACT_US)
                .stream()
                .map(MessageResponse::from)
                .toList();
    }
}
