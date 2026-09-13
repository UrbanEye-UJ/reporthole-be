package za.co.urbaneye.reporthole.message.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    @Override
    @Transactional
    public void submitContact(ContactMessageRequest request) {
        messageRepository.save(Message.builder()
                .senderName(request.name())
                .senderEmail(request.email())
                .subject(request.subject())
                .content(request.content())
                .category(MessageCategory.CONTACT_US)
                .build());
        log.info("Contact message stored from {}", request.email());
    }

    @Override
    @Transactional
    public void sendMessage(SendMessageRequest request, UUID senderUserId) {
        User user = userRepository.findById(senderUserId)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
        UserAuth auth = userAuthRepository.findById(senderUserId)
                .orElseThrow(() -> new IllegalStateException("Authenticated user auth not found"));

        messageRepository.save(Message.builder()
                .senderUserId(senderUserId)
                .senderName(user.getFirstName() + " " + user.getLastName())
                .senderEmail(auth.getEmail())
                .subject(request.subject())
                .content(request.content())
                .category(MessageCategory.CIVILIAN_COMPLAINT)
                .build());
        log.info("Message stored from user {}", senderUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MessageResponse> getCivilianComplaints() {
        return messageRepository
                .findByCategoryOrderByCreatedAtDesc(MessageCategory.CIVILIAN_COMPLAINT)
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
