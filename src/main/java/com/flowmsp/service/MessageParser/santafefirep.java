package com.flowmsp.service.MessageParser;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import com.flowmsp.domain.customer.Customer;
import com.flowmsp.domain.location.Location;
import com.flowmsp.service.PolygonUtil;
import com.flowmsp.service.Message.MessageService;
import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Position;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;

/*
DISPATCH:BAFD:0BAFD - 10/14 22:46 - BAFD:20-000050 9914 EMERGENCY AID SERVICES

150 HICKORY,CARLYLE//BAFD:0BAFD//53 Y/O F, C/DIFF BREATHING, NAUSEA, DEHYDRATED
 */

public class santafefirep implements MsgParser {
    private static final Logger log = LoggerFactory.getLogger(santafefirep.class);

    @Override
    public ParsedMessage Process(Customer cust, String msg, MessageService msgService) {
        ParsedMessage parsedMessage = new ParsedMessage();
        parsedMessage.ErrorFlag = 99;

        log.info("santafefirep msg");
        log.info(msg);

        try {
	    String[] lines = msg.split("\r\n");

	    parsedMessage.Code = get_code(lines);
	    parsedMessage.text = get_text(lines);
	    parsedMessage.Address = get_address(lines);

	    log.info("Code: " + parsedMessage.Code);
	    log.info("Address: " + parsedMessage.Address);
	    
	    //Address should be of at-least 5 characters
	    if (parsedMessage.Address.length() > 5) {
		Location location = msgService.getLocationByAddress(parsedMessage.Address);
		Point latLon = null;
		if (location == null) {
		    latLon = msgService.getPointByGoogleMaps(cust, parsedMessage.Address);
		    if (latLon != null) {
			location = msgService.getLocationByPoint(latLon);
		    }
		} else {
		    // this should be the centre of location polygon
		    latLon = PolygonUtil.getCenter(location.geoOutline);
		}
		if (location != null) {
		    parsedMessage.location = location;
		}
		if (latLon != null) {
		    parsedMessage.messageLatLon = latLon;
		}
	    }

	    parsedMessage.ErrorFlag = 0;
	} catch (Exception ex) {
            parsedMessage.ErrorFlag = 1;
        }
    
        return parsedMessage;
    }

    private String get_text(String[] lines) {
	return String.join(" ", lines);
    }

    private String get_code(String[] lines) {
	var p = Pattern.compile(".*\\d+-\\d+ \\d{4} (.*)");
	var m = p.matcher(lines[0]);

	if (m.matches())
	    return m.group(1);
	else
	    return "";
    }

    private String get_address(String[] lines) {
	var p = Pattern.compile("(.*),(.*)//.*//.*");
	var m = p.matcher(lines[2]);

	if (m.matches())
	    return m.group(1) + ", " + m.group(2) + " IL";
	else
	    return "";
    }
}
