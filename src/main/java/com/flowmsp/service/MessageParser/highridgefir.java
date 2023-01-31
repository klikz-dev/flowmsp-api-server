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
433 THOREAU TRL, HIGHRIDGE

ALMMED : MEDICAL ALARM
Cross Streets : WALDEN LN 
http://maps.google.com/maps?q=+38.48351%2C-90.50735
Comments:

6426 6827 
 */

public class highridgefir implements MsgParser {
    private static final Logger log = LoggerFactory.getLogger(highridgefir.class);

    @Override
    public ParsedMessage Process(Customer cust, String msg, MessageService msgService) {
        ParsedMessage parsedMessage = new ParsedMessage();
        parsedMessage.ErrorFlag = 99;

        log.info("highridgefir msg");
        log.info(msg);

        try {
            String [] lines = msg.split("\r\n");

	    parsedMessage.Code = get_code(lines);
	    parsedMessage.text = get_text(lines);
	    parsedMessage.Address = get_address(lines);

	    var has_latlon = get_latlon(lines, parsedMessage, msgService);

	    log.info("Code: " + parsedMessage.Code);
	    log.info("Address: " + parsedMessage.Address);
	    
	    if (has_latlon == 0) {
		log.info("using LAT/LON");
                parsedMessage.ErrorFlag = 0;
	    }

	    //Address should be of at-least 5 characters
	    else if (parsedMessage.Address.length() > 5) {
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
	var p = Pattern.compile("Cross Streets : (.*)");
	var code = "";

	for (int i = 0; i < lines.length; i++) {
	    var line = lines[i].trim();
	    var m = p.matcher(line);

	    // we want the previous line
	    if (m.matches()) {
		if (code.contains(":"))
		    return code.split(":")[1].trim();
		else
		    return code.trim();
	    }

	    code = line;
	}
	
	return "";
    }

    private String get_address(String[] lines) {
	if (lines.length > 0)
	    return lines[0];
	else
	    return "";
    }

    private int get_latlon(String[] lines, ParsedMessage pm, MessageService ms) {
	var p = Pattern.compile("http://maps.google.com/maps\\?q=([-+]?\\d+\\.\\d+)%2C(-?\\d+\\.\\d+)");
	var lat = "";
	var lon = "";

	for (int i = 0; i < lines.length; i++) {
	    var line = lines[i].trim();
	    var m = p.matcher(line);

	    if (m.matches()) {
		lat = m.group(1);
		lon = m.group(2);
	    }
	}
	
	log.info("lat=" + lat);
	log.info("lon=" + lon);

	if (lat.length() > 0 && lon.length() > 0) {
	    var dlat = Double.parseDouble(lat) ;
	    var dlon = Double.parseDouble(lon) ;
	    
	    var pos = new Position(dlon, dlat);
	    var point = new Point(pos);

	    pm.messageLatLon = point;

	    try {
		pm.location = ms.getLocationByPoint(pm.messageLatLon);
		
		log.info(pm.messageLatLon.toString());
		return 0;
	    }
	    catch (Exception e) {
		log.error("getLocationByPoint failed for " + lat + " " + lon, e);
	    }
	}

	return 1;
    }
}
