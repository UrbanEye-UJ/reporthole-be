package za.co.urbaneye.reporthole.message.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.co.urbaneye.reporthole.message.dto.ContactMessageRequest;
import za.co.urbaneye.reporthole.message.dto.MessageResponse;
import za.co.urbaneye.reporthole.message.dto.SendMessageRequest;
import za.co.urbaneye.reporthole.message.entity.Message;
import za.co.urbaneye.reporthole.message.entity.MessageCategory;
import za.co.urbaneye.reporthole.message.repository.MessageRepository;
import za.co.urbaneye.reporthole.message.service.impl.MessageServiceImpl;
import za.co.urbaneye.reporthole.user.entity.User;
import za.co.urbaneye.reporthole.user.entity.UserAuth;
import za.co.urbaneye.reporthole.user.repository.IUserAuthRepository;
import za.co.urbaneye.reporthole.user.repository.IUserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link MessageServiceImpl}: all four service methods.
 */
@ExtendWith(MockitoExtension.class)
class MessageServiceImplTest {

    @Mock private MessageRepository messageRepository;
    @Mock private IUserRepository userRepository;
    @Mock private IUserAuthRepository userAuthRepository;

    @InjectMocks
    private MessageServiceImpl service;

    // ── submitContact ─────────────────────────────────────────────────────────

    @Test
    void submitContact_savesContactUsMessage() {
        ContactMessageRequest req = new ContactMessageRequest(
                "John Smith", "john@example.com", "Pothole on Main Rd", "There is a big pothole.");

        when(messageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.submitContact(req);

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(captor.capture());
        Message saved = captor.getValue();

        assertThat(saved.getSenderName()).isEqualTo("John Smith");
        assertThat(saved.getSenderEmail()).isEqualTo("john@example.com");
        assertThat(saved.getSubject()).isEqualTo("Pothole on Main Rd");
        assertThat(saved.getContent()).isEqualTo("There is a big pothole.");
        assertThat(saved.getCategory()).isEqualTo(MessageCategory.CONTACT_US);
    }

    // ── sendMessage ───────────────────────────────────────────────────────────

    @Test
    void sendMessage_resolvesUserDetails_savesCivilianComplaint() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().userId(userId).firstName("Jane").lastName("Doe").build();
        UserAuth auth = UserAuth.builder().authId(userId).email("jane@doe.com").build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userAuthRepository.findById(userId)).thenReturn(Optional.of(auth));
        when(messageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SendMessageRequest req = new SendMessageRequest("Feedback", "This road is terrible.");
        service.sendMessage(req, userId);

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(captor.capture());
        Message saved = captor.getValue();

        assertThat(saved.getSenderUserId()).isEqualTo(userId);
        assertThat(saved.getSenderName()).isEqualTo("Jane Doe");
        assertThat(saved.getSenderEmail()).isEqualTo("jane@doe.com");
        assertThat(saved.getSubject()).isEqualTo("Feedback");
        assertThat(saved.getContent()).isEqualTo("This road is terrible.");
        assertThat(saved.getCategory()).isEqualTo(MessageCategory.CIVILIAN_COMPLAINT);
    }

    @Test
    void sendMessage_userNotFound_throwsIllegalState() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.sendMessage(
                new SendMessageRequest(null, "content"), userId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Authenticated user not found");
    }

    @Test
    void sendMessage_authNotFound_throwsIllegalState() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().userId(userId).firstName("X").lastName("Y").build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userAuthRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.sendMessage(
                new SendMessageRequest(null, "content"), userId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Authenticated user auth not found");
    }

    // ── getCivilianComplaints ─────────────────────────────────────────────────

    @Test
    void getCivilianComplaints_delegatesToRepoAndMapsToDto() {
        UUID id = UUID.randomUUID();
        Message msg = Message.builder()
                .id(id).senderName("Alice K.").senderEmail("a***@example.com")
                .subject("Road issue").content("Pothole on Main St.")
                .category(MessageCategory.CIVILIAN_COMPLAINT)
                .createdAt(LocalDateTime.now())
                .build();

        when(messageRepository.findByCategoryOrderByCreatedAtDesc(MessageCategory.CIVILIAN_COMPLAINT))
                .thenReturn(List.of(msg));

        List<MessageResponse> result = service.getCivilianComplaints();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(id);
        assertThat(result.get(0).category()).isEqualTo(MessageCategory.CIVILIAN_COMPLAINT);
    }

    // ── getContactMessages ────────────────────────────────────────────────────

    @Test
    void getContactMessages_delegatesToRepoAndMapsToDto() {
        UUID id = UUID.randomUUID();
        Message msg = Message.builder()
                .id(id).senderName("John Smith").senderEmail("john@example.com")
                .subject("Hello").content("Interested in the platform.")
                .category(MessageCategory.CONTACT_US)
                .createdAt(LocalDateTime.now())
                .build();

        when(messageRepository.findByCategoryOrderByCreatedAtDesc(MessageCategory.CONTACT_US))
                .thenReturn(List.of(msg));

        List<MessageResponse> result = service.getContactMessages();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(id);
        assertThat(result.get(0).category()).isEqualTo(MessageCategory.CONTACT_US);
    }

    @Test
    void getContactMessages_noMessages_returnsEmpty() {
        when(messageRepository.findByCategoryOrderByCreatedAtDesc(MessageCategory.CONTACT_US))
                .thenReturn(List.of());

        assertThat(service.getContactMessages()).isEmpty();
    }
}
