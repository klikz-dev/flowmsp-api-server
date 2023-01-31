package com.flowmsp.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Position;

import java.io.IOException;
import java.util.Iterator;

public class PointDeserializer extends StdDeserializer<Point> {

    public PointDeserializer() {
        super(Point.class);
    }

    @Override
    public Point deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);
        if(node.get("longitude") == null) {
            if(node.get("coordinates") == null) {
                return null;
            }
            else {
                Iterator<JsonNode> i = node.get("coordinates").elements();
                double longitude = i.next().asDouble();
                double latitude  = i.next().asDouble();
                return new Point(new Position(longitude, latitude));
            }
        }
        else {
            double longitude = node.get("longitude").doubleValue();
            double latitude = node.get("latitude").doubleValue();
            return new Point(new Position(longitude, latitude));
        }
    }
}
