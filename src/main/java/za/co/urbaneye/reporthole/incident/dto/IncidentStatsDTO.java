package za.co.urbaneye.reporthole.incident.dto;

public record IncidentStatsDTO(
        long totalIncidents,
        long resolvedIncidents
) {
}
