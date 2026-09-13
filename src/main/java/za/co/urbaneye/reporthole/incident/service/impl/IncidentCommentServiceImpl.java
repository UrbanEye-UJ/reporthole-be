package za.co.urbaneye.reporthole.incident.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import za.co.urbaneye.reporthole.incident.dto.IncidentCommentResponse;
import za.co.urbaneye.reporthole.incident.entity.IncidentComment;
import za.co.urbaneye.reporthole.incident.repository.IncidentCommentRepository;
import za.co.urbaneye.reporthole.incident.repository.IncidentRepository;
import za.co.urbaneye.reporthole.incident.service.interfaces.IIncidentCommentService;
import za.co.urbaneye.reporthole.user.entity.User;
import za.co.urbaneye.reporthole.user.repository.IUserRepository;

import java.util.List;
import java.util.UUID;

/**
 * Handles reading and posting of incident comments.
 * Open to all authenticated roles — no role restriction applied here.
 */
@Service
@RequiredArgsConstructor
public class IncidentCommentServiceImpl implements IIncidentCommentService {

    private final IncidentCommentRepository commentRepository;
    private final IncidentRepository incidentRepository;
    private final IUserRepository userRepository;

    @Override
    public List<IncidentCommentResponse> getComments(UUID incidentId) {
        return commentRepository
                .findByIncident_IncidentIdOrderByCreatedAtAsc(incidentId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public IncidentCommentResponse addComment(UUID incidentId, String content) {
        UUID userId = UUID.fromString(
                (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal());

        var incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Incident not found"));

        User author = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        IncidentComment comment = IncidentComment.builder()
                .incident(incident)
                .author(author)
                .content(content)
                .build();

        return toResponse(commentRepository.save(comment));
    }

    private IncidentCommentResponse toResponse(IncidentComment comment) {
        User author = comment.getAuthor();
        String name = author.getFirstName() + " " + author.getLastName();
        return new IncidentCommentResponse(
                comment.getId(),
                comment.getContent(),
                comment.getCreatedAt(),
                name,
                author.getRole().name()
        );
    }
}
