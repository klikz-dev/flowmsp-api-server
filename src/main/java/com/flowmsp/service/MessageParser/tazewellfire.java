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
public class tazewellfire implements MsgParser {
    private static final Logger log = LoggerFactory.getLogger(tazewellfire.class);

    @Override
    public ParsedMessage Process(Customer cust, String msg, MessageService msgService) {
        ParsedMessage parsedMessage = new ParsedMessage();
        parsedMessage.ErrorFlag = 99;

        log.info("tazewellfire msg");
        log.info(msg);

        try {
            String [] lines = msg.split("\r\n");

            parsedMessage.text = String.join(" ", lines);

	    var code_ok = get_code(lines, parsedMessage);
	    var addr_ok = get_address(lines, parsedMessage, msgService);

	    log.info("Code: " + parsedMessage.Code);
	    log.info("Address: " + parsedMessage.Address);

	    if (code_ok == 0 && addr_ok == 0) {
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
	var p = Pattern.compile("CAD #.*");
	
	for (int i = 0; i < lines.length; i++) {
	    var m = p.matcher(lines[i].trim());

	    // code is on the NEXT line
	    if (m.matches()) {
		if ((i + 1) < lines.length) {
		    log.info("found code");
		    pm.Code = lines[i+1].trim();

		    return 0;
		}
	    }
	}

	return 1;
    }

    private int get_address(String[] lines, ParsedMessage pm, MessageService ms) {
	var p = Pattern.compile("(.*) \\[\\d+\\] \\((.*), (.*)\\)");
	var addr = "";

	for (int i = 0; i < lines.length; i++) {
	    var line = lines[i].trim();
	    var m = p.matcher(line);

	    if (m.matches()) {
		log.info("found address line: ");

		addr = m.group(1);

		log.info("group2=" + m.group(2));
		log.info("group3=" + m.group(3));
		var lat = Double.parseDouble(m.group(2));
		var lon = Double.parseDouble(m.group(3));
		var pos = new Position(lon, lat);
		var point = new Point(pos);

		pm.messageLatLon = point;

		try {
		    pm.location = ms.getLocationByPoint(pm.messageLatLon);
		}
		catch (Exception e) {
		    log.error("getLocationByPoint failed for " + lat + " " + lon, e);
		}

		log.info(pm.messageLatLon.toString());

		pm.Address = addr.trim();
		return 0;
	    }
	}

	pm.Address = addr.trim();

	return 1;
    }
}
