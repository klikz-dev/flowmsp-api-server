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

public class KankakeeEmail implements MsgParser {
    private static final Logger log = LoggerFactory.getLogger(KankakeeEmail.class);

    @Override
    public ParsedMessage Process(Customer cust, String msg, MessageService msgService) {
    ParsedMessage parsedMessage = new ParsedMessage();
    parsedMessage.ErrorFlag = 99;
    try {
        String plainMsg = msg;
        // Subject-Automatic R&R Notification: ALARM:FIRE Msg-ALARM:FIRE 455 CONCORD PL, Bourbonnais 6/18/2018 00:55:55 [2018-00001685 KB143]
        plainMsg = plainMsg.replace("+", " ");
        plainMsg = plainMsg.replace("%3A", ":");
        plainMsg = plainMsg.replace("%2F", "/");
        plainMsg = plainMsg.replace("%2C", ",");
        plainMsg = plainMsg.replace("%2E", ".");
        plainMsg = plainMsg.replace("\r\n", " ");
        plainMsg = plainMsg.replace("\n", " ");
        plainMsg = msgService.removeHyperlink(plainMsg);
        
        parsedMessage.text = get_text(plainMsg);
	parsedMessage.Code = get_code(plainMsg);
	parsedMessage.Address = get_address(cust, plainMsg);

	log.info("Code: " + parsedMessage.Code);
	log.info("Address: " + parsedMessage.Address);
	
	if (parsedMessage.Address.contains("LAT: ")) {
	    log.info("will use LAT/LON");

	    get_latlon(plainMsg, parsedMessage, msgService);
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
	var p = Pattern.compile(".* Msg-(.*)");
	var m = p.matcher(msg);

	if (m.matches())
	    return m.group(1).trim();
	else
	    return msg;
    }

    private String get_address(Customer cust, String msg) {
	var p = Pattern.compile(".*Msg-(.*?) (.*) (\\d+/\\d+/\\d{4}) (\\d{2}:\\d{2}:\\d{2}) .*");
	var m = p.matcher(msg);
	var addr = "";

	// if no address matched, return
	if (m.matches())
	    addr = m.group(2).trim();
	else
	    return addr;

	// append state
	if ((cust.address.state.length() > 0) && (addr.contains("LAT: ") == false))
	    addr += (", " + cust.address.state);

	return addr;
    }

    private int get_latlon(String msg, ParsedMessage pm, MessageService ms) {
	var p = Pattern.compile(".*LAT: (\\d+\\.\\d+); LON: (-?\\d+\\.\\d+).*");
	var m = p.matcher(msg);

	if (m.matches()) {
	    var lat = Double.parseDouble(m.group(1));
	    var lon = Double.parseDouble(m.group(2));

	    log.info("lat=" + lat);
	    log.info("lon=" + lon);

	    var pos = new Position(lon, lat);
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

    private String get_code(String msg) {
	var p = Pattern.compile("Subject-Automatic R&R Notification: (.*) Msg-.*");
	var m = p.matcher(msg);

	if (m.matches())
	    return m.group(1).trim();
	else
	    return "";
    }
}
