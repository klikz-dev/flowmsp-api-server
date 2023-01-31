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

public class sandboxEmail implements MsgParser {
    private static final Logger log = LoggerFactory.getLogger(sandboxEmail.class);

    @Override
    public ParsedMessage Process(Customer cust, String msg, MessageService msgService) {
        ParsedMessage parsedMessage = new ParsedMessage();
        parsedMessage.ErrorFlag = 99;

        log.info("sandbox msg");
        log.info(msg);

	/*
	  CALL: random population of fire alarms, odor of gas and smell of smoke
	  INFO: Auto-Traced Area
	  ADDR: 115 Franklin Turnpike
	  CITY: Mahwah
	  DST: NJ
	  ZIP: 07430
	  GPS: 41.100019,-74.146818
	 */
        try {
            String [] lines = msg.split("\n");

            parsedMessage.text = msg.replace("\n", " ");
	    parsedMessage.Code = get_matching_line(lines, "CALL: (.*)");

	    var address = get_matching_line(lines, "ADDR: (.*)");
	    var city = get_matching_line(lines, "CITY: (.*)");
	    var gps = get_matching_line(lines, "GPS: (.*)");
	    var dst = get_matching_line(lines, "DST: (.*)");
	    var zip = get_matching_line(lines, "ZIP: (.*)");
	    var units = get_matching_line(lines, "UNITS: (.*)");

	    parsedMessage.incidentID = get_matching_line(lines, "ID: (.*)");

	    get_units(units, parsedMessage);

	    if (address.length() > 0 && city.length() > 0)
		parsedMessage.Address = address + ", " + city + ", " + dst + " " + zip;
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

    private int get_units(String units, ParsedMessage pm) {
	if (units.length() == 0)
	    return 1;

	for (String u : units.split(",")) {
	    log.info("u=" + u);
	    pm.units.add(u.trim());
	}

	return 0;
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
