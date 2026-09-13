package za.co.urbaneye.reporthole.message.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.urbaneye.reporthole.message.entity.Message;
import za.co.urbaneye.reporthole.message.entity.MessageCategory;

import java.util.List;
import java.util.UUID;

/**
 * Repository for {@link Message} persistence.
 *
 * @author Refentse
 * @since 1.0
 */
public interface MessageRepository extends JpaRepository<Message, UUID> {

    /**
     * Returns all messages of a given category, newest first.
     *
     * @param category the category to filter by
     * @return messages in descending creation order
     */
    List<Message> findByCategoryOrderByCreatedAtDesc(MessageCategory category);
}
