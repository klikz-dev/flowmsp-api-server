package com.flowmsp.service.MessageParser;

import com.flowmsp.domain.customer.Customer;
import com.flowmsp.domain.location.Location;
import com.flowmsp.service.PolygonUtil;
import com.flowmsp.service.Message.MessageService;
import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Position;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import javax.xml.xpath.XPath;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.HashMap;

public class UrbanaEmail implements MsgParser {
    private static final Logger log = LoggerFactory.getLogger(UrbanaEmail.class);

    @Override
    public ParsedMessage Process(Customer cust, String msg, MessageService msgService) {
        ParsedMessage parsedMessage = new ParsedMessage();
        parsedMessage.ErrorFlag = 99;

        log.info("Urbana msg");
        log.info(msg);

        /*
       	Call Time:1/24/2021 21:47:12Call Type:STILL=20
		Address:534 FAIRLAWN DR
		 URBANACommon Name:
		Closest Intersection:ANDERSON ST / S VINE STAdditional Location Info:
		Nature of Call:
		Assigned Units:L261
		 E254
		Priority:1-HighStatus:1 - High
		Quadrant:98116District:UF-M1Beat:64
		CFS Number:133
		Primary Incident:2021-00001097 (CD111)
		Radio Channel:
		Narrative:ACTIVATED CO DETECTOR IN RES
		NOBODY ILL
		40.1042717437536/-88.2013793589179
         */
        try {
            String[] lines = msg.split("\r?\n");

			parsedMessage.Code = get_code(lines);
			parsedMessage.text = get_text(lines);
			parsedMessage.Address = get_address(cust, lines);

			get_units(parsedMessage, lines);

			var has_latlon = get_latlon(lines, parsedMessage, msgService);

			if (has_latlon == 0)
				parsedMessage.ErrorFlag = 0;

            //Address should be of at-least 5 characters
            else if (parsedMessage.Address.length() > 5) {
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

            parsedMessage.ErrorFlag = 0;
        } catch (Exception ex) {
            // log.error(ex);
            parsedMessage.ErrorFlag = 1;
        }

        return parsedMessage;
    }

	private String get_text(String[] lines) {
		return String.join(" ", lines);
	}

    /*
    Assigned Units:E251
 325
 352
 MED
     */
	private void get_units(ParsedMessage pm, String[] lines) {
		var msg = String.join(" ", lines);
		var p = Pattern.compile(".*Assigned Units:(.*)Priority.*");
		var m = p.matcher(msg);

		if (m.matches()) {
			var nodes = m.group(1).split(" ");

			for (int i = 0; i < nodes.length; i++)
				pm.units.add(nodes[i]);
		}
	}

	private String get_code(String[] lines) {
		var p = Pattern.compile(".*Call Type:(.+)");

		for (int i = 0; i < lines.length; i++) {
			var line = lines[i].trim();
			var m = p.matcher(line);

			if (m.matches())
				return m.group(1);

		}

		return "";
	}

	/*
	Address:534 FAIRLAWN DR
 URBANACommon Name:
	 */
	private String get_address(Customer cust, String[] lines) {
		var msg = String.join(" ", lines);
		var p = Pattern.compile(".*Address:(.*)Common Name:.*");
		var m = p.matcher(msg);

		if (!m.matches())
			return "";

		var addr = m.group(1);

		// add state from customer record
		addr += (", " + cust.address.state);

		return addr;
	}

	/*
	40.1042717437536/-88.2013793589179
	 */
	private int get_latlon(String[] lines, ParsedMessage pm, MessageService ms) throws javax.xml.xpath.XPathExpressionException {
		var p = Pattern.compile("([-+]?\\d+\\.\\d+)/([-+]?\\d+\\.\\d+)");
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
}
