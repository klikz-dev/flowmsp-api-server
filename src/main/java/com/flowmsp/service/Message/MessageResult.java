package com.flowmsp.service.Message;

import com.flowmsp.domain.customer.Customer;
import com.flowmsp.domain.location.Location;
import com.mongodb.client.model.geojson.Point;
import org.bson.codecs.pojo.annotations.BsonProperty;

import java.util.ArrayList;
import java.util.List;

public class MessageResult {
    public String messageRaw;
    public String messageOrig;
    public String smsNumber;
    public Customer customer;
    public String messageRefined;
    public String messageType;
    public String messageAddress;
    public String messageLocationID;
    public Location messageLocation;
    @BsonProperty("lonLat")     // The database uses lonLat due to legacy issues, may want to adjust at some point
    public Point messageLatLon;
    public String emailGateway;
    public int errorFlag;
    public String errorDescription;
    public String messageID;
    public List<String> units = new ArrayList<>();
    public String incidentID;
    public String SFTP_userid;

    public MessageResult() {
        errorFlag = 0;
        errorDescription = "";
    }


    public MessageResult copy() {
        MessageResult messageResult = new MessageResult();
        messageResult.messageRaw = messageRaw;
        messageResult.messageOrig = messageOrig;
        messageResult.smsNumber = smsNumber;
        messageResult.customer = customer;
        messageResult.messageRefined = messageRefined;
        messageResult.messageType = messageType;
        messageResult.messageAddress = messageAddress;
        messageResult.messageLocationID = messageLocationID;
        messageResult.messageLocation = messageLocation;
        messageResult.messageLatLon = messageLatLon;
        messageResult.emailGateway = emailGateway;
        messageResult.errorFlag = errorFlag;
        messageResult.errorDescription = errorDescription;
        messageResult.messageID = messageID;
        messageResult.units = units;
        messageResult.incidentID = incidentID;
        return messageResult;
    }

}
