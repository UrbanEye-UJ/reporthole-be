package za.co.urbaneye.reporthole.incident.clustering;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import za.co.urbaneye.reporthole.incident.entity.Incident;
import za.co.urbaneye.reporthole.incident.entity.IssueType;
import za.co.urbaneye.reporthole.incident.repository.IncidentRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Groups incidents into geographic clusters using Lloyd's K-Means algorithm with
 * k-means++ centroid seeding and Haversine great-circle distance.
 *
 * <p>Centroids are recomputed each iteration as the arithmetic mean of each cluster's
 * latitude/longitude — a standard approximation that is accurate enough at city scale,
 * where clusters span at most a few kilometres.</p>
 */
@Service
@RequiredArgsConstructor
public class KMeansIncidentClusteringService implements IncidentClusteringService {

    private static final int MAX_ITERATIONS = 100;
    private static final double CONVERGENCE_EPSILON_METRES = 1.0;

    private final IncidentRepository incidentRepository;

    @Override
    public List<IncidentClusterDTO> clusterIncidents(int k, IssueType issueType) {
        if (k < 1) {
            throw new IllegalArgumentException("k must be at least 1");
        }

        List<LocatedIncident> points = incidentRepository.findByDeletedFalse().stream()
                .filter(incident -> issueType == null || incident.getIncidentType() == issueType)
                .map(this::toLocatedIncident)
                .toList();

        if (points.isEmpty()) {
            return List.of();
        }

        int effectiveK = Math.min(k, points.size());
        double[][] centroids = initializeCentroidsPlusPlus(points, effectiveK);
        int[] assignments = new int[points.size()];

        for (int iteration = 0; iteration < MAX_ITERATIONS; iteration++) {
            boolean assignmentsChanged = assignToNearestCentroid(points, centroids, assignments);
            double[][] recomputed = recomputeCentroids(points, assignments, centroids, effectiveK);
            boolean centroidsMoved = !centroidsConverged(centroids, recomputed);
            centroids = recomputed;

            if (!assignmentsChanged && !centroidsMoved) {
                break;
            }
        }

        return buildClusters(points, assignments, centroids, effectiveK);
    }

    private LocatedIncident toLocatedIncident(Incident incident) {
        return new LocatedIncident(
                incident.getIncidentId(),
                incident.getLocation().getY(),
                incident.getLocation().getX());
    }

    /**
     * k-means++ seeding: the first centroid is picked uniformly at random; each
     * subsequent centroid is picked from the remaining points with probability
     * proportional to its squared distance from the nearest already-chosen centroid.
     * This spreads initial centroids out and gives far more reliable convergence
     * than naive random initialization.
     */
    private double[][] initializeCentroidsPlusPlus(List<LocatedIncident> points, int k) {
        Random random = new Random(42);
        double[][] centroids = new double[k][2];

        LocatedIncident first = points.get(random.nextInt(points.size()));
        centroids[0] = new double[]{first.latitude(), first.longitude()};

        for (int c = 1; c < k; c++) {
            double[] squaredDistances = new double[points.size()];
            double totalWeight = 0.0;

            for (int p = 0; p < points.size(); p++) {
                LocatedIncident point = points.get(p);
                double minDist = Double.MAX_VALUE;
                for (int existing = 0; existing < c; existing++) {
                    double dist = GeoDistanceUtil.haversineMeters(
                            point.latitude(), point.longitude(), centroids[existing][0], centroids[existing][1]);
                    minDist = Math.min(minDist, dist);
                }
                squaredDistances[p] = minDist * minDist;
                totalWeight += squaredDistances[p];
            }

            int chosen;
            if (totalWeight <= 0.0) {
                // Every remaining point coincides with an existing centroid — pick arbitrarily.
                chosen = c % points.size();
            } else {
                double target = random.nextDouble() * totalWeight;
                double cumulative = 0.0;
                chosen = points.size() - 1;
                for (int p = 0; p < points.size(); p++) {
                    cumulative += squaredDistances[p];
                    if (cumulative >= target) {
                        chosen = p;
                        break;
                    }
                }
            }
            LocatedIncident point = points.get(chosen);
            centroids[c] = new double[]{point.latitude(), point.longitude()};
        }
        return centroids;
    }

