package com.flowmsp.domain;

import org.bson.codecs.pojo.annotations.BsonProperty;

import com.flowmsp.domain.location.Location;
import com.flowmsp.service.patch.AllowedPatches;
import com.flowmsp.service.patch.PatchOp;
import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Position;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;

/**
 * Common class for address information, intended to be embedded in other objects which
 * have address information.
 */
public class Message {
    public String    id;
    public Long 	sequence;
    public String    customerId;
    public String    customerSlug;
    public String textRaw;
    public String text;
    @BsonProperty("lonLat")     // The database uses lonLat due to legacy issues, may want to adjust at some point
    public Point     latLon;
    public String locationID;
    public String address;
    public String status;
    public String type;
    public String messageID;
    public String source;
    //    public Date   createdOn;
    public List<String> units = new ArrayList<>();
    public String incidentID;

    public Message() {

    }

    public Message(String textRaw, String text,Point latLon, String locationID, String address, String status, String type, String messageID, String source, List<String>units, String incidentID) {
        this.textRaw = textRaw;
        this.text = text;
        this.latLon = latLon;
        this.locationID = locationID;
        this.address = address;
        this.status = status;
        this.type = type;
        this.messageID = messageID;
        this.source = source;
	this.units = units;
	this.incidentID = incidentID;
    }
    
    public Message(String textRaw, String text,Double lat, Double lon, String locationID, String address, String status, String type, String messageID, String source, List<String>units, String incidentID) {
    	this(textRaw, text, new Point(new Position(lon, lat)), locationID, address, status, type, messageID, source, units, incidentID);
   }
}
