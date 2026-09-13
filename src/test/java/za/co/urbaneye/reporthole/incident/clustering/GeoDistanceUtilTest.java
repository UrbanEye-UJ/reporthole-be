package za.co.urbaneye.reporthole.incident.clustering;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class GeoDistanceUtilTest {

    @Test
    void samePoint_isZeroDistance() {
        double distance = GeoDistanceUtil.haversineMeters(-26.2041, 28.0473, -26.2041, 28.0473);
        assertThat(distance).isEqualTo(0.0, within(1e-6));
    }

    @Test
    void oneDegreeLatitude_isApproximately111Kilometres() {
        double distance = GeoDistanceUtil.haversineMeters(0.0, 0.0, 1.0, 0.0);
        assertThat(distance).isCloseTo(111_195.0, within(500.0));
    }

    @Test
    void knownCityPair_matchesApproximateDistance() {
        // Johannesburg CBD to Pretoria CBD — real-world distance is ~50km.
        double distance = GeoDistanceUtil.haversineMeters(-26.2041, 28.0473, -25.7479, 28.2293);
        assertThat(distance).isBetween(45_000.0, 55_000.0);
    }

    @Test
    void isSymmetric() {
        double a = GeoDistanceUtil.haversineMeters(-26.2041, 28.0473, -25.7479, 28.2293);
        double b = GeoDistanceUtil.haversineMeters(-25.7479, 28.2293, -26.2041, 28.0473);
        assertThat(a).isEqualTo(b, within(1e-6));
    }
}
