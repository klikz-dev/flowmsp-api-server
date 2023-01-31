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
Agency: Utica Fire District
floMSP: 0671443f-79c4-4ff3-8f60-42af45d9e673
Incident Nr: 20-90938
Location: 715 ERICA DR 
Activity: Alarm-CO (Carbon monoxide)
Time Reported: 08/10/2020  19:05

Units Responded:  
   Dispatched:     0:0 

Blotter: 
1211 GOT CALLED AT STATION FOR CO ALARM WENT EN ROUTE
*/
public class valcom implements MsgParser {
    private static final Logger log = LoggerFactory.getLogger(valcom.class);

    @Override
    public ParsedMessage Process(Customer cust, String msg, MessageService msgService) {
        ParsedMessage parsedMessage = new ParsedMessage();
        parsedMessage.ErrorFlag = 99;

        log.info("valcom msg");
        log.info(msg);

        try {
            String [] lines = msg.split("\n");

            parsedMessage.text = get_text(lines) ;

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

    private String get_text(String[] lines) {
	var p = Pattern.compile("(Agency: |Incident Nr: |Location: |Activity: |Disposition: |Time reported: ).*");
	var s = "";

	for (int i = 0; i < lines.length; i++) {
	    var m = p.matcher(lines[i].trim());

	    if (m.matches()) {
		log.info("line matches " + lines[i].trim());
		s += lines[i].trim();
		s += " ";
	    }
	}

	return s ;
    }

    private int get_code(String[] lines, ParsedMessage pm) {
	var p = Pattern.compile("Activity: (.*)");
	
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
	var p = Pattern.compile("Lat/Long: (\\d+\\.\\d+), (-?\\d+\\.\\d+)") ;
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
	var a = Pattern.compile("Location: (.*)");
	var f = Pattern.compile("floMSP: (.*)");
	var city_state = "";

	pm.Address = "";

	for (int i = 0; i < lines.length; i++) {
	    var line = lines[i].trim();
	    var m = a.matcher(line);

	    if (m.matches()) {
		log.info("found address line: " + m.group(1));
		pm.Address = m.group(1).trim();
	    }

	    var fm = f.matcher(line);

	    // Mendota 017f4729-cc30-44b0-8b8c-3960e77a61d4
	    // Peru    55cc9aca-ab30-462e-a5ad-35f57ccde282
	    // LaSalle 3cd731a6-e428-4562-90d9-ed4e138598f3
	    // Utica   0671443f-79c4-4ff3-8f60-42af45d9e673
	    if (fm.matches()) {
		var flowmsp = fm.group(1);

		if (flowmsp.equals("017f4729-cc30-44b0-8b8c-3960e77a61d4"))
		    city_state = ", Mendota, IL" ;
		else if (flowmsp.equals("55cc9aca-ab30-462e-a5ad-35f57ccde282"))
		    city_state = ", Peru, IL" ;
		else if (flowmsp.equals("3cd731a6-e428-4562-90d9-ed4e138598f3"))
		    city_state = ", LaSalle, IL" ;
		else if (flowmsp.equals("0671443f-79c4-4ff3-8f60-42af45d9e673"))
		    city_state = ", Utica, IL" ;
	    }
	}

	// remove apartment numbers, google doesn't seem to like them
	var aptno = Pattern.compile(".*( #.+)");
	var m = aptno.matcher(pm.Address);

	if (m.matches()) {
	    var apt = m.group(1);
	    
	    log.info("removing apartment number: " + apt);
	    log.info("before: " + pm.Address);
	    pm.Address = pm.Address.replace(apt, "");
	    log.info("after: " + pm.Address);
	}

	pm.Address += city_state ;

	return 0;
    }
}