    private boolean assignToNearestCentroid(List<LocatedIncident> points, double[][] centroids, int[] assignments) {
        boolean changed = false;
        for (int i = 0; i < points.size(); i++) {
            LocatedIncident point = points.get(i);
            int nearest = 0;
            double nearestDist = Double.MAX_VALUE;
            for (int c = 0; c < centroids.length; c++) {
                double dist = GeoDistanceUtil.haversineMeters(
                        point.latitude(), point.longitude(), centroids[c][0], centroids[c][1]);
                if (dist < nearestDist) {
                    nearestDist = dist;
                    nearest = c;
                }
            }
            if (assignments[i] != nearest) {
                changed = true;
                assignments[i] = nearest;
            }
        }
        return changed;
    }

    /**
     * Recomputes each centroid as the mean location of its assigned points. A cluster
     * left with no members (possible after a bad seed) is reseeded at the point currently
     * farthest from its own centroid, so it has a chance to pick up members next iteration
     * instead of staying permanently empty.
     */
    private double[][] recomputeCentroids(List<LocatedIncident> points, int[] assignments, double[][] previousCentroids, int k) {
        double[][] sums = new double[k][2];
        int[] counts = new int[k];

        for (int i = 0; i < points.size(); i++) {
            int cluster = assignments[i];
            sums[cluster][0] += points.get(i).latitude();
            sums[cluster][1] += points.get(i).longitude();
            counts[cluster]++;
        }

        double[][] newCentroids = new double[k][2];
        for (int c = 0; c < k; c++) {
            if (counts[c] == 0) {
                newCentroids[c] = farthestPointFromItsCentroid(points, assignments, previousCentroids);
            } else {
                newCentroids[c] = new double[]{sums[c][0] / counts[c], sums[c][1] / counts[c]};
            }
        }
        return newCentroids;
    }

    private double[] farthestPointFromItsCentroid(List<LocatedIncident> points, int[] assignments, double[][] centroids) {
        int farthestIndex = 0;
        double maxDist = -1.0;
        for (int i = 0; i < points.size(); i++) {
            LocatedIncident point = points.get(i);
            double[] centroid = centroids[assignments[i]];
            double dist = GeoDistanceUtil.haversineMeters(point.latitude(), point.longitude(), centroid[0], centroid[1]);
            if (dist > maxDist) {
                maxDist = dist;
                farthestIndex = i;
            }
        }
        LocatedIncident farthest = points.get(farthestIndex);
        return new double[]{farthest.latitude(), farthest.longitude()};
    }

    private boolean centroidsConverged(double[][] previous, double[][] current) {
        for (int c = 0; c < previous.length; c++) {
            double dist = GeoDistanceUtil.haversineMeters(previous[c][0], previous[c][1], current[c][0], current[c][1]);
            if (dist > CONVERGENCE_EPSILON_METRES) {
                return false;
            }
        }
        return true;
    }

    private List<IncidentClusterDTO> buildClusters(List<LocatedIncident> points, int[] assignments, double[][] centroids, int k) {
        List<List<UUID>> clusterMembers = new ArrayList<>(k);
        for (int c = 0; c < k; c++) {
            clusterMembers.add(new ArrayList<>());
        }
        for (int i = 0; i < points.size(); i++) {
            clusterMembers.get(assignments[i]).add(points.get(i).incidentId());
        }

        List<IncidentClusterDTO> clusters = new ArrayList<>();
        for (int c = 0; c < k; c++) {
            List<UUID> members = clusterMembers.get(c);
            if (members.isEmpty()) {
                continue;
            }
            clusters.add(IncidentClusterDTO.builder()
                    .clusterIndex(c)
                    .centroidLatitude(centroids[c][0])
                    .centroidLongitude(centroids[c][1])
                    .size(members.size())
                    .incidentIds(members)
                    .build());
        }
        return clusters;
    }

    private record LocatedIncident(UUID incidentId, double latitude, double longitude) {}
}
