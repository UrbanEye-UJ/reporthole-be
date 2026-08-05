package za.co.urbaneye.reporthole.incident.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import za.co.urbaneye.reporthole.incident.dto.IncidentRequestDTO;
import za.co.urbaneye.reporthole.incident.dto.IncidentResponseDTO;
import za.co.urbaneye.reporthole.incident.dto.IncidentStatsDTO;
import za.co.urbaneye.reporthole.incident.dto.ResolveIncidentRequest;
import za.co.urbaneye.reporthole.incident.entity.Assignment;
import za.co.urbaneye.reporthole.incident.entity.AssignmentStatus;
import za.co.urbaneye.reporthole.incident.entity.AssignmentWorkflow;
import za.co.urbaneye.reporthole.incident.entity.Incident;
import za.co.urbaneye.reporthole.incident.entity.IncidentReporter;
import za.co.urbaneye.reporthole.incident.entity.IssueType;
import za.co.urbaneye.reporthole.incident.exception.AssignmentException;
import za.co.urbaneye.reporthole.incident.repository.AssignmentRepository;
import za.co.urbaneye.reporthole.incident.repository.AssignmentWorkflowRepository;
import za.co.urbaneye.reporthole.incident.repository.IncidentRepository;
import za.co.urbaneye.reporthole.incident.repository.IncidentReporterRepository;
import za.co.urbaneye.reporthole.incident.service.interfaces.ImageStorageService;
import za.co.urbaneye.reporthole.incident.service.interfaces.IncidentService;
import za.co.urbaneye.reporthole.user.entity.User;
import za.co.urbaneye.reporthole.user.entity.UserRole;
import za.co.urbaneye.reporthole.user.exception.UserServiceException;
import za.co.urbaneye.reporthole.user.repository.IUserAuthRepository;
import za.co.urbaneye.reporthole.user.repository.IUserRepository;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IncidentServiceImpl implements IncidentService {

    private final IncidentRepository incidentRepository;
    private final IncidentReporterRepository incidentReporterRepository;
    private final AssignmentWorkflowRepository assignmentWorkflowRepository;
    private final AssignmentRepository assignmentRepository;
    private final IUserRepository userRepository;
    private final ImageStorageService imageStorageService;
    private final IncidentSseService incidentSseService;

    private static final double MANUAL_DUPLICATE_RADIUS_METRES = 1_000.0;
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    @Override
    public IncidentResponseDTO createIncident(IncidentRequestDTO request) {
        final UUID userId = currentUserId();
        //TODO Read User from cache this is not viable to check user everytim on db
        final User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserServiceException("User not found"));
        final Point point = geometryFactory.createPoint(
                new Coordinate(request.longitude(), request.latitude())
        );

        Optional<Incident> nearestDuplicate = request.forceCreate()
                ? Optional.empty()
                : incidentRepository.findNearestDuplicate(
                        request.latitude(), request.longitude(),
                        MANUAL_DUPLICATE_RADIUS_METRES, request.incidentType());

        if (nearestDuplicate.isPresent()) {
            Incident existing = nearestDuplicate.get();
            boolean alreadyConfirmed = incidentReporterRepository
                    .existsByIncident_IncidentIdAndUser_UserId(existing.getIncidentId(), userId);
            return IncidentResponseDTO.builder()
                    .incidentId(existing.getIncidentId())
                    .incidentType(existing.getIncidentType())
                    .description(existing.getDescription())
                    .source(existing.getSource())
                    .incidentDate(existing.getIncidentDate())
                    .latitude(existing.getLocation().getY())
                    .longitude(existing.getLocation().getX())
                    .imageUrl(existing.getImageUrl())
                    .locationAddress(existing.getLocationAddress())
                    .userId(existing.getUser().getUserId())
                    .reportCount(existing.getReportCount())
                    .duplicate(true)
                    .alreadyConfirmed(alreadyConfirmed)
                    .existingIncidentId(existing.getIncidentId())
                    .status(resolveStatus(existing.getIncidentId()))
                    .build();
        }

        final String imageUrl = imageStorageService.saveBase64Image(request.imageBase64());
        Incident incident = new Incident();
        incident.setIncidentType(request.incidentType());
        incident.setDescription(request.description());
        incident.setSource(request.source());
        incident.setIncidentDate(LocalDateTime.now());
        incident.setLocation(point);
        incident.setImageUrl(imageUrl);
        incident.setLocationAddress(request.locationAddress());
        incident.setUser(user);
        final Incident saved = incidentRepository.save(incident);
        incidentReporterRepository.save(new IncidentReporter(saved, user));
        return IncidentResponseDTO.builder()
                .incidentId(saved.getIncidentId())
                .incidentType(saved.getIncidentType())
                .description(saved.getDescription())
                .source(saved.getSource())
                .incidentDate(saved.getIncidentDate())
                .latitude(point.getY())
                .longitude(point.getX())
                .imageUrl(saved.getImageUrl())
                .locationAddress(saved.getLocationAddress())
                .userId(user.getUserId())
                .reportCount(saved.getReportCount())
                .reporterCount(1)
                .duplicate(false)
                .status(AssignmentStatus.REPORTED)
                .build();
    }

    @Override
    @Transactional
    public IncidentResponseDTO confirmDuplicate(UUID incidentId) {
        final UUID userId = currentUserId();
        final User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserServiceException("User not found"));
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new RuntimeException("Incident not found: " + incidentId));
        boolean alreadyLinked = incidentReporterRepository.existsByIncident_IncidentIdAndUser_UserId(incidentId, userId);
        if (!alreadyLinked) {
            incidentRepository.incrementReportCount(incidentId);
            incidentReporterRepository.save(new IncidentReporter(incident, user));
        }
        int reporterCount = incidentReporterRepository.countByIncident_IncidentId(incidentId);
        Set<UUID> recipients = new HashSet<>(incidentReporterRepository.findUserIdsByIncidentId(incidentId));
        incidentSseService.pushIncidentUpdate(incidentId, recipients);
        return IncidentResponseDTO.builder()
                .incidentId(incident.getIncidentId())
                .incidentType(incident.getIncidentType())
                .description(incident.getDescription())
                .source(incident.getSource())
                .incidentDate(incident.getIncidentDate())
                .latitude(incident.getLocation().getY())
                .longitude(incident.getLocation().getX())
                .imageUrl(incident.getImageUrl())
                .locationAddress(incident.getLocationAddress())
                .userId(incident.getUser().getUserId())
                .reportCount(incident.getReportCount() + 1)
                .reporterCount(reporterCount)
                .duplicate(true)
                .existingIncidentId(incident.getIncidentId())
                .status(resolveStatus(incidentId))
                .build();
    }

    @Override
    public List<IncidentResponseDTO> getMyIncidents() {
        final UUID userId = currentUserId();
        return incidentRepository.findAllReportedByUser(userId).stream()
                .map(incident -> toResponseDTO(incident, userId))
                .toList();
    }

    @Override
    public List<IncidentResponseDTO> getRecentIncidents(int limit) {
        return incidentRepository.findByDeletedFalseOrderByIncidentDateDesc(PageRequest.of(0, limit)).stream()
                .map(incident -> toResponseDTO(incident, incident.getUser().getUserId()))
                .toList();
    }

    @Override
    public IncidentStatsDTO getIncidentStats() {
        return new IncidentStatsDTO(
                incidentRepository.countByDeletedFalse(),
                assignmentWorkflowRepository.countResolvedIncidents()
        );
    }

    @Override
    public List<IncidentResponseDTO> searchMyIncidents(String keyword, IssueType issueType) {
        final UUID userId = currentUserId();
        // Blank keyword treated as no keyword filter
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        return incidentRepository.searchByUser(userId, kw, issueType).stream()
                .map(incident -> toResponseDTO(incident, userId))
                .toList();
    }

    @Override
    public IncidentResponseDTO getIncidentById(UUID incidentId) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new RuntimeException("Incident not found: " + incidentId));
        int reporterCount = incidentReporterRepository.countByIncident_IncidentId(incidentId);
        return IncidentResponseDTO.builder()
                .incidentId(incident.getIncidentId())
                .incidentType(incident.getIncidentType())
                .description(incident.getDescription())
                .source(incident.getSource())
                .incidentDate(incident.getIncidentDate())
                .latitude(incident.getLocation().getY())
                .longitude(incident.getLocation().getX())
                .imageUrl(incident.getImageUrl())
                .locationAddress(incident.getLocationAddress())
                .userId(incident.getUser().getUserId())
                .reportCount(incident.getReportCount())
                .reporterCount(reporterCount)
                .duplicate(false)
                .status(resolveStatus(incidentId))
                .build();
    }

    @Override
    @Transactional
    public IncidentResponseDTO assignIncident(UUID incidentId, UUID contractorId) {
        requireAdmin();

        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new AssignmentException("Incident not found: " + incidentId));

        User contractor = userRepository.findById(contractorId)
                .orElseThrow(() -> new AssignmentException("Contractor not found: " + contractorId));
        if (contractor.getRole() != UserRole.CONTRACTOR) {
            throw new AssignmentException("Selected user is not a contractor");
        }

        assignmentRepository.save(
                Assignment.builder()
                        .status(AssignmentStatus.ASSIGNED)
                        .incident(incident)
                        .contractor(contractor)
                        .build()
        );

        AssignmentWorkflow workflow = new AssignmentWorkflow();
        workflow.setIncident(incident);
        workflow.setStatus(AssignmentStatus.ASSIGNED);
        workflow.setNotes("Assigned to " + contractor.getFirstName() + " " + contractor.getLastName());
        assignmentWorkflowRepository.save(workflow);

        return toResponseDTO(incident, incident.getUser().getUserId());
    }

    private User requireAdmin() {
        User currentUser = userRepository.findById(currentUserId())
                .orElseThrow(() -> new AssignmentException("User not found"));
        if (currentUser.getRole() != UserRole.ADMIN) {
            throw new AssignmentException("Only admins can perform this action");
        }
        return currentUser;
    }

    @Override
    @Transactional
    public IncidentResponseDTO verifyIncident(UUID incidentId) {
        User admin = requireAdmin();

        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new AssignmentException("Incident not found: " + incidentId));

        if (resolveStatus(incidentId) != AssignmentStatus.REPORTED) {
            throw new AssignmentException("Only reported incidents can be verified");
        }

        AssignmentWorkflow workflow = new AssignmentWorkflow();
        workflow.setIncident(incident);
        workflow.setUpdatedBy(admin);
        workflow.setStatus(AssignmentStatus.VERIFIED);
        workflow.setNotes("Verified by " + admin.getFirstName() + " " + admin.getLastName());
        assignmentWorkflowRepository.save(workflow);

        Set<UUID> recipients = new HashSet<>(incidentReporterRepository.findUserIdsByIncidentId(incidentId));
        incidentSseService.pushIncidentUpdate(incidentId, recipients);

        return toResponseDTO(incident, incident.getUser().getUserId());
    }

    @Override
    public List<IncidentResponseDTO> getMyAssignments() {
        final UUID contractorId = currentUserId();
        return assignmentRepository.findByContractor_UserId(contractorId).stream()
                .map(assignment -> {
                    Incident incident = assignment.getIncident();
                    return toResponseDTO(incident, incident.getUser().getUserId());
                })
                .toList();
    }

    @Override
    @Transactional
    public IncidentResponseDTO acceptAssignment(UUID incidentId) {
        final UUID contractorId = currentUserId();
        Assignment assignment = assignmentRepository.findByIncident_IncidentIdAndContractor_UserId(incidentId, contractorId)
                .orElseThrow(() -> new AssignmentException("Assignment not found for this contractor and incident"));

        if (assignment.getStatus() != AssignmentStatus.ASSIGNED) {
            throw new AssignmentException("Only pending assignments can be accepted");
        }

        User contractor = assignment.getContractor();
        assignment.setStatus(AssignmentStatus.IN_PROGRESS);
        assignmentRepository.save(assignment);

        Incident incident = assignment.getIncident();
        AssignmentWorkflow workflow = new AssignmentWorkflow();
        workflow.setIncident(incident);
        workflow.setUpdatedBy(contractor);
        workflow.setStatus(AssignmentStatus.IN_PROGRESS);
        workflow.setNotes("Accepted by " + contractor.getFirstName() + " " + contractor.getLastName());
        assignmentWorkflowRepository.save(workflow);

        Set<UUID> recipients = new HashSet<>(incidentReporterRepository.findUserIdsByIncidentId(incidentId));
        incidentSseService.pushIncidentUpdate(incidentId, recipients);

        return toResponseDTO(incident, incident.getUser().getUserId());
    }

    @Override
    @Transactional
    public IncidentResponseDTO rejectAssignment(UUID incidentId) {
        final UUID contractorId = currentUserId();
        Assignment assignment = assignmentRepository.findByIncident_IncidentIdAndContractor_UserId(incidentId, contractorId)
                .orElseThrow(() -> new AssignmentException("Assignment not found for this contractor and incident"));

        if (assignment.getStatus() != AssignmentStatus.ASSIGNED) {
            throw new AssignmentException("Only pending assignments can be rejected");
        }

        User contractor = assignment.getContractor();
        Incident incident = assignment.getIncident();
        assignmentRepository.delete(assignment);

        AssignmentWorkflow workflow = new AssignmentWorkflow();
        workflow.setIncident(incident);
        workflow.setUpdatedBy(contractor);
        workflow.setStatus(AssignmentStatus.VERIFIED);
        workflow.setNotes("Rejected by " + contractor.getFirstName() + " " + contractor.getLastName() + " — needs reassignment");
        assignmentWorkflowRepository.save(workflow);

        Set<UUID> recipients = new HashSet<>(incidentReporterRepository.findUserIdsByIncidentId(incidentId));
        incidentSseService.pushIncidentUpdate(incidentId, recipients);

        return toResponseDTO(incident, incident.getUser().getUserId());
    }

    @Override
    @Transactional
    public IncidentResponseDTO resolveIncident(UUID incidentId, ResolveIncidentRequest request) {
        final UUID contractorId = currentUserId();
        Assignment assignment = assignmentRepository.findByIncident_IncidentIdAndContractor_UserId(incidentId, contractorId)
                .orElseThrow(() -> new AssignmentException("Assignment not found for this contractor and incident"));

        final String photoUrl = imageStorageService.saveBase64Image(request.photoBase64());

        assignment.setStatus(AssignmentStatus.RESOLVED);
        assignment.setCompletionDate(LocalDateTime.now());
        assignment.setResolutionImageUrl(photoUrl);
        assignment.setResolutionNotes(request.note());
        assignmentRepository.save(assignment);

        Incident incident = assignment.getIncident();
        AssignmentWorkflow workflow = new AssignmentWorkflow();
        workflow.setIncident(incident);
        workflow.setStatus(AssignmentStatus.RESOLVED);
        workflow.setNotes(request.note());
        assignmentWorkflowRepository.save(workflow);

        Set<UUID> recipients = new HashSet<>(incidentReporterRepository.findUserIdsByIncidentId(incidentId));
        incidentSseService.pushIncidentUpdate(incidentId, recipients);

        return toResponseDTO(incident, incident.getUser().getUserId());
    }

    @Override
    @Transactional
    public void deleteIncident(UUID incidentId) {
        final UUID userId = currentUserId();
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new RuntimeException("Incident not found: " + incidentId));
        if (!incident.getUser().getUserId().equals(userId)) {
            throw new UserServiceException("You can only delete your own incidents");
        }
        incident.setDeleted(true);
        incidentRepository.save(incident);
    }

    private IncidentResponseDTO toResponseDTO(Incident incident, UUID userId) {
        return IncidentResponseDTO.builder()
                .incidentId(incident.getIncidentId())
                .incidentType(incident.getIncidentType())
                .description(incident.getDescription())
                .source(incident.getSource())
                .incidentDate(incident.getIncidentDate())
                .latitude(incident.getLocation().getY())
                .longitude(incident.getLocation().getX())
                .imageUrl(incident.getImageUrl())
                .locationAddress(incident.getLocationAddress())
                .userId(userId)
                .reportCount(incident.getReportCount())
                .reporterCount(incidentReporterRepository.countByIncident_IncidentId(incident.getIncidentId()))
                .duplicate(false)
                .status(resolveStatus(incident.getIncidentId()))
                .build();
    }

    /** Current status is the most recent workflow entry; incidents with no entries yet are still just REPORTED. */
    private AssignmentStatus resolveStatus(UUID incidentId) {
        return assignmentWorkflowRepository.findFirstByIncident_IncidentIdOrderByUpdatedDateDesc(incidentId)
                .map(AssignmentWorkflow::getStatus)
                .orElse(AssignmentStatus.REPORTED);
    }

    private UUID currentUserId() {
        return UUID.fromString(
                (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal()
        );
    }
}
