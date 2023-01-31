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
Loc:1250 NEUBRECHT RD XSt:FINDLAY RD NORFOLK SOUTHER Grid:1355 Units:BTFD Rmk:ZONE 1 GENERAL FIRE ALARM...OPS2
 */

public class hinckleyfire implements MsgParser {
    private static final Logger log = LoggerFactory.getLogger(hinckleyfire.class);

    @Override
    public ParsedMessage Process(Customer cust, String msg, MessageService msgService) {
        ParsedMessage parsedMessage = new ParsedMessage();
        parsedMessage.ErrorFlag = 99;

        log.info("hinckleyfire msg");
        log.info(msg);

        String plainMsg = msg;

        if (plainMsg.contains("Msg-")) {
            plainMsg = plainMsg.substring(plainMsg.indexOf("Msg-") + "Msg-".length());
            plainMsg = plainMsg.trim();
        }

        try {
            parsedMessage.text = plainMsg;
	    String [] lines = plainMsg.split(";");

	    parsedMessage.Address = lines[0];

	    get_code(lines[1].trim(), parsedMessage);

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
		
		parsedMessage.ErrorFlag = 0;
	    }
	    
        } catch (Exception ex) {
            parsedMessage.ErrorFlag = 1;
        }

        return parsedMessage;
    }

    private int get_code(String line2, ParsedMessage pm) {
	if (line2.contains("Medical Alarm"))
	    pm.Code = "Medical Alarm";
	else if (line2.contains("Ambulance Request"))
	    pm.Code = "Ambulance Request";
	else if (line2.contains("Fire Alarm"))
	    pm.Code = "Fire Alarm";
	else
	    pm.Code = line2;

	return 0;
    }
}
