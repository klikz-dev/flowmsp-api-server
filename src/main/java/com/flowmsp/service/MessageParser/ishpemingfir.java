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
CAD:FIRE ALL OTHER;MAMA MIAS;207 E PEARL;S FIRST;S SECOND;J203;ISHPEMING CITY;Event spawned from TRAFFIC PROB/VIOLATIONS. [01/02/2020 12:59:38 BURUSE] LOW CABLE LI
 */
public class ishpemingfir implements MsgParser {
    private static final Logger log = LoggerFactory.getLogger(ishpemingfir.class);

    @Override
    public ParsedMessage Process(Customer cust, String msg, MessageService msgService) {
        ParsedMessage parsedMessage = new ParsedMessage();
        parsedMessage.ErrorFlag = 99;

        log.info("ishpemingfir msg");
        log.info(msg);

        String plainMsg = msg;

        if (plainMsg.contains("Msg-")) {
            plainMsg = plainMsg.substring(plainMsg.indexOf("Msg-") + "Msg-".length());
            plainMsg = plainMsg.trim();
        }

        try {
            parsedMessage.text = plainMsg;
            String [] lines = plainMsg.split(";");
	    var addr = get_address_line(lines);

	    parsedMessage.Code = lines[0].split(":")[1];

	    if (addr.length() > 0)
		parsedMessage.Address = addr + ", Ishpeming, MI";

	    log.info("code: <" + parsedMessage.Code + ">");
	    log.info("addr: <" + parsedMessage.Address + ">");

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

    private String get_address_line(String[] lines) {
	var p = Pattern.compile("(\\d+ .*)");
	
	for (int i = 0; i < lines.length; i++) {
	    if (lines[i].equals("J203"))
		break;
		 
	    var m = p.matcher(lines[i]);

	    if (m.matches())
		return m.group(1);
	}

	return lines[1];
    }
}
