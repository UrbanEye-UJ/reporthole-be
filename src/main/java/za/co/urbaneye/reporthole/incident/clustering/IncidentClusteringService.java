package za.co.urbaneye.reporthole.incident.clustering;

import za.co.urbaneye.reporthole.incident.entity.IssueType;

import java.util.List;

public interface IncidentClusteringService {

    /**
     * Groups all non-deleted incidents into {@code k} clusters of nearby locations
     * using K-Means, optionally restricted to a single issue type.
     *
     * @param k        the number of clusters to form; reduced automatically if fewer
     *                 incidents than {@code k} are available
     * @param issueType when non-null, only incidents of this type are clustered
     * @return the resulting clusters, ordered by cluster index; empty if there are
     *         no matching incidents
     * @throws IllegalArgumentException if {@code k} is less than 1
     */
    List<IncidentClusterDTO> clusterIncidents(int k, IssueType issueType);
}
