package za.co.urbaneye.reporthole.incident.clustering;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.co.urbaneye.reporthole.incident.entity.Incident;
import za.co.urbaneye.reporthole.incident.entity.IssueType;
import za.co.urbaneye.reporthole.incident.repository.IncidentRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KMeansIncidentClusteringServiceTest {

    @Mock
    private IncidentRepository incidentRepository;

    @InjectMocks
    private KMeansIncidentClusteringService clusteringService;

    private static final GeometryFactory GF = new GeometryFactory(new PrecisionModel(), 4326);

    // Three real-world locations far enough apart (tens to hundreds of km) that any
    // reasonable K-Means run must separate them into distinct clusters, regardless
    // of centroid seeding.
    private static final double[] JHB = {-26.2041, 28.0473};
    private static final double[] PTA = {-25.7479, 28.2293};
    private static final double[] CPT = {-33.9249, 18.4241};

    private Incident buildIncident(double[] base, double jitter, IssueType type) {
        Incident incident = new Incident();
        incident.setIncidentId(UUID.randomUUID());
        incident.setIncidentType(type);
        incident.setLocation(GF.createPoint(new Coordinate(base[1] + jitter, base[0] + jitter)));
        return incident;
    }

    @Test
    void clusterIncidents_throws_whenKIsLessThanOne() {
        assertThatThrownBy(() -> clusteringService.clusterIncidents(0, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void clusterIncidents_returnsEmpty_whenNoIncidents() {
        when(incidentRepository.findByDeletedFalse()).thenReturn(List.of());

        List<IncidentClusterDTO> clusters = clusteringService.clusterIncidents(3, null);

        assertThat(clusters).isEmpty();
    }

    @Test
    void clusterIncidents_groupsWellSeparatedIncidentsIntoDistinctClusters() {
        Map<UUID, String> groupByIncidentId = new HashMap<>();
        List<Incident> incidents = new java.util.ArrayList<>();

        for (int i = 0; i < 4; i++) {
            Incident jhb = buildIncident(JHB, i * 0.001, IssueType.POTHOLE);
            Incident pta = buildIncident(PTA, i * 0.001, IssueType.POTHOLE);
            Incident cpt = buildIncident(CPT, i * 0.001, IssueType.POTHOLE);
            groupByIncidentId.put(jhb.getIncidentId(), "JHB");
            groupByIncidentId.put(pta.getIncidentId(), "PTA");
            groupByIncidentId.put(cpt.getIncidentId(), "CPT");
            incidents.add(jhb);
            incidents.add(pta);
            incidents.add(cpt);
        }

        when(incidentRepository.findByDeletedFalse()).thenReturn(incidents);

        List<IncidentClusterDTO> clusters = clusteringService.clusterIncidents(3, null);

        assertThat(clusters).hasSize(3);
        assertThat(clusters.stream().mapToInt(IncidentClusterDTO::size).sum()).isEqualTo(12);

        // Every incident ID from the source list appears in exactly one cluster.
        Set<UUID> seen = new java.util.HashSet<>();
        for (IncidentClusterDTO cluster : clusters) {
            assertThat(seen.addAll(cluster.incidentIds())).isTrue();
        }
        assertThat(seen).hasSize(12);

        // Each cluster is "pure" — all its members came from the same geographic group.
        for (IncidentClusterDTO cluster : clusters) {
            Set<String> groupsInCluster = cluster.incidentIds().stream()
                    .map(groupByIncidentId::get)
                    .collect(java.util.stream.Collectors.toSet());
            assertThat(groupsInCluster).hasSize(1);
        }
    }

    @Test
    void clusterIncidents_filtersByIssueType_whenTypeProvided() {
        Incident pothole = buildIncident(JHB, 0.0, IssueType.POTHOLE);
        Incident crack = buildIncident(JHB, 0.001, IssueType.CRACK);
        when(incidentRepository.findByDeletedFalse()).thenReturn(List.of(pothole, crack));

        List<IncidentClusterDTO> clusters = clusteringService.clusterIncidents(2, IssueType.POTHOLE);

        assertThat(clusters).hasSize(1);
        assertThat(clusters.getFirst().incidentIds()).containsExactly(pothole.getIncidentId());
    }

    @Test
    void clusterIncidents_reducesKToNumberOfPoints_whenKExceedsPointCount() {
        Incident only = buildIncident(JHB, 0.0, IssueType.POTHOLE);
        when(incidentRepository.findByDeletedFalse()).thenReturn(List.of(only));

        List<IncidentClusterDTO> clusters = clusteringService.clusterIncidents(5, null);

        assertThat(clusters).hasSize(1);
        assertThat(clusters.getFirst().size()).isEqualTo(1);
        assertThat(clusters.getFirst().incidentIds()).containsExactly(only.getIncidentId());
    }
}
