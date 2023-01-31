package com.flowmsp.service.MessageParser;

import com.flowmsp.domain.customer.Customer;
import com.flowmsp.domain.location.Location;
import com.flowmsp.service.PolygonUtil;
import com.flowmsp.service.Message.MessageService;
import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Position;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OgleEmail implements MsgParser {
    private static final Logger log = LoggerFactory.getLogger(OgleEmail.class);

    @Override
    public ParsedMessage Process(Customer cust, String msg, MessageService msgService) {
	ParsedMessage parsedMessage = new ParsedMessage();
	parsedMessage.ErrorFlag = 99;
	try {
	    // Ambulance Call at 319 S Main St, Creston, 60113, 41.927908, -088.964709
	    //Ambulance Call at 2525 E Ash Rd, Byron, 61010
	    parsedMessage.Code = get_code(msg);
	    parsedMessage.text = get_text(msg);
	    parsedMessage.Address = get_address(msg);

	    var has_latlon = get_latlon(parsedMessage.Address, parsedMessage, msgService);

	    log.info("Code: " + parsedMessage.Code);
	    log.info("Address: " + parsedMessage.Address);
	    
	    if (has_latlon == 0) {
		log.info("using LAT/LON");
                parsedMessage.ErrorFlag = 0;
	    }

	    //Address should be of at-least 5 characters
	    else if (parsedMessage.Address.length() > 5) {
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
	    parsedMessage.ErrorFlag = 0;
	} catch (Exception ex) {
	    parsedMessage.ErrorFlag = 1;
	}
	return parsedMessage;
    }

    private String get_text(String msg) {
	var p = Pattern.compile("Subject-(.*) Msg-(.*)");
	var m = p.matcher(msg);

	if (m.matches())
	    return m.group(1).trim() + m.group(2).trim();
	else
	    return msg;
    }

    private String get_code(String msg) {
	var p = Pattern.compile("Subject-(.*) at.* Msg-.*");
	var m = p.matcher(msg);

	if (m.matches())
	    return m.group(1).trim();
	else
	    return "";
    }

    private String get_address(String msg) {
	var p = Pattern.compile("Subject-.* at (.*) Msg-.*");
	var m = p.matcher(msg);
	var address = "";

	if (m.matches())
	    address = m.group(1).trim();

	// remove apartment numbers, google doesn't seem to like them
	var aptno = Pattern.compile(".*( #.+)");
	var ma = aptno.matcher(address);
    
	if (ma.matches()) {
	    var apt = ma.group(1);
	
	    log.info("removing apartment number: " + apt);
	    log.info("before: " + address);
	    address = address.replace(apt, "");
	    log.info("after: " + address);
	}

	return address;
    }

    private int get_latlon(String msg, ParsedMessage pm, MessageService ms) {
	var p = Pattern.compile(".*, (\\d+\\.\\d+), (-?\\d+\\.\\d+)") ;
	var m = p.matcher(msg);
	var lat = "";
	var lon = "";

	if (m.matches()) {
	    lat = m.group(1);
	    lon = m.group(2);
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
}
