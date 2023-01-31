package com.flowmsp.service;

import com.google.common.collect.Lists;
import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Polygon;
import com.mongodb.client.model.geojson.Position;

import java.util.ArrayList;
import java.util.List;

public interface PolygonUtil {

    /**
     * Given a polygon, return the bounding box around it. This works with a polygon in any coordinate system.
     *
     * @param polygon   The polygon to be analysed
     * @return          The bounding box around the input polygon
     */
    static Polygon getBoundingBox(Polygon polygon) {
        double minLongitude = Double.POSITIVE_INFINITY;
        double maxLongitude = Double.NEGATIVE_INFINITY;
        double minLatitude  = Double.POSITIVE_INFINITY;
        double maxLatitude  = Double.NEGATIVE_INFINITY;

        for(Position position : polygon.getExterior()) {
            double longitude = position.getValues().get(0);
            double latitude  = position.getValues().get(1);
            if(longitude < minLongitude) minLongitude = longitude;
            if(longitude > maxLongitude) maxLongitude = longitude;
            if(latitude < minLatitude) minLatitude = latitude;
            if(latitude > maxLatitude) maxLatitude = latitude;
        }

        Position p1 = new Position(minLongitude, minLatitude);
        Position p2 = new Position(minLongitude, maxLatitude);
        Position p3 = new Position(maxLongitude, maxLatitude);
        Position p4 = new Position(maxLongitude, minLatitude);
        Position p5 = new Position(minLongitude, minLatitude);

        return new Polygon(Lists.newArrayList(p1, p2, p3, p4, p5));
    }

    /**
     * Given a polygon, find the center of the bounding box surrounding that polygon. This works with polygons
     * specified in any coordinate system.
     *
     * @param polygon   The polygon to be analysed
     * @return          The point at the center of the bounding box around the polygon
     */
    static Point getCenter(Polygon polygon) {
        Polygon p = getBoundingBox(polygon);
        Position p1 = p.getExterior().get(0);
        Position p2 = p.getExterior().get(2);

        Double longitude = (p1.getValues().get(0) + p2.getValues().get(0)) / 2.0;
        Double latitude  = (p1.getValues().get(1) + p2.getValues().get(1)) / 2.0;

        return new Point(new Position(longitude, latitude));
    }

    /**
     * Given a polygon specified with lat/lon coordinates, return a polygon mapped to cartesian coordinates with the
     * first point of the polygon at the origin. The units of measure for the resulting polygon are in meters.
     *
     * @param polygon   The polygon in lat/lon coordinates to be converted
     * @return          The resulting polygon in cartesian coordinates
     */
    static Polygon cartesianConversion(Polygon polygon) {
        List<Position> positions = polygon.getExterior();
        double lonAnchor = positions.get(0).getValues().get(0);
        double latAnchor = positions.get(0).getValues().get(1);

        List<Position> cartesianPositions = new ArrayList<>();
        for(int x = 0; x < positions.size(); ++x) {
            double lon = positions.get(x).getValues().get(0);
            double lat = positions.get(x).getValues().get(1);

            double xPos = (lon - lonAnchor) * (6378137 * Math.PI / 180.0) * Math.cos(latAnchor * Math.PI / 180);
            double yPos = (lat - latAnchor) * (6378137 * Math.PI / 180.0);

            cartesianPositions.add(new Position(xPos, yPos));
        }

        return new Polygon(cartesianPositions);
    }

    /**
     * Given a polygon specified with lat/lon coordinates, return the area in sq ft of that polygon. Uses the
     * cross product formula as explained at http://mathworld.wolfram.com/PolygonArea.html
     *
     * @param polygon   The polygon in lat/lon coordinates
     * @return          The area of the polygon in sq ft
     */
    static Double areaLatLonPolygon(Polygon polygon) {
        // Since the cartesian conversion results in a polygon on a grid with meters as the unit of measure, the
        // area computation results in sq. meters. This must be converted to sq. ft.
        return areaCartesianPolygon(cartesianConversion(polygon))  *  10.76391041671;
    }

    /**
     * Given a polygon specified with cartestian coordinates, return the area of that polygon. Uses the
     * cross product formula as explained at http://mathworld.wolfram.com/PolygonArea.html
     *
     * @param polygon   The polygon in cartestian coordinates
     * @return          The area of the polygon
     */
    static Double areaCartesianPolygon(Polygon polygon) {
        List<Position> positions = polygon.getExterior();
        Double area = 0.0;

        for(int x = 1; x < positions.size(); ++x) {
            Position p0 = positions.get(x-1);
            Position p1 = positions.get(x);

            area = area + (p0.getValues().get(0) * p1.getValues().get(1) - p0.getValues().get(1) * p1.getValues().get(0));
        }
        return Math.abs(area/2.0);
    }

    static Double feetToMeters(Double feet) {
        return feet * 0.3048;
    }

    static Double feetToMeters(Integer feet) {
        return feet * 0.3048;
    }
}
