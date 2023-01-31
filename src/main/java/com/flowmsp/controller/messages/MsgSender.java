package com.flowmsp.controller.messages;

import org.bson.codecs.pojo.annotations.BsonProperty;

import com.flowmsp.domain.Address;
import com.flowmsp.domain.location.Location;
import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Position;
import java.util.List;

/**
 * Common class for address information, intended to be embedded in other objects which
 * have address information.
 */
public class MsgSender {
    public String 	id;
    public Long 	sequence;
    public String customerID;
    public String text;
    public String textRaw;
    public String address;
    public String status;
    public String type;
    public String locationID;
    @BsonProperty("lonLat")     // The database uses lonLat due to legacy issues, may want to adjust at some point
    public Point     latLon;
    public List<String> units;
    public String incidentID;
    public Boolean isInUnitFilter;

    public MsgSender() {

    }

    public MsgSender(Long sequence, String customerID, String text, String textRaw, String address, String status, String type, String locationID, Point latLon) {
        this.sequence = sequence;
        this.customerID = customerID;
        this.text = text;
        this.textRaw = textRaw;
        this.address = address;
        this.status = status;
        this.type = type;
        this.locationID = locationID;
        this.latLon = latLon;
    }

    public MsgSender copy() {
        MsgSender msgSender = new MsgSender();
        msgSender.id = this.id;
        msgSender.sequence = this.sequence;
        msgSender.customerID = this.customerID;
        msgSender.text = this.text;
        msgSender.textRaw = this.textRaw;
        msgSender.address = this.address;
        msgSender.status = this.status;
        msgSender.type = this.type;
        msgSender.locationID = this.locationID;
        msgSender.latLon = this.latLon;
        msgSender.units = this.units;
        msgSender.incidentID = this.incidentID;
        msgSender.isInUnitFilter = this.isInUnitFilter;
        return msgSender;
    }

    public MsgSender(Long sequence, String customerID, String text, String textRaw, String address, String status, String type, String locationID, Double lat, Double lon) {
        this(sequence, customerID, text, textRaw, address, status, type, locationID, new Point(new Position(lon, lat)));
    }
}
