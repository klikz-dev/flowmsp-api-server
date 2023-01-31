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

public class marlowvolfir implements MsgParser {
    private static final Logger log = LoggerFactory.getLogger(marlowvolfir.class);

    @Override
    public ParsedMessage Process(Customer cust, String msg, MessageService msgService) {
        ParsedMessage parsedMessage = new ParsedMessage();
        parsedMessage.ErrorFlag = 99;

        log.info("marlowvolfir msg");
        log.info(msg);

        try {
            String [] lines = msg.split("\n");

            parsedMessage.text = get_text(lines) ;

	    var code_ok = get_code(lines, parsedMessage);
	    var addr_ok = get_address(lines, parsedMessage);
	    var latlon_ok = 1 ;

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
	var p = Pattern.compile("This message is confidential.*");
	var s = "";

	for (int i = 0; i < lines.length; i++) {
	    var m = p.matcher(lines[i].trim());

	    if (m.matches())
		break;

	    s += lines[i].trim();
	    s += " ";
	}

	return s ;
    }

    private int get_code(String[] lines, ParsedMessage pm) {
	if (lines.length > 1) {
	    pm.Code = lines[1].trim();
	    return 0;
	}
	else {
	    pm.Code = "";
	    return 1;
	}
    }

    private int get_address(String[] lines, ParsedMessage pm) {
	if (lines.length > 2) {
	    pm.Address = lines[2].trim();
	    return 0;
	}
	else {
	    pm.Address = "";
	    return 1;
	}
    }
}
