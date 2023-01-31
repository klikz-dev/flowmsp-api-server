package com.flowmsp.service.MessageParser;

import com.flowmsp.domain.customer.Customer;
import com.flowmsp.domain.location.Location;
import com.flowmsp.service.PolygonUtil;
import com.flowmsp.service.Message.MessageService;
import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Position;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.HashMap;

/*
CAD #1469684:
ALARM
470 TOM BALL RD, TAZEWELL [379] (36.434669, -83.571917)
BUILDING 200;  ZONE 63
*/
public class westplainsfi implements MsgParser {
    private static final Logger log = LoggerFactory.getLogger(westplainsfi.class);

    @Override
    public ParsedMessage Process(Customer cust, String msg, MessageService msgService) {
        ParsedMessage parsedMessage = new ParsedMessage();
        parsedMessage.ErrorFlag = 99;

        log.info("westplainsfi msg");
        log.info(msg);

        try {
            String [] lines = msg.split("\n");

            parsedMessage.text = msg.replace("\n", " ");

	    var code_ok = get_code(lines, parsedMessage);
	    var addr_ok = get_address(lines, parsedMessage);
	    var latlon_ok = get_latlon(lines, parsedMessage, msgService);

	    log.info("Text: " + parsedMessage.text);
	    log.info("Code: " + parsedMessage.Code);
	    log.info("Address: " + parsedMessage.Address);

	    if (latlon_ok == 0) {
		log.info("using LAT/LON");
                parsedMessage.ErrorFlag = 0;
	    }
            else if (parsedMessage.Address.length() > 5) {
		log.info("using Address");
                parsedMessage.ErrorFlag = 0;

                // Now PlainMsg has Address Only
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

        } catch (Exception ex) {
	    // log.error(ex);
            parsedMessage.ErrorFlag = 1;
        }

        return parsedMessage;
    }

    private int get_code(String[] lines, ParsedMessage pm) {
	var p = Pattern.compile("Code: (.*)");
	
	for (int i = 0; i < lines.length; i++) {
	    var m = p.matcher(lines[i].trim());

	    if (m.matches()) {
		log.info("found code");
		pm.Code = m.group(1).trim();
		return 0;
	    }
	}

	return 1;
    }

    private int get_latlon(String[] lines, ParsedMessage pm, MessageService ms) {
	var p = Pattern.compile("LAT/LON: (\\d+\\.\\d+)([-+]\\d+\\.\\d+)") ;
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

    private int get_address(String[] lines, ParsedMessage pm) {
	var a = Pattern.compile("ADDR: (.*)");

	pm.Address = "";

	for (int i = 0; i < lines.length; i++) {
	    var line = lines[i].trim();
	    var m = a.matcher(line);

	    if (m.matches()) {
		log.info("found address line: " + m.group(1));
		pm.Address = m.group(1).trim() + ", MO" ;
	    }
	}

	// remove apartment numbers, google doesn't seem to like them
	var aptno = Pattern.compile(".*( #.+)");
	var m = aptno.matcher(pm.Address);

	if (m.matches()) {
	    var apt = m.group(1);
	    
	    log.info("removing apartment number: " + apt);
	    log.info("before: " + pm.Address);
	    pm.Address = pm.Address.replace(apt, "");
	    log.info("after: " + pm.Address);
	}

	return 0;
    }
}
