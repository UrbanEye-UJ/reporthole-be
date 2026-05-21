package za.co.urbaneye.reporthole.incident.service.impl;

import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Service;
import za.co.urbaneye.reporthole.incident.dto.IncidentRequestDTO;
import za.co.urbaneye.reporthole.incident.entity.Incident;
import za.co.urbaneye.reporthole.incident.repository.IncidentRepository;
import za.co.urbaneye.reporthole.incident.service.interfaces.ImageStorageService;
import za.co.urbaneye.reporthole.incident.service.interfaces.IncidentService;
import za.co.urbaneye.reporthole.user.entity.User;
import za.co.urbaneye.reporthole.user.repository.IUserAuthRepository;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class IncidentServiceImpl implements IncidentService {

    private final IncidentRepository incidentRepository;
    private final IUserAuthRepository userRepository;
    private final ImageStorageService imageStorageService;
    private final GeometryFactory geometryFactory = new GeometryFactory();
    @Override
    public Incident createIncident(IncidentRequestDTO request) {
        final User user = userRepository.findById(request.getUserId())
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
        return incidentRepository.save(incident);
    }
}
