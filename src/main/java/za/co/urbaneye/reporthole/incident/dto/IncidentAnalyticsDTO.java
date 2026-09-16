package za.co.urbaneye.reporthole.incident.dto;

import za.co.urbaneye.reporthole.incident.entity.AssignmentStatus;
import za.co.urbaneye.reporthole.incident.entity.IssueType;

import java.util.List;

/**
 * Aggregated, municipality-scoped operational analytics for the admin/security-admin analytics
 * dashboard. Every figure is derived from data already recorded elsewhere — nothing here is
 * estimated or hardcoded.
 */
public record IncidentAnalyticsDTO(
        long totalIncidents,
        long resolvedIncidents,
        long openIncidents,
        Double avgResolutionHours,
        List<StatusBreakdownEntry> statusBreakdown,
        List<TypeBreakdownEntry> typeBreakdown,
        List<MonthlyTrendEntry> monthlyTrend,
        List<ResolutionTrendEntry> resolutionTimeTrend
) {
    public record StatusBreakdownEntry(AssignmentStatus status, long count) {
    }

    public record TypeBreakdownEntry(IssueType type, long count) {
    }

    /** {@code month} is formatted "YYYY-MM". */
    public record MonthlyTrendEntry(String month, long count) {
    }

    /** {@code month} is formatted "YYYY-MM"; {@code avgHours} is null if nothing resolved that month. */
    public record ResolutionTrendEntry(String month, Double avgHours) {
    }
}
