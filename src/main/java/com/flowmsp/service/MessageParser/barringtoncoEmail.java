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

public class barringtoncoEmail implements MsgParser {
    private static final Logger log = LoggerFactory.getLogger(barringtoncoEmail.class);

    @Override
    public ParsedMessage Process(Customer cust, String msg, MessageService msgService) {
        ParsedMessage parsedMessage = new ParsedMessage();
        parsedMessage.ErrorFlag = 99;

        log.info("barringtonco msg");
        log.info(msg);

	/*
	  FWD:From Active911
	  ADDR:98 ALGONQUIN RD
	  ID:07182593
	  CITY:Lake County
	  UNIT:BCF37
	  INFO:BARRINGTON UNITED METHODIST CHURCH, PATIENT FELL.
	  CAD#: $BCF19001697
	  TIME: 14:43:05
	  MAP:1627C
	  DST:IL
	  CALL:AMBULANCE CALL
	  DATE:1573850589
	  GPS:42.1130189867552, -88.1750130496102
	  Active911#:193469520
	 */
        try {
            String [] messageArray = msg.split("\n");

            parsedMessage.text = msg.replace("\n", " ");
	    parsedMessage.Code = get_matching_line(messageArray, "CALL:(.*)");

	    var address = get_matching_line(messageArray, "ADDR:(.*)");
	    var city = get_matching_line(messageArray, "CITY:(.*)");
	    var gps = get_matching_line(messageArray, "GPS:(.*)");

	    if (address.length() > 0 && city.length() > 0)
		parsedMessage.Address = address + ", " + city + ", IL";
	    else
		parsedMessage.Address = "";

	    log.info("Code: " + parsedMessage.Code);
	    log.info("Address: " + parsedMessage.Address);

	    // prefer lat/lon if available
	    if (gps.length() > 0) {
		var p = Pattern.compile("(.*), (.*)");
		var m = p.matcher(gps);

		log.info("gps=" + gps);

		if (m.matches()) {
		    log.info("found a gps match");
		}
		else {
		    log.info("did not find a gps match");
		}

		var lat = m.group(1);
		var lon = m.group(2);

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
	    //log.error(ex);
            parsedMessage.ErrorFlag = 1;
        }

        return parsedMessage;
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
