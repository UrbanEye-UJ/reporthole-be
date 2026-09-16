package za.co.urbaneye.reporthole.incident.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import za.co.urbaneye.reporthole.admin.municipality.entity.Municipality;
import za.co.urbaneye.reporthole.admin.municipality.repository.IMunicipalityRepository;
import za.co.urbaneye.reporthole.incident.config.IncidentProperties;
import za.co.urbaneye.reporthole.incident.dto.IncidentAnalyticsDTO;
import za.co.urbaneye.reporthole.incident.dto.IncidentPageResponse;
import za.co.urbaneye.reporthole.incident.dto.IncidentRequestDTO;
import za.co.urbaneye.reporthole.incident.dto.IncidentResponseDTO;
import za.co.urbaneye.reporthole.incident.dto.IncidentStatsDTO;
import za.co.urbaneye.reporthole.incident.dto.RejectAssignmentRequest;
import za.co.urbaneye.reporthole.incident.dto.ResolveIncidentRequest;
import za.co.urbaneye.reporthole.incident.dto.WorkflowEntryDTO;
import za.co.urbaneye.reporthole.incident.entity.AiReviewDecision;
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
import za.co.urbaneye.reporthole.notification.service.interfaces.IMailService;
import za.co.urbaneye.reporthole.notification.service.interfaces.INotificationService;
import za.co.urbaneye.reporthole.user.entity.User;
import za.co.urbaneye.reporthole.user.entity.UserAuth;
import za.co.urbaneye.reporthole.user.entity.UserRole;
import za.co.urbaneye.reporthole.user.exception.UserServiceException;
import za.co.urbaneye.reporthole.user.repository.IUserAuthRepository;
import za.co.urbaneye.reporthole.user.repository.IUserRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class IncidentServiceImpl implements IncidentService {

    private final IncidentRepository incidentRepository;
    private final IncidentReporterRepository incidentReporterRepository;
    private final AssignmentWorkflowRepository assignmentWorkflowRepository;
    private final AssignmentRepository assignmentRepository;
    private final IUserRepository userRepository;
    private final IUserAuthRepository userAuthRepository;
    private final ImageStorageService imageStorageService;
    private final IncidentSseService incidentSseService;
    private final IMailService mailService;
    private final INotificationService notificationService;
    private final IncidentProperties incidentProperties;
    private final IMunicipalityRepository municipalityRepository;

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
                    .aiGenerated(existing.isAiGenerated())
                    .aiConfidence(existing.getAiConfidence())
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
        if (request.confidence() != null) {
            incident.setAiGenerated(true);
            incident.setAiConfidence(request.confidence());
        }
        try {
            municipalityRepository.findContainingPoint(request.latitude(), request.longitude())
                    .ifPresent(incident::setMunicipality);
        } catch (DataAccessException ex) {
            // Spatial containment isn't available in every environment (e.g. H2 in tests, which
            // has no PostGIS functions) — fall back to unassigned, same as "no containing
            // municipality found". Verification still re-tags and spatially re-validates later.
            log.debug("Municipality auto-assignment skipped: {}", ex.getMessage());
        }
        final Incident saved = incidentRepository.save(incident);
        incidentReporterRepository.save(new IncidentReporter(saved, user));

        // Notify all admins that a new incident needs attention.
        String typeLabel = formatType(saved.getIncidentType());
        userRepository.findByRole(UserRole.ADMIN).forEach(admin ->
                notificationService.notify(admin, "New " + typeLabel + " report — awaiting verification"));

        AssignmentStatus status = AssignmentStatus.REPORTED;
        if (saved.isAiGenerated()) {
            status = applyAiReviewDecision(saved);
        }

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
                .status(status)
                .aiGenerated(saved.isAiGenerated())
                .aiConfidence(saved.getAiConfidence())
                .build();
    }

    /**
     * Applies the AI confidence threshold to a newly created, AI-generated incident:
     * confidence at or above {@link IncidentProperties#getAiApprovalThreshold()} is
     * auto-verified, skipping manual review; anything lower is left as {@code REPORTED}
     * so it surfaces in the normal admin verification queue.
     *
     * @return the resulting status, so the caller can return it in the response without
     *         an extra lookup
     */
    private AssignmentStatus applyAiReviewDecision(Incident incident) {
        AiReviewDecision decision = AiReviewDecision.from(
                incident.getAiConfidence(), incidentProperties.getAiApprovalThreshold());

        if (decision == AiReviewDecision.PENDING_REVIEW) {
            return AssignmentStatus.REPORTED;
        }

        AssignmentWorkflow workflow = new AssignmentWorkflow();
        workflow.setIncident(incident);
        workflow.setStatus(AssignmentStatus.VERIFIED);
        workflow.setNotes(String.format(
                "Auto-approved by AI (confidence %.0f%%, threshold %.0f%%)",
                incident.getAiConfidence() * 100, incidentProperties.getAiApprovalThreshold() * 100));
        assignmentWorkflowRepository.save(workflow);
        return AssignmentStatus.VERIFIED;
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
                .aiGenerated(incident.isAiGenerated())
                .aiConfidence(incident.getAiConfidence())
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
    public List<IncidentResponseDTO> getRecentIncidents(int limit, UUID municipalityId) {
        List<Incident> incidents;
        if (municipalityId != null) {
            incidents = incidentRepository.findByDeletedFalseAndMunicipality_IdOrderByIncidentDateDesc(
                    municipalityId, PageRequest.of(0, limit));
        } else {
            Municipality municipality = resolveAdminMunicipality();
            incidents = municipality != null
                    ? incidentRepository.findForAdmin(municipality, PageRequest.of(0, limit))
                    : incidentRepository.findByDeletedFalseOrderByIncidentDateDesc(PageRequest.of(0, limit));
        }
        return incidents.stream()
                .map(incident -> toResponseDTO(incident, incident.getUser().getUserId()))
                .toList();
    }

    @Override
    public IncidentStatsDTO getIncidentStats() {
        Municipality municipality = resolveAdminMunicipality();
        long total = municipality != null
                ? incidentRepository.countForAdmin(municipality)
                : incidentRepository.countByDeletedFalse();
        return new IncidentStatsDTO(total, assignmentWorkflowRepository.countResolvedIncidents());
    }

    @Override
    public IncidentAnalyticsDTO getIncidentAnalytics(UUID municipalityId) {
        // An ADMIN is always locked to their own municipality; the passed filter is only
        // honoured for callers with no municipality of their own (SECURITY_ADMIN).
        Municipality adminMunicipality = resolveAdminMunicipality();
        UUID effectiveMunicipalityId = adminMunicipality != null ? adminMunicipality.getId() : municipalityId;

        List<Incident> incidents = effectiveMunicipalityId != null
                ? incidentRepository.findByDeletedFalseAndMunicipality_Id(effectiveMunicipalityId)
                : incidentRepository.findByDeletedFalse();

        if (incidents.isEmpty()) {
            return new IncidentAnalyticsDTO(0, 0, 0, null, List.of(), List.of(), List.of(), List.of());
        }

        List<UUID> incidentIds = incidents.stream().map(Incident::getIncidentId).toList();

        // findStatusBreakdown only covers incidents that have at least one AssignmentWorkflow
        // row. A freshly-reported incident has none yet, but is still REPORTED (the same default
        // resolveStatus() falls back to) — fold that gap into the REPORTED bucket rather than
        // silently dropping those incidents from the funnel.
        Map<AssignmentStatus, Long> statusCounts = new EnumMap<>(AssignmentStatus.class);
        for (Object[] row : assignmentWorkflowRepository.findStatusBreakdown(incidentIds)) {
            statusCounts.put((AssignmentStatus) row[0], (Long) row[1]);
        }
        long incidentsWithWorkflow = statusCounts.values().stream().mapToLong(Long::longValue).sum();
        long incidentsWithoutWorkflow = incidents.size() - incidentsWithWorkflow;
        if (incidentsWithoutWorkflow > 0) {
            statusCounts.merge(AssignmentStatus.REPORTED, incidentsWithoutWorkflow, Long::sum);
        }
        List<IncidentAnalyticsDTO.StatusBreakdownEntry> statusBreakdown = Arrays.stream(AssignmentStatus.values())
                .map(status -> new IncidentAnalyticsDTO.StatusBreakdownEntry(status, statusCounts.getOrDefault(status, 0L)))
                .toList();

        long resolvedCount = statusBreakdown.stream()
                .filter(e -> e.status() == AssignmentStatus.RESOLVED)
                .mapToLong(IncidentAnalyticsDTO.StatusBreakdownEntry::count)
                .sum();

        List<IncidentAnalyticsDTO.TypeBreakdownEntry> typeBreakdown = incidents.stream()
                .collect(Collectors.groupingBy(Incident::getIncidentType, Collectors.counting()))
                .entrySet().stream()
                .map(e -> new IncidentAnalyticsDTO.TypeBreakdownEntry(e.getKey(), e.getValue()))
                .toList();

        YearMonth earliestTrendMonth = YearMonth.now().minusMonths(5);
        Map<YearMonth, Long> incidentsByMonth = incidents.stream()
                .map(i -> YearMonth.from(i.getIncidentDate()))
                .filter(month -> !month.isBefore(earliestTrendMonth))
                .collect(Collectors.groupingBy(m -> m, Collectors.counting()));
        List<IncidentAnalyticsDTO.MonthlyTrendEntry> monthlyTrend = monthRange(earliestTrendMonth).stream()
                .map(m -> new IncidentAnalyticsDTO.MonthlyTrendEntry(m.toString(), incidentsByMonth.getOrDefault(m, 0L)))
                .toList();

        List<Assignment> resolvedAssignments = assignmentRepository
                .findByIncident_IncidentIdInAndStatusAndCompletionDateIsNotNull(incidentIds, AssignmentStatus.RESOLVED);

        Double avgResolutionHours = resolvedAssignments.isEmpty() ? null : resolvedAssignments.stream()
                .mapToDouble(this::resolutionHours)
                .average()
                .orElse(0);

        Map<YearMonth, List<Assignment>> resolvedByMonth = resolvedAssignments.stream()
                .filter(a -> !YearMonth.from(a.getCompletionDate()).isBefore(earliestTrendMonth))
                .collect(Collectors.groupingBy(a -> YearMonth.from(a.getCompletionDate())));
        List<IncidentAnalyticsDTO.ResolutionTrendEntry> resolutionTimeTrend = monthRange(earliestTrendMonth).stream()
                .map(m -> {
                    List<Assignment> monthAssignments = resolvedByMonth.get(m);
                    Double avgHours = monthAssignments == null ? null
                            : monthAssignments.stream().mapToDouble(this::resolutionHours).average().orElse(0);
                    return new IncidentAnalyticsDTO.ResolutionTrendEntry(m.toString(), avgHours);
                })
                .toList();

        return new IncidentAnalyticsDTO(
                incidents.size(),
                resolvedCount,
                incidents.size() - resolvedCount,
                avgResolutionHours,
                statusBreakdown,
                typeBreakdown,
                monthlyTrend,
                resolutionTimeTrend
        );
    }

    private double resolutionHours(Assignment assignment) {
        return Duration.between(assignment.getAssignmentDate(), assignment.getCompletionDate()).toMinutes() / 60.0;
    }

    private List<YearMonth> monthRange(YearMonth start) {
        return Stream.iterate(start, m -> m.plusMonths(1)).limit(6).toList();
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
        return toResponseDTO(incident, incident.getUser().getUserId());
    }

    @Override
    @Transactional
    public IncidentResponseDTO assignIncident(UUID incidentId, UUID contractorId) {
        User admin = requireAdmin();

        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new AssignmentException("Incident not found: " + incidentId));

        User contractor = userRepository.findById(contractorId)
                .orElseThrow(() -> new AssignmentException("Contractor not found: " + contractorId));
        if (contractor.getRole() != UserRole.CONTRACTOR) {
            throw new AssignmentException("Selected user is not a contractor");
        }
        boolean canHandle = contractor.getSpecialisations().contains(IssueType.OTHER)
                || contractor.getSpecialisations().contains(incident.getIncidentType());
        if (!canHandle) {
            throw new AssignmentException(
                    "Contractor is not specialised in " + incident.getIncidentType());
        }

        // Enforce municipality boundary — skip if either party has no municipality (bootstrap/legacy).
        Municipality adminMunicipality = admin.getMunicipality();
        if (adminMunicipality != null && incident.getMunicipality() != null
                && !adminMunicipality.equals(incident.getMunicipality())) {
            throw new AssignmentException("Incident belongs to a different municipality");
        }
        if (adminMunicipality != null && contractor.getMunicipality() != null
                && !adminMunicipality.equals(contractor.getMunicipality())) {
            throw new AssignmentException("Contractor belongs to a different municipality");
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

        UserAuth contractorAuth = userAuthRepository.findById(contractor.getUserId())
                .orElseThrow(() -> new AssignmentException("Contractor auth record not found"));
        mailService.sendJobAssignedEmail(
                contractorAuth.getEmail(),
                contractor.getFirstName(),
                incident.getIncidentType().name(),
                incident.getLocationAddress(),
                incident.getIncidentId().toString()
        );

        String label = formatType(incident.getIncidentType());
        notificationService.notify(contractor, "You have been assigned a " + label + " job");
        incidentReporterRepository.findUserIdsByIncidentId(incidentId).forEach(uid ->
                userRepository.findById(uid).ifPresent(u ->
                        notificationService.notify(u, "A contractor has been assigned to your " + label + " report")));

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

        // Tag the incident to the verifying admin's municipality — it becomes their work item.
        // Enforced spatially where boundary data exists: an admin cannot claim an incident that
        // was actually reported outside their own municipality's real boundary. Municipalities
        // without boundary data on file (boundary == null) fall open — there's no ground truth
        // to check against, so tagging proceeds unchecked as before.
        if (admin.getMunicipality() != null) {
            Municipality municipality = admin.getMunicipality();
            if (municipality.getBoundary() != null && !municipality.getBoundary().contains(incident.getLocation())) {
                throw new AssignmentException(
                        "Incident location falls outside " + municipality.getName() + "'s boundary");
            }
            incident.setMunicipality(municipality);
            incidentRepository.save(incident);
        }

        AssignmentWorkflow workflow = new AssignmentWorkflow();
        workflow.setIncident(incident);
        workflow.setUpdatedBy(admin);
        workflow.setStatus(AssignmentStatus.VERIFIED);
        workflow.setNotes("Verified by " + admin.getFirstName() + " " + admin.getLastName());
        assignmentWorkflowRepository.save(workflow);

        Set<UUID> recipients = new HashSet<>(incidentReporterRepository.findUserIdsByIncidentId(incidentId));
        incidentSseService.pushIncidentUpdate(incidentId, recipients);

        // Notify every reporter linked to this incident.
        String label = formatType(incident.getIncidentType());
        incidentReporterRepository.findUserIdsByIncidentId(incidentId).forEach(uid ->
                userRepository.findById(uid).ifPresent(u ->
                        notificationService.notify(u, "Your " + label + " report has been verified")));

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

        String label = formatType(incident.getIncidentType());
        incidentReporterRepository.findUserIdsByIncidentId(incidentId).forEach(uid ->
                userRepository.findById(uid).ifPresent(u ->
                        notificationService.notify(u, "Work has started on your " + label + " report")));

        return toResponseDTO(incident, incident.getUser().getUserId());
    }

    @Override
    @Transactional
    public IncidentResponseDTO rejectAssignment(UUID incidentId, RejectAssignmentRequest request) {
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
        workflow.setNotes("Rejected by " + contractor.getFirstName() + " " + contractor.getLastName()
                + " — needs reassignment. Reason: " + request.reason());
        assignmentWorkflowRepository.save(workflow);

        Set<UUID> recipients = new HashSet<>(incidentReporterRepository.findUserIdsByIncidentId(incidentId));
        incidentSseService.pushIncidentUpdate(incidentId, recipients);

        // Notify all admins that this incident needs reassignment.
        String label = formatType(incident.getIncidentType());
        userRepository.findByRole(UserRole.ADMIN).forEach(admin ->
                notificationService.notify(admin, "Contractor rejected " + label + " — needs reassignment"));

        return toResponseDTO(incident, incident.getUser().getUserId());
    }

    @Override
    @Transactional
    public IncidentResponseDTO addProgressUpdate(UUID incidentId, String note) {
        final UUID contractorId = currentUserId();
        Assignment assignment = assignmentRepository.findByIncident_IncidentIdAndContractor_UserId(incidentId, contractorId)
                .orElseThrow(() -> new AssignmentException("Assignment not found for this contractor and incident"));

        if (assignment.getStatus() != AssignmentStatus.IN_PROGRESS) {
            throw new AssignmentException("Progress updates can only be added while the incident is IN_PROGRESS");
        }

        User contractor = assignment.getContractor();
        Incident incident = assignment.getIncident();

        AssignmentWorkflow update = AssignmentWorkflow.builder()
                .incident(incident)
                .updatedBy(contractor)
                .status(AssignmentStatus.IN_PROGRESS)
                .notes(note)
                .build();
        assignmentWorkflowRepository.save(update);

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

        String label = formatType(incident.getIncidentType());
        incidentReporterRepository.findUserIdsByIncidentId(incidentId).forEach(uid ->
                userRepository.findById(uid).ifPresent(u ->
                        notificationService.notify(u, "Your " + label + " report has been resolved")));

        return toResponseDTO(incident, incident.getUser().getUserId());
    }

    @Override
    public List<IncidentResponseDTO> getIncidentsPendingAiReview() {
        requireAdmin();
        return incidentRepository.findByAiGeneratedTrueAndDeletedFalseOrderByIncidentDateDesc().stream()
                .filter(incident -> resolveStatus(incident.getIncidentId()) == AssignmentStatus.REPORTED)
                .map(incident -> toResponseDTO(incident, incident.getUser().getUserId()))
                .toList();
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

    @Override
    @Transactional
    public IncidentResponseDTO reopenIncident(UUID incidentId) {
        User admin = requireAdmin();
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new AssignmentException("Incident not found: " + incidentId));

        if (resolveStatus(incidentId) != AssignmentStatus.RESOLVED) {
            throw new AssignmentException("Only resolved incidents can be reopened");
        }

        // Remove the completed assignment so the admin can issue a fresh one.
        assignmentRepository.deleteByIncident_IncidentId(incidentId);

        AssignmentWorkflow workflow = new AssignmentWorkflow();
        workflow.setIncident(incident);
        workflow.setUpdatedBy(admin);
        workflow.setStatus(AssignmentStatus.VERIFIED);
        workflow.setNotes("Reopened by " + admin.getFirstName() + " " + admin.getLastName());
        assignmentWorkflowRepository.save(workflow);

        Set<UUID> recipients = new HashSet<>(incidentReporterRepository.findUserIdsByIncidentId(incidentId));
        incidentSseService.pushIncidentUpdate(incidentId, recipients);

        String label = formatType(incident.getIncidentType());
        incidentReporterRepository.findUserIdsByIncidentId(incidentId).forEach(uid ->
                userRepository.findById(uid).ifPresent(u ->
                        notificationService.notify(u, "Your " + label + " report has been reopened for reassignment")));

        return toResponseDTO(incident, incident.getUser().getUserId());
    }

    @Override
    @Transactional
    public IncidentResponseDTO reportStillUnresolved(UUID incidentId) {
        final UUID userId = currentUserId();
        final User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserServiceException("User not found"));
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new AssignmentException("Incident not found: " + incidentId));

        if (resolveStatus(incidentId) != AssignmentStatus.RESOLVED) {
            throw new AssignmentException("Only resolved incidents can be reported as still unresolved");
        }

        AssignmentWorkflow workflow = new AssignmentWorkflow();
        workflow.setIncident(incident);
        workflow.setStatus(AssignmentStatus.VERIFIED);
        workflow.setNotes("Reported still unresolved by " + user.getFirstName() + " " + user.getLastName());
        assignmentWorkflowRepository.save(workflow);

        boolean alreadyLinked = incidentReporterRepository.existsByIncident_IncidentIdAndUser_UserId(incidentId, userId);
        if (!alreadyLinked) {
            incidentReporterRepository.save(new IncidentReporter(incident, user));
        }

        Set<UUID> recipients = new HashSet<>(incidentReporterRepository.findUserIdsByIncidentId(incidentId));
        incidentSseService.pushIncidentUpdate(incidentId, recipients);

        return toResponseDTO(incident, incident.getUser().getUserId());
    }

    @Override
    public List<IncidentResponseDTO> getNearbyIncidents(double latitude, double longitude, double radiusMeters) {
        return incidentRepository.findNearby(latitude, longitude, radiusMeters).stream()
                .map(incident -> toResponseDTO(incident, incident.getUser().getUserId()))
                .toList();
    }

    @Override
    public IncidentPageResponse searchIncidents(UUID municipalityId, IssueType issueType, int page, int size) {
        Page<Incident> result = incidentRepository.search(
                municipalityId, issueType, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "incidentDate")));
        List<IncidentResponseDTO> content = result.getContent().stream()
                .map(incident -> toResponseDTO(incident, incident.getUser().getUserId()))
                .toList();
        return new IncidentPageResponse(content, result.getTotalElements(), page, size);
    }

    private IncidentResponseDTO toResponseDTO(Incident incident, UUID userId) {
        UUID incidentId = incident.getIncidentId();
        List<WorkflowEntryDTO> history = assignmentWorkflowRepository
                .findAllByIncident_IncidentIdOrderByUpdatedDateAsc(incidentId)
                .stream()
                .map(WorkflowEntryDTO::from)
                .toList();
        return IncidentResponseDTO.builder()
                .incidentId(incidentId)
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
                .reporterCount(incidentReporterRepository.countByIncident_IncidentId(incidentId))
                .duplicate(false)
                .status(resolveStatus(incidentId))
                .workflowHistory(history)
                .aiGenerated(incident.isAiGenerated())
                .aiConfidence(incident.getAiConfidence())
                .build();
    }

    /** Human-readable issue type label, e.g. "POTHOLE" → "Pothole". */
    private static String formatType(IssueType type) {
        if (type == null) return "incident";
        String raw = type.name().replace('_', ' ').toLowerCase();
        return Character.toUpperCase(raw.charAt(0)) + raw.substring(1);
    }

    /** Current status is the most recent workflow entry; incidents with no entries yet are still just REPORTED. */
    private AssignmentStatus resolveStatus(UUID incidentId) {
        return assignmentWorkflowRepository.findFirstByIncident_IncidentIdOrderByUpdatedDateDesc(incidentId)
                .map(AssignmentWorkflow::getStatus)
                .orElse(AssignmentStatus.REPORTED);
    }

    /**
     * Returns the calling ADMIN's municipality, or {@code null} if:
     * <ul>
     *   <li>there is no authenticated principal (e.g. unit tests without a security context),</li>
     *   <li>the caller is not an ADMIN, or</li>
     *   <li>the admin has not been assigned a municipality yet (bootstrap/legacy accounts).</li>
     * </ul>
     * Callers treat {@code null} as "no municipality filter — show everything".
     */
    private Municipality resolveAdminMunicipality() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof String principal)) return null;
        UUID callerId;
        try {
            callerId = UUID.fromString(principal);
        } catch (IllegalArgumentException e) {
            return null;
        }
        return userRepository.findById(callerId)
                .filter(u -> u.getRole() == UserRole.ADMIN)
                .map(User::getMunicipality)
                .orElse(null);
    }

    private UUID currentUserId() {
        return UUID.fromString(
                (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal()
        );
    }
}
