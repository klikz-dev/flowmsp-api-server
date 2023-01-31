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
import java.util.HashMap;

/*
Call type: ASSIST 
Case number: 2020-00000838 
Date/time: 2020-08-04T16:04:00 
Street: 1507 STONEWALL ST 
City: Dublin 
State: GA 
Zip: 31021 
Lat/Long: 32.533520705,-82.926393101 
Cross: LACROSS ST / S LANCASTER ST 
Notes: DSP AND ENR @ 16:04 
ON SCENE @ 16:09 
10-8 @ 16:20 
COURTESY CALL
*/
public class osagebeachfi implements MsgParser {
    private static final Logger log = LoggerFactory.getLogger(osagebeachfi.class);

    @Override
    public ParsedMessage Process(Customer cust, String msg, MessageService msgService) {
        ParsedMessage parsedMessage = new ParsedMessage();
        parsedMessage.ErrorFlag = 99;

        log.info("osagebeachfi msg");
        log.info(msg);

        try {
            String [] lines = msg.split("\n");

            parsedMessage.text = msg.replace("\r\n", " ");

	    var code_ok = get_code(lines, parsedMessage);
	    var addr_ok = get_address(lines, parsedMessage);
	    var latlon_ok = get_latlon(lines, parsedMessage, msgService);

	    log.info("Text: " + parsedMessage.text);
	    log.info("Code: " + parsedMessage.Code);
	    log.info("Address: " + parsedMessage.Address);

	    if (latlon_ok == 0) {
		log.info("using LAT/LON");
                parsedMessage.ErrorFlag = 0;
	    }
            else if (parsedMessage.Address.length() > 5) {
		log.info("using Address");
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
	    // log.error(ex);
            parsedMessage.ErrorFlag = 1;
        }

        return parsedMessage;
    }

    private int get_code(String[] lines, ParsedMessage pm) {
	var p = Pattern.compile("Category: (.*)");
	
	for (int i = 0; i < lines.length; i++) {
	    var m = p.matcher(lines[i].trim());

	    if (m.matches()) {
		log.info("found code");
		pm.Code = m.group(1).trim();
		return 0;
	    }
	}

	return 1;
    }

    private int get_latlon(String[] lines, ParsedMessage pm, MessageService ms) {
	var p = Pattern.compile("Lat/Long: (\\d+\\.\\d+),([-+]\\d+\\.\\d+)") ;
	var lat = "";
	var lon = "";

	for (int i = 0; i < lines.length; i++) {
	    var line = lines[i].trim();
	    var m = p.matcher(line);

	    if (m.matches()) {
		lat = m.group(1);
		lon = m.group(2);
	    }
	}
	
	if (lat.length() > 0 && lon.length() > 0) {
	    var dlat = Double.parseDouble(lat) ;
	    var dlon = Double.parseDouble(lon) ;
	    
	    var pos = new Position(dlon, dlat);
	    var point = new Point(pos);

	    pm.messageLatLon = point;

	    try {
		pm.location = ms.getLocationByPoint(pm.messageLatLon);
		
		log.info(pm.messageLatLon.toString());
		return 0;
	    }
	    catch (Exception e) {
		log.error("getLocationByPoint failed for " + lat + " " + lon, e);
	    }
	}

	return 1;
    }

    private int get_address(String[] lines, ParsedMessage pm) {
	var address_line = "" ;
	int j = lines.length;

	for (int i = 0; i < lines.length; i++) {
	    var line = lines[i].trim();

	    if (line.contains("Address:")) {
		log.info("found address line");

		j = i + 1;
		break;
	    }
	}

	for (; j < lines.length; j++) {
	    var line = lines[j].trim();

	    if (line.equals(""))
		break;

	    if (line.contains("Business:")) {
		log.info("end of address reached at: " + line);
		break;
	    }
	    
	    address_line += line;
	    address_line += " ";
	}
	
	// remove apartment numbers, google doesn't seem to like them
	var aptno = Pattern.compile(".*( #.+)");
	var m = aptno.matcher(address_line);

	if (m.matches()) {
	    var apt = m.group(1);
	    
	    log.info("removing apartment number: " + apt);
	    log.info("before: " + address_line);
	    address_line = address_line.replace(apt, "");
	    log.info("after: " + address_line);
	}

	pm.Address = address_line.trim() ;

	return 0;
    }
}
