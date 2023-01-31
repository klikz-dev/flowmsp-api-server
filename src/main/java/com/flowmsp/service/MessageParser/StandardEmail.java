package com.flowmsp.service.MessageParser;

import com.flowmsp.domain.customer.Customer;
import com.flowmsp.domain.location.Location;
import com.flowmsp.service.PolygonUtil;
import com.flowmsp.service.Message.MessageService;
import com.mongodb.client.model.geojson.Point;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class StandardEmail implements MsgParser {
    @Override
    public ParsedMessage Process(Customer cust, String msg, MessageService msgService) {
	ParsedMessage parsedMessage = new ParsedMessage();
	parsedMessage.ErrorFlag = 99;
	try {
	    String plainMsg = msg;
	    // CALL: FI - Fall Injury ADDR: 15006 Parkside Ave CITY: Oak Forest UNITS: A39,E39
	    plainMsg = plainMsg.replace("+", " ");
	    plainMsg = plainMsg.replace("%3A", ":");
	    plainMsg = plainMsg.replace("%2F", "/");
	    plainMsg = plainMsg.replace("%2C", ",");
	    plainMsg = plainMsg.replace("%2E", ".");
	    plainMsg = plainMsg.replace("\r\n", " ");
	    plainMsg = plainMsg.replace("\n", " ");
	    plainMsg = msgService.removeHyperlink(plainMsg);
	    
	    
	    //Subject is not necessary in this.
	    int idx = -1;
	    if (plainMsg.startsWith("Subject-")) {
		idx = plainMsg.indexOf("Msg-");
		if (idx >= 0) {
		    plainMsg = plainMsg.substring(idx + 4);
		    plainMsg = plainMsg.trim();
		}
	    }
	    
	    parsedMessage.text = plainMsg;
	    parsedMessage.Code = get_field("CALL", plainMsg);
	    parsedMessage.Address = get_address(plainMsg, cust);

	    get_units(plainMsg, parsedMessage);

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

    private String get_field(String field, String msg) {
	var p = Pattern.compile(".*" + field + ": (.*?)( [A-Z]+:.*|$)");
	var m = p.matcher(msg);

	if (m.matches())
	    return m.group(1);
	else
	    return "";
    }

    private String get_address(String msg, Customer cust) {
	var addr = get_field("ADDR", msg);
	var city = get_field("CITY", msg);

	city = city.replace("Cal Cty", "Calument City");

	if (city.length() > 0)
	    addr += (", " + city);

	// append state
	if (cust.address.state.length() > 0)
	    addr += (" " + cust.address.state);

	return addr;
    }

    private int get_units(String msg, ParsedMessage pm) {
	var unit = get_field("UNIT", msg);

	if (unit.length() > 0) {
	    var units = unit.split(",");

	    for (String u : units)
		pm.units.add(u);

	    return 0;
	}
	else
	    return 1;
    }
}
