package com.flowmsp.service.MessageParser;

import com.flowmsp.domain.customer.Customer;
import com.flowmsp.domain.location.Location;
import com.flowmsp.service.PolygonUtil;
import com.flowmsp.service.Message.MessageService;
import com.mongodb.client.model.geojson.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Position;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class victoriafire implements MsgParser {
    private static final Logger log = LoggerFactory.getLogger(victoriafire.class);

    @Override
    public ParsedMessage Process(Customer cust, String msg, MessageService msgService) {
        ParsedMessage parsedMessage = new ParsedMessage();
        parsedMessage.ErrorFlag = 99;

        log.info("victoriafire msg");
        log.info(msg);

	/*
	  Agency: VICT    Victoria Fire
	  Incident Nr: 202000000023
	  Location: 8400 Gallery Pkwy, Victoria, 55386
	  Activity: F1301 Res Fire Alrm-RP-No Signs
	  Disposition: CL Cleared
	  Time reported: 01/17/2020 9:08:10 PM
	 */
        try {
            String [] messageArray = msg.split("\n");

            parsedMessage.text = get_message_text(messageArray);

	    get_code_and_address(messageArray, parsedMessage);

	    log.info("Code: " + parsedMessage.Code);
	    log.info("Address: " + parsedMessage.Address);

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
	    //log.error(ex);
            parsedMessage.ErrorFlag = 1;
        }

        return parsedMessage;
    }

    private void get_code_and_address(String[] lines, ParsedMessage pm) {
	var p = Pattern.compile("(.*?) at (.*)");
	
	for (int i = 0; i < lines.length; i++) {
	    var line = lines[i].trim();
	    var m = p.matcher(line);

	    if (m.matches()) {
		pm.Code = m.group(1);
		pm.Address = m.group(2);

		return;
	    }
	}

	pm.Code = pm.Address = "";

	return;
    }

    private String get_message_text(String[] lines) {
	var p = Pattern.compile("Time reported: .*");
	var msg = "";

	for (int i = 0; i < lines.length; i++) {
	    var line = lines[i].trim();

	    var m = p.matcher(line);

	    if (m.matches())
		break;

	    msg += (line + " ");
	}

	return msg;
    }

    private String get_matching_line(String[] lines, String pattern) {
	var p = Pattern.compile(pattern);

	for (int i = 0; i < lines.length; i++) {
	    var line = lines[i].trim();

	    var m = p.matcher(line);

	    if (m.matches()) {
		log.info(i + " found pattern " + pattern);
		return m.group(1);
	    }
	}

	return "";
    }
}
