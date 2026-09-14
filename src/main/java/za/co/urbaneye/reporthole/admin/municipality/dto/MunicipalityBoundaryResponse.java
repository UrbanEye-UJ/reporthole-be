package za.co.urbaneye.reporthole.admin.municipality.dto;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;

import java.util.ArrayList;
import java.util.List;

/**
 * GeoJSON-shaped view of a municipality's boundary geometry — {@code type} is always
 * {@code "MultiPolygon"}, and {@code coordinates} nests as polygon &gt; ring &gt; point &gt;
 * {@code [lon, lat]}, matching the GeoJSON spec exactly so the frontend can hand the whole
 * object straight to react-leaflet's {@code <GeoJSON>} component as a geometry.
 *
 * @param type        always {@code "MultiPolygon"}
 * @param coordinates polygon &gt; ring &gt; point &gt; {@code [lon, lat]} nested coordinates
 * @author Refentse
 * @since 1.0
 */
public record MunicipalityBoundaryResponse(String type, List<List<List<double[]>>> coordinates) {

    /**
     * @param boundary the JTS geometry, or {@code null} if the municipality has no boundary yet
     * @return the GeoJSON-shaped DTO, or {@code null} (omitted from the response) if there's no boundary
     */
    public static MunicipalityBoundaryResponse from(MultiPolygon boundary) {
        if (boundary == null) {
            return null;
        }

        final List<List<List<double[]>>> polygons = new ArrayList<>();
        for (int i = 0; i < boundary.getNumGeometries(); i++) {
            final Polygon polygon = (Polygon) boundary.getGeometryN(i);
            final List<List<double[]>> rings = new ArrayList<>();
            rings.add(toRing(polygon.getExteriorRing().getCoordinates()));
            for (int h = 0; h < polygon.getNumInteriorRing(); h++) {
                rings.add(toRing(polygon.getInteriorRingN(h).getCoordinates()));
            }
            polygons.add(rings);
        }
        return new MunicipalityBoundaryResponse("MultiPolygon", polygons);
    }

    private static List<double[]> toRing(Coordinate[] coordinates) {
        final List<double[]> ring = new ArrayList<>(coordinates.length);
        for (Coordinate c : coordinates) {
            ring.add(new double[]{c.x, c.y});
        }
        return ring;
    }
}
