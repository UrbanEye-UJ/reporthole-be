package za.co.urbaneye.reporthole.incident.clustering;

import lombok.Builder;

import java.util.List;
import java.util.UUID;

/**
 * A group of incidents that are geographically close to one another, as identified
 * by K-Means clustering on their reported locations.
 */
@Builder
public record IncidentClusterDTO(
        int clusterIndex,
        double centroidLatitude,
        double centroidLongitude,
        int size,
        List<UUID> incidentIds
) {}
