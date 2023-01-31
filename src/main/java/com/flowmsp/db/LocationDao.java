package com.flowmsp.db;

import com.flowmsp.domain.hydrant.Hydrant;
import com.flowmsp.domain.location.Location;
import com.google.common.collect.Lists;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Position;

import static com.mongodb.client.model.Filters.geoIntersects;

import java.util.List;

public class LocationDao extends BaseDao<Location> {

    public LocationDao(MongoDatabase database) {
        super(database, Location.class, true);
    }

    @Override
    protected String getCollectionName(String slug) {
        return slug.concat(".Location");
    }

    public List<Location> getAssociatedWithHydrant(String hydrantId) {
        return getAllByFieldValue("hydrants", hydrantId);
    }
    
    public List<Location> getWithIn(Point p) {    	
        return getAllByFilter(geoIntersects("geoOutline", p));
    }

    public List<Location> getWithIn(Double latitude, Double longitude) {
        Position pos = new Position(Lists.newArrayList(longitude, latitude));
        Point p = new Point(pos);
        return getWithIn(p);
    }
}
