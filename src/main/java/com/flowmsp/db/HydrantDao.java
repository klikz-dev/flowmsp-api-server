package com.flowmsp.db;

import com.flowmsp.domain.hydrant.Hydrant;
import com.google.common.collect.Lists;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Position;

import java.util.List;

import static com.mongodb.client.model.Filters.*;


public class HydrantDao extends BaseDao<Hydrant> {
    public HydrantDao(MongoDatabase database) { super(database, Hydrant.class, true); }

    @Override
    protected String getCollectionName(String slug) {
        return slug.concat(".Hydrant");
    }

    @Override
    protected void createIndexes(MongoCollection mongoCollection) {
        mongoCollection.createIndex(Indexes.geo2dsphere("lonLat"));
    }

    public List<Hydrant> getNear(Point p, Double maxMeters) {
        return getAllByFilter(near("lonLat", p, maxMeters, 0.0));
    }

    public List<Hydrant> getNear(Double latitude, Double longitude, Double maxMeters) {
        Position pos = new Position(Lists.newArrayList(longitude, latitude));
        Point p = new Point(pos);
        return getNear(p, maxMeters);
    }
}
