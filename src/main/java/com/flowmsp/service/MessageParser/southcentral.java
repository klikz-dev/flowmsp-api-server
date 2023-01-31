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
FWD:From Active911
ID:2048306
DATE:1601361592
DST:OH
INFO:- 10 YOF ASTHMA ATTACK /
DATE: 09/29/20
TIME: 02:39:04
ZIP: 44606
ADDR:9606 HARRISON RD
CALL:BREATHING DIFFICULTY
CITY:Apple Creek
Active911#:237630418
*/
public class southcentral implements MsgParser {
    private static final Logger log = LoggerFactory.getLogger(southcentral.class);

    @Override
    public ParsedMessage Process(Customer cust, String msg, MessageService msgService) {
        ParsedMessage parsedMessage = new ParsedMessage();
        parsedMessage.ErrorFlag = 99;

        log.info("southcentral msg");
        log.info(msg);

        try {
            String [] lines = msg.split("\r\n");

            parsedMessage.text = msg.replace("\r\n", " ");

	    var code_ok = get_code(lines, parsedMessage);
	    var addr_ok = get_address(lines, parsedMessage);

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
    private int get_code(String[] lines, ParsedMessage pm) {
	pm.Code = get_matching_line(lines, "CALL:(.*)");

	return 0;
    }

    private int get_address(String[] lines, ParsedMessage pm) {
	var state = get_matching_line(lines, "DST:(.*)");
	var city = get_matching_line(lines, "CITY:(.*)");
	var addr = get_matching_line(lines, "ADDR:(.*)");
	var zip = get_matching_line(lines, "ZIP:(.*)");

	pm.Address = (addr + ", " + city + ", " + state + "  " + zip);

	return 0;
    }
}
