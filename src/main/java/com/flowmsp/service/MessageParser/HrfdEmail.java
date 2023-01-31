package com.flowmsp.service.MessageParser;

import com.flowmsp.domain.customer.Customer;
import com.flowmsp.domain.location.Location;
import com.flowmsp.service.PolygonUtil;
import com.flowmsp.service.Message.MessageService;
import com.mongodb.client.model.geojson.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class HrfdEmail implements MsgParser {
    private static final Logger log = LoggerFactory.getLogger(HrfdEmail.class);

    @Override
    public ParsedMessage Process(Customer cust, String msg, MessageService msgService) {
	ParsedMessage parsedMessage = new ParsedMessage();
	parsedMessage.ErrorFlag = 99;
	    
	try {
	    String plainMsg = msg;
	    log.info("Hrfd msg");
	    log.info(msg);
	    var p = Pattern.compile("Subject-HRFD:(.*) \\((.+)\\) Msg-(.*)") ;
	    var m = p.matcher(plainMsg);
	    var address = "";

	    if (m.matches()) {
		address = m.group(1);
		parsedMessage.Code = m.group(2);
		parsedMessage.text = m.group(1) + " " + m.group(2) + " " + m.group(3);

		// remove apartment numbers, google doesn't seem to like them
		var aptno = Pattern.compile(".*( #.+)");
		var a = aptno.matcher(address);

		if (a.matches()) {
		    var apt = a.group(1);

		    log.info("removing apartment number: " + apt);
		    log.info("before: " + address);
		    address = address.replace(apt, "");
		    log.info("after: " + address);
		}

		parsedMessage.Address = address + ", Roscoe, IL";

		log.info("Address: " + parsedMessage.Address);
	    }
	    else {
		parsedMessage.text = plainMsg;
	    }

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
}
