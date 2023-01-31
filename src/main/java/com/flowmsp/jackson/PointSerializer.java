package com.flowmsp.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.mongodb.client.model.geojson.Point;

import java.io.IOException;

public class PointSerializer extends StdSerializer<Point> {

    public PointSerializer() {
        super(Point.class);
    }

    @Override
    public void serialize(Point value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        gen.writeNumberField("latitude", value.getCoordinates().getValues().get(1));
        gen.writeNumberField("longitude", value.getCoordinates().getValues().get(0));
        gen.writeEndObject();
    }
}
