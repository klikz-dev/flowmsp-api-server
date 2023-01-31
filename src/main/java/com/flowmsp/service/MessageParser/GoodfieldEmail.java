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
import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Position;

public class GoodfieldEmail implements MsgParser {
    private static final Logger log = LoggerFactory.getLogger(GoodfieldEmail.class);

    @Override
    public ParsedMessage Process(Customer cust, String msg, MessageService msgService) {
        ParsedMessage parsedMessage = new ParsedMessage();
        parsedMessage.ErrorFlag = 99;

        log.info("Goodfield msg");
        log.info(msg);

	/*
	  CALL: 10-50 UK;PLACE: 141 IRONS RD - NE SECTOR, CONGERVILLE [111];ID: 20191031-30611;INFO: JIM=20
	  CALL: AMBU;PLACE: EUREKA COLLEGE BEN MAJOR HALL, 722 S HENRY ST, EUREKA [711] (40.714030, -89.266122);ID: 20191018-29456;INFO:
	  CALL: AMBU;PLACE: 700 N MAIN ST, EUREKA [711] (40.730845, -89.273857);ID: 20191018-29455;INFO: APT 188-MAPLE LAWN APTS
	*/
        try {
	    var p_call = Pattern.compile("CALL: (.+)");
	    var p_addr = Pattern.compile("PLACE: (.*) \\[\\d+\\] \\((.+), (.+)\\).*");
	    var lat = (String) null ;
	    var lon = (String) null ;

            String [] messageArray = msg.split(";");
            parsedMessage.text = msg;

	    if (messageArray.length > 0)
		parsedMessage.Code = messageArray[0].trim() ;

            for (int i = 0; i < messageArray.length; i++) {
		var line = messageArray[i].trim();
		log.info(i + " " + line);

		// code
		var c = p_call.matcher(line);
		if (c.matches())
		    parsedMessage.Code = c.group(1);

		// address
		var a = p_addr.matcher(line);
		if (a.matches()) {
		    parsedMessage.Address = a.group(1) + ", IL" ;
		    log.info(i + " found address " + parsedMessage.Address);
		    lat = a.group(2);
		    lon = a.group(3);

		    log.info("lat=" + lat);
		    log.info("lon=" + lon);
		}
	    }
	    
	    log.info("Code: " + parsedMessage.Code);
	    log.info("Address: " + parsedMessage.Address);

	    // prefer lat/lon if available
	    if (lat != null) {
		log.info("LAT: " + lat + " LON: " + lon);
	   
		var latMsg = Double.parseDouble(lat);
		var lonMsg = Double.parseDouble(lon);

		Point latLon = new Point(new Position(lonMsg, latMsg));
		Location location = null;

		if (latLon != null) {
		    parsedMessage.messageLatLon = latLon;
		    location = msgService.getLocationByPoint(latLon);
		}

		if (location != null)
		    parsedMessage.location = location;
	    }
	    else {
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
	    }

            parsedMessage.ErrorFlag = 0;
        } catch (Exception ex) {
	    // log.error(ex);
            parsedMessage.ErrorFlag = 1;
        }

        return parsedMessage;
    }
}
