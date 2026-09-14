package za.co.urbaneye.reporthole.incident.clustering;

import za.co.urbaneye.reporthole.incident.entity.IssueType;

import java.util.List;
import java.util.UUID;

public interface IncidentClusteringService {

    /**
     * Groups non-deleted incidents into {@code k} clusters of nearby locations using
     * K-Means, optionally restricted to a single issue type and/or a single municipality.
     *
     * @param k              the number of clusters to form; reduced automatically if fewer
     *                       incidents than {@code k} are available
     * @param issueType      when non-null, only incidents of this type are clustered
     * @param municipalityId when non-null, only incidents belonging to this municipality are
     *                       clustered — used by the SECURITY_ADMIN map view to inspect one
     *                       municipality's hotspots at a time
     * @return the resulting clusters, ordered by cluster index; empty if there are
     *         no matching incidents
     * @throws IllegalArgumentException if {@code k} is less than 1
     */
    List<IncidentClusterDTO> clusterIncidents(int k, IssueType issueType, UUID municipalityId);
}
