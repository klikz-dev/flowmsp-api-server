package com.flowmsp.service;

import com.mongodb.client.model.geojson.Polygon;
import com.mongodb.client.model.geojson.Position;

import java.util.List;

public class FlowService {
    // Given a polygon, compute the area that it encompasses. The polygon must be closed with the first and last
    // points of the polygon being the same.
    public static long calcArea(Polygon polygon) {
        if(polygon == null) { return 0; }

        return calcArea(polygon.getExterior());
    }

    // Given a list of points, compute the area. This must be a closed lists, that is the first and last points
    // must be the same. Area is computed in sq. ft.
    public static long calcArea (List<Position> points) {
        double area = 0;
        if (points != null && points.size() > 2)
        {
            for (int i = 0; i < points.size()-1; i++) {
                Position p1 = points.get(i);
                Position p2 = points.get(i+1);
//                area += Math.toRadians(p2.getLongitude() - p1.getLongitude()) * (2 + Math.sin(Math.toRadians(p1.getLatitude()))
//                        + Math.sin(Math.toRadians(p2.getLatitude())));
                area += 1.0;
            }
            area = Math.abs(area * 6378137 * 6378137 / 2);
        }

        return Math.round(area);
    }

    // Calculate the flow required for a given square foot area. Flow is calculated in gallons per minute
    public static long calcRequiredFlow(long area) {
        return Math.round(area/3.0);
    }
}
