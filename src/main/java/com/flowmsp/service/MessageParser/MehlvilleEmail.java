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

/*
29 MVA Rescue Per PD ATHagemann Rd / Mooney Ln BUS: XST: TAC:CC911 SO - FTAC 20 Mehlville FPD 1740,7415,1737,RCDN1,1756,17DN
 */

public class MehlvilleEmail implements MsgParser {
    private static final Logger log = LoggerFactory.getLogger(MehlvilleEmail.class);

    @Override
    public ParsedMessage Process(Customer cust, String msg, MessageService msgService) {
        ParsedMessage parsedMessage = new ParsedMessage();
        parsedMessage.ErrorFlag = 99;

        log.info("msg");
        log.info(msg);

        String plainMsg = msg;

        if (plainMsg.contains("Msg-")) {
            plainMsg = plainMsg.substring(plainMsg.indexOf("Msg-") + "Msg-".length());
            plainMsg = plainMsg.trim();
        }

        try {
            parsedMessage.text = plainMsg;

	    parsedMessage.Code = get_code(plainMsg);
	    parsedMessage.Address = get_address(cust, plainMsg);

	    get_units(plainMsg, parsedMessage);

	    log.info("code: <" + parsedMessage.Code + ">");
	    log.info("addr: <" + parsedMessage.Address + ">");
	    log.info("units: <" + parsedMessage.units + ">");

	    //Address should be of at-least 5 characters
	    if (parsedMessage.Address.length() > 5) {
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

    private int get_units(String msg, ParsedMessage pm) {
	var p = Pattern.compile(".* +\\d{2} +([\\d,A-Z]+) +(\\d+) +(\\d+)");
	var m = p.matcher(msg);

	if (m.matches()) {
	    log.info("units match: " + m.group(1));
	    var units = m.group(1).split(",");

	    for (String u : units) {
		log.info("u=" + u);
		pm.units.add(u);
	    }

	    return 0;
	}

	return 1;
    }

    private String get_address(Customer cust, String msg) {
	var p = Pattern.compile(".*?AT:(.*?)(BUS|XST|TAC):.*");
	var m = p.matcher(msg);
	var addr = "";

	// if no address matched, return
	if (m.matches())
	    addr = m.group(1).trim();
	else
	    return addr;

	// append city
	if (cust.address.city.length() > 0)
	    addr += (", " + cust.address.city);

	// append state
	if (cust.address.state.length() > 0)
	    addr += (", " + cust.address.state);

	return addr;
    }

    private String get_code (String msg) {
	var p = Pattern.compile("(.*)AT:.*");
	var m = p.matcher(msg);
	
	if (m.matches())
	    return m.group(1).trim();
	else
	    return "";
    }
}
