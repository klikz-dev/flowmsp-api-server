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
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

/*
CAD #1469684:
ALARM
470 TOM BALL RD, TAZEWELL [379] (36.434669, -83.571917)
BUILDING 200;  ZONE 63
*/
public class mcleanfirede implements MsgParser {
    private static final Logger log = LoggerFactory.getLogger(mcleanfirede.class);

    @Override
    public ParsedMessage Process(Customer cust, String msg, MessageService msgService) {
        ParsedMessage parsedMessage = new ParsedMessage();
        parsedMessage.ErrorFlag = 99;

        log.info("mcleanfirede msg");
        log.info(msg);

        try {
	    var idx = msg.indexOf("Msg-");
	    var plainMsg = "";

	    if (idx >= 0)
		plainMsg = msg.substring(idx + 4);
	    else
		plainMsg = msg;

            String [] lines = msg.split(";");

            parsedMessage.text = get_text(lines);

	    var code_ok = get_code(lines, parsedMessage);
	    var addr_ok = get_address(lines, parsedMessage);
	    var latlon_ok = get_latlon(lines, parsedMessage, msgService);

	    log.info("Code: " + parsedMessage.Code);
	    log.info("Address: " + parsedMessage.Address);

            if (parsedMessage.Address.length() > 5) {
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

	    else if (latlon_ok == 0) {
		log.info("using LAT/LON");
                parsedMessage.ErrorFlag = 0;
	    }
        } catch (Exception ex) {
	    // log.error(ex);
            parsedMessage.ErrorFlag = 1;
        }

        return parsedMessage;
    }

    private String get_text(String[] lines) {
	var p = Pattern.compile("^(UNIT|ALL UNITS|CALL|PLACE|ADDR|BLDG|APT|CITY|XSTREETS|TIME):(.*)");
	var s = "";
	var dt = new Date();
	var df = new SimpleDateFormat("yyyy-MM-dd");

	df.setTimeZone(TimeZone.getTimeZone("US/Central"));

	for (int i = 0; i < lines.length; i++) {
	    var line = lines[i].trim();
	    var m = p.matcher(line);

	    if (m.matches()) {
		if (m.group(1).equals("TIME")) {
		    var ymd = df.format(dt);
		    var d = "TIME:" + ymd + " " + m.group(2);
		    
		    s += d;
		}
		else {
		    s += line;
		}

		s += " ";
		
	    }
	}

	return s ;
    }

    private int get_code(String[] lines, ParsedMessage pm) {
	var p = Pattern.compile("CALL:(.*)");
	
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
	var plat = Pattern.compile("LAT:(\\d+)");
	var plon = Pattern.compile("LON:(\\d+)");
	var lat = "";
	var lon = "";

	for (int i = 0; i < lines.length; i++) {
	    var line = lines[i].trim();
	    var mlat = plat.matcher(line);
	    var mlon = plon.matcher(line);

	    if (mlat.matches())
		lat = mlat.group(1);

	    if (mlon.matches())
		lon = mlon.group(1);
	}
	
	if (lat.length() > 0 && lon.length() > 0) {
	    var dlat = Double.parseDouble(lat) / 1000000;
	    var dlon = Double.parseDouble(lon) / 1000000 * -1 ;
	    
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
	var a = Pattern.compile("ADDR:(.*)");
	var c = Pattern.compile("CITY:(.*)");
	var addr = "";
	var city = "";

	for (int i = 0; i < lines.length; i++) {
	    var line = lines[i].trim();

	    var am = a.matcher(line);

	    if (am.matches()) {
		addr = am.group(1);
		log.info("found address line: " + addr);
	    }

	    var cm = c.matcher(line);

	    if (cm.matches()) {
		city = cm.group(1);
		log.info("found city line: " + city);
	    }
	}

	pm.Address = addr.trim();

	if (city.length() > 0) {
	    pm.Address += ", ";
	    pm.Address += city;
	}

	pm.Address += ", IL" ;

	return 0;
    }
}
