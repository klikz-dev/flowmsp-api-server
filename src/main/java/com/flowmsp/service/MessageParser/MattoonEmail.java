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

public class MattoonEmail implements MsgParser {
    private static final Logger log = LoggerFactory.getLogger(MattoonEmail.class);

    @Override
    public ParsedMessage Process(Customer cust, String msg, MessageService msgService) {
	ParsedMessage parsedMessage = new ParsedMessage();
	parsedMessage.ErrorFlag = 99;
	    
	try {
	    String plainMsg = msg;
	    log.info("Mattoon msg");
	    log.info(msg);

	    parsedMessage.Code = msg.substring(26, 41).trim();
	    parsedMessage.text = msg.substring(0, 600).trim();
		parsedMessage.incidentID = get_incident_id(msg);

	    var address = msg.substring(97, 97 + 73).trim();
	    var city = msg.substring(170, 170 + 34).trim();
	    var address_line = address + " " + city + " " + "IL";

	    log.info("address=<" + address + ">");
	    log.info("city=<" + city + ">");
	    log.info("address_line=<" + address_line + ">");

	    parsedMessage.Address = address_line;

		var has_latlon = get_latlon(msg, parsedMessage, msgService);

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
	    parsedMessage.ErrorFlag = 1;
	}

	return parsedMessage;
    }

	private String get_incident_id(String msg) {
    	return msg.substring(0,11);
	}

	//  X Coordinate: -088.6200 ALI Y Coordinate: 039.5997
	private int get_latlon(String msg, ParsedMessage pm, MessageService ms) throws javax.xml.xpath.XPathExpressionException {
    	var pLon = Pattern.compile(".* X Coordinate: ([-+]?\\d+\\.\\d+) .*");
    	var mLon = pLon.matcher(msg);

    	var pLat = Pattern.compile(".* Y Coordinate: ([-+]?\\d+\\.\\d+) .*");
    	var mLat = pLat.matcher(msg);
		var lat = "";
		var lon = "";

		if (mLon.find())
			lon = mLon.group(1);

		if (mLat.find())
			lat = mLat.group(1);

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
