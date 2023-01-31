package com.flowmsp.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.mongodb.client.model.geojson.Polygon;
import com.mongodb.client.model.geojson.Position;

import java.io.IOException;

public class PolygonSerializer extends StdSerializer<Polygon> {

    public PolygonSerializer() {
        super(Polygon.class);
    }

    @Override
    public void serialize(Polygon value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartArray();
        for(Position position : value.getExterior()) {
            gen.writeStartObject();
            gen.writeNumberField("latitude", position.getValues().get(1));
            gen.writeNumberField("longitude", position.getValues().get(0));
            gen.writeEndObject();
        }
        gen.writeEndArray();
    }
}
