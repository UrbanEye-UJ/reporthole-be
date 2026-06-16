package za.co.urbaneye.reporthole.incident.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import za.co.urbaneye.reporthole.incident.dto.IncidentRequestDTO;
import za.co.urbaneye.reporthole.incident.dto.IncidentResponseDTO;
import za.co.urbaneye.reporthole.incident.entity.Incident;
import za.co.urbaneye.reporthole.incident.entity.IncidentReporter;
import za.co.urbaneye.reporthole.incident.repository.IncidentRepository;
import za.co.urbaneye.reporthole.incident.repository.IncidentReporterRepository;
import za.co.urbaneye.reporthole.incident.service.interfaces.ImageStorageService;
import za.co.urbaneye.reporthole.incident.service.interfaces.IncidentService;
import za.co.urbaneye.reporthole.user.entity.User;
import za.co.urbaneye.reporthole.user.exception.UserServiceException;
import za.co.urbaneye.reporthole.user.repository.IUserAuthRepository;

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
    private final IUserAuthRepository userRepository;
    private final ImageStorageService imageStorageService;
    private final IncidentSseService incidentSseService;

    private static final double MANUAL_DUPLICATE_RADIUS_METRES = 1_000.0;
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    @Override
    public IncidentResponseDTO createIncident(IncidentRequestDTO request) {
        final UUID userId = currentUserId();
        final User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserServiceException("User not found"));
        final Point point = geometryFactory.createPoint(
                new Coordinate(request.getLongitude(), request.getLatitude())
        );

        Optional<Incident> nearestDuplicate = request.isForceCreate()
                ? Optional.empty()
                : incidentRepository.findNearestDuplicate(
                        request.getLatitude(), request.getLongitude(),
                        MANUAL_DUPLICATE_RADIUS_METRES, request.getIncidentType());

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
                    .build();
        }

        final String imageUrl = imageStorageService.saveBase64Image(request.getImageBase64());
        Incident incident = new Incident();
        incident.setIncidentType(request.getIncidentType());
        incident.setDescription(request.getDescription());
        incident.setSource(request.getSource());
        incident.setIncidentDate(LocalDateTime.now());
        incident.setLocation(point);
        incident.setImageUrl(imageUrl);
        incident.setLocationAddress(request.getLocationAddress());
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
                .build();
    }

    @Override
    public List<IncidentResponseDTO> getMyIncidents() {
        final UUID userId = currentUserId();
        return incidentRepository.findAllReportedByUser(userId).stream()
                .map(incident -> IncidentResponseDTO.builder()
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
                        .build())
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
                .build();
    }

    private UUID currentUserId() {
        return UUID.fromString(
                (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal()
        );
    }
}
