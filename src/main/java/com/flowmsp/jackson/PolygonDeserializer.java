package com.flowmsp.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.mongodb.client.model.geojson.Polygon;
import com.mongodb.client.model.geojson.Position;

import java.io.IOException;
import java.util.ArrayList;

public class PolygonDeserializer extends StdDeserializer<Polygon>
{
    public PolygonDeserializer() {
        this(null);
    }

    public PolygonDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public Polygon deserialize(JsonParser p, DeserializationContext ctxt) throws IOException
    {
        JsonNode node = p.getCodec().readTree(p);
        ArrayList<Position> points = new ArrayList<>();
        for(JsonNode jsonNode : node)
        {
            double lat = jsonNode.get("latitude").doubleValue();
            double lon = jsonNode.get("longitude").doubleValue();
            points.add(new Position(lon, lat));
        }

        return new Polygon(points);
    }
}
