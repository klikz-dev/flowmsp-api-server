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
CAD #1469684:
ALARM
470 TOM BALL RD, TAZEWELL [379] (36.434669, -83.571917)
BUILDING 200;  ZONE 63
*/
public class ottawafirede implements MsgParser {
    private static final Logger log = LoggerFactory.getLogger(ottawafirede.class);

    @Override
    public ParsedMessage Process(Customer cust, String msg, MessageService msgService) {
        ParsedMessage parsedMessage = new ParsedMessage();
        parsedMessage.ErrorFlag = 99;

        log.info("ottawafirede msg");
        log.info(msg);

        try {
	    var idx = msg.indexOf("Msg-");
	    var plainMsg = "";

	    if (idx >= 0)
		plainMsg = msg.substring(idx + 4);
	    else
		plainMsg = msg;

            parsedMessage.text = plainMsg ;

	    var code_ok = get_code(plainMsg, parsedMessage);
	    var addr_ok = get_address(plainMsg, parsedMessage);
	    //var latlon_ok = get_latlon(plainMsg, parsedMessage, msgService);
	    var latlon_ok = 1;

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

    private int get_code(String msg, ParsedMessage pm) {
	var p = Pattern.compile("Call Type: (.*) City: .*");
	var m = p.matcher(msg);
	
	if (m.matches()) {
	    pm.Code = m.group(1).trim();
	    return 0;
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

    private int get_address(String msg, ParsedMessage pm) {
	var p = Pattern.compile(".*?City: (.*) Address: (.*)");
	var m = p.matcher(msg.trim());
	var city = "";
	var addr = "";

	if (m.matches()) {
	    city = m.group(1);
	    addr = m.group(2);

	    pm.Address = (addr + ", " + city + ", IL");
	}

	return 0;
    }
}
