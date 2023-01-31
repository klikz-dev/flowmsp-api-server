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

public class RockComEmail implements MsgParser {
    private static final Logger log = LoggerFactory.getLogger(RockComEmail.class);

    @Override
    public ParsedMessage Process(Customer cust, String msg, MessageService msgService) {
	ParsedMessage parsedMessage = new ParsedMessage();
	parsedMessage.ErrorFlag = 99;

        log.info("RockCom msg");
        log.info(msg);

	try {
	    //STILL/BATTERY VICTIM/PD ON SCENE maps.google.com/maps?q=823+EVANS+AVE+MACHESNEY+PARK+IL+61115 W SSE 4.9/6.6 T58 Mostly cloudy
	    // as of 12/19/2019
	    // NPFD:320 E GREENVIEW (Fire) GENERAL/2ND CALL/NATURAL GAS ODOR OUTSIDE maps.google.com/maps?q=N42.3409+W89.05734 W SW 7.7/9.9 T40 Cloudy
	    String plainMsg = msg;
	    int idx = -1;

	    idx = plainMsg.indexOf("Msg-");
	    if (idx >= 0) {
		plainMsg = plainMsg.replace("Msg-", "");
		plainMsg = plainMsg.substring(idx);
		plainMsg = plainMsg.trim();
	    }

	    var latlon_rc = getLatLon(plainMsg, parsedMessage);
	    log.info("latlon_rc=" + latlon_rc);

	    //Address should be of at-least 5 characters
	    if (parsedMessage.Address.length() > 5) {
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
	    parsedMessage.ErrorFlag = 1;
	}
	return parsedMessage;
    }

    // assumes north latitude and west longitude, which is probably bad
    public int getLatLon(String msg, ParsedMessage pm) {
	var p1 = Pattern.compile(".* \\((.*)\\) .* maps.google.com/maps\\?q=(.*?) .*");
        var m1 = p1.matcher(msg);

	if (m1.matches()) {
	    log.info("group1=" + m1.group(1));
	    log.info("group2=" + m1.group(2));

	    pm.Address = m1.group(2).replace("+", " ");
	    pm.Code = m1.group(1);

	    return 0;
	}
	
	var p2 = Pattern.compile(".*\\d+-\\d+ (.*)");
        var m2 = p2.matcher(msg);

	if (m2.matches()) {
	    log.info("group1=" + m2.group(1));

	    pm.Address = m2.group(1);
	    pm.Code = "";

	    return 0;
	}

	log.info("getLatLon, pattern match failed");
	return 1;
    }
}
