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
public class lakesfoursea implements MsgParser {
    private static final Logger log = LoggerFactory.getLogger(lakesfoursea.class);

    @Override
    public ParsedMessage Process(Customer cust, String msg, MessageService msgService) {
        ParsedMessage parsedMessage = new ParsedMessage();
        parsedMessage.ErrorFlag = 99;

        log.info("lakesfoursea msg");
        log.info(msg);

        try {
	    String field_terminator = "";
	    var porter = porter_dispatch(msg);

	    // DISPATCH:F31D02, UNCONSCIOUS/FAINTING, 1795 FOREST LN, POT, btwn FOUR SEASONS PKY and ELM TREE LN
	    if (porter)
		field_terminator = ",";
	    else
		field_terminator = "\r\n";

            String [] lines = msg.split(field_terminator);

            parsedMessage.text = String.join(" ", lines);

	    var code_ok = get_code(lines, parsedMessage, porter);
	    var addr_ok = get_address(lines, parsedMessage, porter);

	    log.info("text: " + parsedMessage.text);
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
        } catch (Exception ex) {
	    // log.error(ex);
            parsedMessage.ErrorFlag = 1;
        }

        return parsedMessage;
    }

    private boolean porter_dispatch(String msg) {
	var p = Pattern.compile("DISPATCH:.*");
	var m = p.matcher(msg.trim());

	return m.matches();
    }

    private int get_code(String[] lines, ParsedMessage pm, boolean porter) {
	if (porter)
	    pm.Code = lines[1];
	else
	    pm.Code = lines[2];

	return 0;
    }

    private int get_address(String[] lines, ParsedMessage pm, boolean porter) {
	var addr = "";
	var city = "";

	if (porter) {
	    addr = lines[2].trim();
	    city = lines[3].trim();
	}
	else {
	    var line = lines[4].trim().split(",");

	    addr = line[0];
	    city = line[1];
	}

	pm.Address = addr + ", Crown Point, IN";
	return 0;
    }
}
