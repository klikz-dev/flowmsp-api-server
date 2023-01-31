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
Alert: FIRE ODOR INVESTIGATION
ALRM LVL: 1, RUN CARD:
LOC:
5 S OLD STATE RD
NORWALK
44857
BTWN: N/A & GIBBS RD

CALLER:

LOC:
, Ohio

RCVD AS Officer Initiated

COM:
Case ID: rNiooj3ks
527-DISPATCH EN ROUTE TO 5 OLD STATE RD S FOR ODOR INVESTIGATION

CT:
0455 at POS 04
INCIDENT: 20-00003249
UNITS: 3912,3923
DATE/TIME: 01/22/2020 21:52:33
http://maps.google.com/maps?z=3D17&q=3Dloc:41.2519582430452+-82.59107479470=
5
 */
public class norwalkfire implements MsgParser {
    private static final Logger log = LoggerFactory.getLogger(norwalkfire.class);

    @Override
    public ParsedMessage Process(Customer cust, String msg, MessageService msgService) {
        ParsedMessage parsedMessage = new ParsedMessage();
        parsedMessage.ErrorFlag = 99;

        log.info("norwalkfire msg");
        log.info(msg);

        try {
            String [] lines = msg.split("\r\n");

            parsedMessage.text = String.join(" ", lines);

	    get_code(lines, parsedMessage);
	    get_address(lines, parsedMessage);

	    log.info("before getLatLon");
	    var latlon_rc = getLatLon(lines, parsedMessage, msgService);

	    log.info("Code: " + parsedMessage.Code);
	    log.info("Address: " + parsedMessage.Address);

            if (latlon_rc == 0) {
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

    private int getLatLon(String[] lines, ParsedMessage pm, MessageService ms) {
	var p = Pattern.compile("http://.*loc:(\\d+\\.\\d+)\\+(-\\d+\\.\\d+)");
	
	for (int i = 0; i < lines.length; i++) {
	    var line = lines[i].trim();
	    var m = p.matcher(line);

	    if (m.matches()) {
		log.info("matched lat/lon");
		log.info("group1=" + m.group(1));
		log.info("group2=" + m.group(2));
		var lat = Double.parseDouble(m.group(1));
		var lon = Double.parseDouble(m.group(2));
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
		//log.info(pm.location.toString());

		return 0;
	    }
        }

	return 1;
    }

    private void get_code(String[] lines, ParsedMessage pm) {
	var p = Pattern.compile("Alert: (.*)");
	
	for (int i = 0; i < lines.length; i++) {
	    var m = p.matcher(lines[i].trim());

	    if (m.matches()) {
		log.info("found code");
		pm.Code = m.group(1);
		return;
	    }
	}

	return;
    }

    private void get_address(String[] lines, ParsedMessage pm) {
	int start = lines.length;
	
	for (int i = 0; i < lines.length; i++) {
	    var line = lines[i].trim();

	    //log.info("line=" + line);

	    if (line.equals("LOC:")) {
		//log.info("found LOC:");
		start = i + 1;
		break;
	    }
	}

	log.info("start=" + start);
	log.info("lines.length=" + lines.length);
	var addr = "";
	var p = Pattern.compile("BTWN: .*");

	for (int i = start; i < lines.length; i++) {
	    var line = lines[i].trim();
	    var m = p.matcher(line);

	    // log.info("line=" + line);

	    if (m.matches()) {
		log.info("found BTWN:");
		break;
	    }
	
	    addr += line;
	    addr += " ";
	}

	pm.Address = addr.trim();
	//log.info("Address=" + pm.Address);

	return;
    }
}
