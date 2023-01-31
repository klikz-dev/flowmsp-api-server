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
Alarm Fire: SOL-E-MAR SUN & SEA DRIVE *BLDG A*   FIRE ALARM ACTIVATION
*/
public class dartmouthfir2 implements MsgParser {
    private static final Logger log = LoggerFactory.getLogger(dartmouthfir2.class);

    @Override
    public ParsedMessage Process(Customer cust, String msg, MessageService msgService) {
        ParsedMessage parsedMessage = new ParsedMessage();
        parsedMessage.ErrorFlag = 99;

        log.info("dartmouthfir2 msg");
        log.info(msg);

        try {
	    var idx = msg.indexOf("Msg-");
	    var plainMsg = "";

	    if (idx >= 0)
		plainMsg = msg.substring(idx + 4);
	    else
		plainMsg = msg;

	    get_text(plainMsg, parsedMessage);
	    get_code(plainMsg, parsedMessage);
	    get_address(plainMsg, parsedMessage);
	    var latlon_ok = get_latlon(plainMsg, parsedMessage, msgService);

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

    private String get_acct(String msg) {
	var p = Pattern.compile("<(\\d+)>.*");
	var m = p.matcher(msg);

	if (m.matches()) {
	    log.info("acct=" + m.group(1));
	    return m.group(1);
	}
	else
	    return "";
    }

    private int get_code(String msg, ParsedMessage pm) {
	pm.Code = "FIRE ALARM";

	return 0;
    }

    private int get_latlon(String msg, ParsedMessage pm, MessageService ms) {
	var acct = get_acct(msg);
	var lat = 0.0;
	var lon = 0.0;

	if (msg.contains("676 DARTMOUTH")) { lat = 41.6032112; lon = -70.9404693 ; }
        else if (msg.contains("783 DARTMOUTH")) { lat = 41.5938076; lon = -70.943246 ; }
        else if (msg.contains("1 BRIDGE")) { lat = 41.5864241 ; lon = -70.9453688 ; }
        else if (msg.contains("25 RUSSELLS MILLS")) { lat = 41.6078913 ; lon = -70.94209479999999 ; }
        else if (msg.contains("KIDS INK TOO 695 DARTMOUTH STREET")) { lat = 41.6020203 ; lon = -70.9416368 ; }

	// multiple businesses
        else if (msg.contains("718 DARTMOUTH")) { lat = 41.6002567 ; lon = -70.9395862 ; }
        else if (msg.contains("674 DARTMOUTH")) { lat = 41.603248 ; lon = -70.9406032 ; }
        else if (msg.contains("Dartmouth Housing ,Sol-E-Mar Lane")) { lat = 41.5997961 ; lon = -70.93766169999999 ; }
        else if (msg.contains("208 ELM")) { lat = 41.5848457 ; lon = -70.9429482 ; }
        else if (msg.contains("728 DARTMOUTH")) { lat = 41.599848 ; lon = -70.9412588 ; }

        else if (msg.contains("548 DARTMOUTH")) { lat = 41.6134297 ; lon = -70.9375436 ; }

        else if (msg.contains("2 COVE")) { lat = 41.6098015 ; lon = -70.93491759999999 ; }
        else if (msg.contains("53 COVE")) { lat = 41.6100217 ; lon = -70.9380528 ; }
        else if (msg.contains("654 DARTMOUTH")) { lat = 41.60521780000001 ; lon = -70.9394476 ; }
        else if (msg.contains("567 DARTMOUTH")) { lat = 41.6109421 ; lon = -70.94014419999999 ; }
        else if (msg.contains("29 RUSSELLS MILLS")) { lat = 41.607503 ; lon = -70.9426514 ; }
        else if (msg.contains("746 DARTMOUTH")) { lat = 41.59810359999999 ; lon = -70.9413981 ; }
        else if (msg.contains("732 DARTMOUTH")) { lat = 41.5991252 ; lon = -70.9412124 ; }
        else if (msg.contains("8 BUSH")) { lat = 41.5928201 ; lon = -70.9476676 ; }
        else if (msg.contains("17 MIDDLE")) { lat = 41.5889597 ; lon = -70.9405158 ; }

	// multiple buildings
        else if (msg.contains("SOL-E-MAR SUN & SEA DRIVE COMMUNITY BUILDING")) { lat = 41.6017631; lon = -70.9384978 ; }

	// buildings A-F
	else if (msg.contains("SOL-E-MAR SUN & SEA DRIVE *BLDG *")) { lat = 41.6017631 ; lon = -70.9384978 ; }
        else if (msg.contains("SOL-E-MAR GARAGE EDGEWORTH")) { lat = 41.6029367 ; lon = -70.9412091 ; }

	// multiple buildings
        else if (msg.contains("SOL-E-MAR SEABREEZE DRIVE")) { lat = 41.6011898 ; lon = -70.93550119999999 ; }

        else if (msg.contains("928 DARTMOUTH")) { lat = 41.5915225 ; lon = -70.94298189999999 ; }
        else if (msg.contains("84 ROGERS")) { lat = 41.606212 ; lon = -70.9387742 ; }
        else if (msg.contains("26 COVE")) { lat = 41.61039419999999 ; lon = -70.9363477 ; }
        else if (msg.contains("247 RUSSELLS MILLS")) { lat = 41.6029456 ; lon = -70.9598847 ; }
        else if (msg.contains("274 SLOCUM")) { lat = 41.6195722 ; lon = -70.9616056 ; }
        else if (msg.contains("PARK DEPARTMENT 487 SMITH NECK ROAD")) { lat = 41.5554753; lon = -70.9543317 ; }
        else if (msg.contains("532 RUSSELLS MILLS")) { lat = 41.59773879999999; lon = -70.9798957 ; }

	// multiple businesses
        else if (msg.contains("550 RUSSELLS MILLS")) { lat = 41.5971325 ; lon = -70.9794314 ; }

        else if (msg.contains("555 BAKERVILLE")) { lat = 41.5940391 ; lon = -70.9780713 ; }
        else if (msg.contains("300 GULF")) { lat = 41.5819225 ; lon = -70.97214079999999 ; }

	if (lat != 0.0 && lon != 0.0) {
	    log.info("lat=" + lat);
	    log.info("lon=" + lon);

	    var pos = new Position(lon, lat);
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
	var p = Pattern.compile("Alarm Fire: (.*) FIRE ALARM ACTIVATION.*");
	var m = p.matcher(msg);

	if (m.matches()) {
	    log.info("address=" + m.group(1));
	    pm.Address = m.group(1);
	}

	return 0;
    }

    private int get_text(String msg, ParsedMessage pm) {
	var p = Pattern.compile("Alarm Fire: (.* FIRE ALARM ACTIVATION) .*");
	var m = p.matcher(msg);

	if (m.matches()) {
	    log.info("text=" + m.group(1));
	    pm.text = m.group(1);
	}

	return 0;
    }
}
