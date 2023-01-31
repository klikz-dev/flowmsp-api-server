package com.flowmsp.service.MessageParser;

import com.flowmsp.domain.psap.PSAP;
import org.bson.codecs.pojo.annotations.BsonProperty;

import com.flowmsp.domain.location.Location;
import com.mongodb.client.model.geojson.Point;
import java.util.List;
import java.util.ArrayList;

public class ParsedMessage {
    public String text;
    public String Address;
    public String Code;
    public Location location;
    @BsonProperty("lonLat")     // The database uses lonLat due to legacy issues, may want to adjust at some point
    public Point messageLatLon;
    public int ErrorFlag;
    public PSAP psap;
    public List<String> units = new ArrayList<>();
    public String incidentID;
}
