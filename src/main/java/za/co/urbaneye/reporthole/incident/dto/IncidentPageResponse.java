package za.co.urbaneye.reporthole.incident.dto;

import java.util.List;

/**
 * One page of a filtered incident search — the SECURITY_ADMIN incidents view's table, which
 * needs real server-side pagination (not a flat capped list) since it's combined with
 * municipality and issue-type filters.
 *
 * @param content        the incidents on this page
 * @param totalElements  total incidents matching the filter, across all pages
 * @param page           the requested page number (0-based)
 * @param size           the requested page size
 * @author Refentse
 * @since 1.0
 */
public record IncidentPageResponse(
        List<IncidentResponseDTO> content,
        long totalElements,
        int page,
        int size
) {}
