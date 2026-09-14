package za.co.urbaneye.reporthole.incident.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import za.co.urbaneye.reporthole.admin.security.service.interfaces.IAuditLogService;
import za.co.urbaneye.reporthole.incident.dto.IncidentCommentResponse;
import za.co.urbaneye.reporthole.incident.entity.IncidentComment;
import za.co.urbaneye.reporthole.incident.repository.IncidentCommentRepository;
import za.co.urbaneye.reporthole.incident.repository.IncidentReporterRepository;
import za.co.urbaneye.reporthole.incident.repository.IncidentRepository;
import za.co.urbaneye.reporthole.incident.service.interfaces.IIncidentCommentService;
import za.co.urbaneye.reporthole.security.SecretUtil;
import za.co.urbaneye.reporthole.user.entity.User;
import za.co.urbaneye.reporthole.user.entity.UserRole;
import za.co.urbaneye.reporthole.user.repository.IUserRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
    private final IAuditLogService auditLogService;
    private final IncidentReporterRepository incidentReporterRepository;
    private final IncidentSseService incidentSseService;

    @Override
    public List<IncidentCommentResponse> getComments(UUID incidentId) {
        return commentRepository
                .findByIncident_IncidentIdOrderByCreatedAtAsc(incidentId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
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

        IncidentComment saved = commentRepository.save(comment);
        auditLogService.record(author, "COMMENT_POSTED", "INCIDENT", incidentId,
                "Posted a comment on an incident");

        Set<UUID> recipients = new HashSet<>(incidentReporterRepository.findUserIdsByIncidentId(incidentId));
        incidentSseService.pushIncidentUpdate(incidentId, recipients);

        return toResponse(saved);
    }

    /**
     * Comments are readable by any authenticated user ({@link #getComments}), so a civilian
     * author's name is masked to their first name and last initial — staff (admin/contractor)
     * names are shown in full since they're acting in a professional capacity, not as PII.
     */
    private IncidentCommentResponse toResponse(IncidentComment comment) {
        User author = comment.getAuthor();
        String name = author.getRole() == UserRole.CIVILIAN
                ? SecretUtil.maskName(author.getFirstName(), author.getLastName())
                : author.getFirstName() + " " + author.getLastName();
        return new IncidentCommentResponse(
                comment.getId(),
                comment.getContent(),
                comment.getCreatedAt(),
                name,
                author.getRole().name()
        );
    }
}
