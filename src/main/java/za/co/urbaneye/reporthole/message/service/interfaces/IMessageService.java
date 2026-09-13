package za.co.urbaneye.reporthole.message.service.interfaces;

import za.co.urbaneye.reporthole.message.dto.ContactMessageRequest;
import za.co.urbaneye.reporthole.message.dto.MessageResponse;
import za.co.urbaneye.reporthole.message.dto.SendMessageRequest;

import java.util.List;
import java.util.UUID;

/**
 * Service contract for the messaging feature.
 *
 * @author Refentse
 * @since 1.0
 */
public interface IMessageService {

    /**
     * Stores a message submitted via the public landing-page contact form.
     * No authentication required — caller identity comes from the request body.
     *
     * @param request the contact form payload
     */
    void submitContact(ContactMessageRequest request);

    /**
     * Stores a message sent by an authenticated user.
     *
     * @param request    the message payload
     * @param senderUserId UUID of the authenticated sender
     */
    void sendMessage(SendMessageRequest request, UUID senderUserId);

    /**
     * Returns all {@code CIVILIAN_COMPLAINT} messages, newest first.
     * Intended for the ADMIN inbox.
     *
     * @return list of civilian complaint messages
     */
    List<MessageResponse> getCivilianComplaints();

    /**
     * Returns all {@code CONTACT_US} messages, newest first.
     * Intended for the SECURITY_ADMIN inbox.
     *
     * @return list of contact-form messages
     */
    List<MessageResponse> getContactMessages();
}
