package za.co.urbaneye.reporthole.incident.service.impl;

import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import za.co.urbaneye.reporthole.incident.dto.IncidentRequestDTO;
import za.co.urbaneye.reporthole.incident.dto.IncidentResponseDTO;
import za.co.urbaneye.reporthole.incident.entity.Incident;
import za.co.urbaneye.reporthole.incident.repository.IncidentRepository;
import za.co.urbaneye.reporthole.incident.service.interfaces.ImageStorageService;
import za.co.urbaneye.reporthole.incident.service.interfaces.IncidentService;
import za.co.urbaneye.reporthole.user.entity.User;
import za.co.urbaneye.reporthole.user.repository.IUserAuthRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IncidentServiceImpl implements IncidentService {

    private final IncidentRepository incidentRepository;
    private final IUserAuthRepository userRepository;
    private final ImageStorageService imageStorageService;
    private final GeometryFactory geometryFactory = new GeometryFactory();
    @Override
    public IncidentResponseDTO createIncident(IncidentRequestDTO request) {
        final String rawId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        final UUID userId = UUID.fromString(rawId);
        final User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        final Point point = geometryFactory.createPoint(
                new Coordinate(request.getLongitude(), request.getLatitude())
        );
        point.setSRID(4326);
        final String imageUrl = imageStorageService.saveBase64Image(request.getImageBase64());
        Incident incident = new Incident();
        incident.setIncidentType(request.getIncidentType());
        incident.setDescription(request.getDescription());
        incident.setSource(request.getSource());
        incident.setIncidentDate(LocalDateTime.now());
        incident.setLocation(point);
        incident.setImageUrl(imageUrl);
        incident.setUser(user);
        final Incident saved = incidentRepository.save(incident);
        return IncidentResponseDTO.builder()
                .incidentId(saved.getIncidentId())
                .incidentType(saved.getIncidentType())
                .description(saved.getDescription())
                .source(saved.getSource())
                .incidentDate(saved.getIncidentDate())
                .latitude(point.getY())
                .longitude(point.getX())
                .imageUrl(saved.getImageUrl())
                .userId(user.getUserId())
                .build();
    }

    @Override
    public List<IncidentResponseDTO> getMyIncidents() {
        final UUID userId = UUID.fromString(
                (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal()
        );
        return incidentRepository.findByUser_UserId(userId).stream()
                .map(incident -> IncidentResponseDTO.builder()
                        .incidentId(incident.getIncidentId())
                        .incidentType(incident.getIncidentType())
                        .description(incident.getDescription())
                        .source(incident.getSource())
                        .incidentDate(incident.getIncidentDate())
                        .latitude(incident.getLocation().getY())
                        .longitude(incident.getLocation().getX())
                        .imageUrl(incident.getImageUrl())
                        .userId(userId)
                        .build())
                .toList();
    }
}
