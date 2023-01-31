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
221;;AUTUMN LN;;MONCKS CORNER;;;SPRING LN;WINDWOOD LN;-80.0807220345628;33.1088860880615;;53A02;;11/06/2020 19:32:46;53 SERVICE CALL;LIFT ASSIST//198LBS;

 0 IncStreetNum
 1 IncPreDir
 2 IncStreetName
 3 IncAptLoc
 4 IncCommunity
 5 IncIntersection
 6 IncLandName
 7 CrossLow
 8 CrossHigh
 9 XCoor (longitude)
10 YCoor (latitude)
11 MedDispatchLevel
12 FireDispatchLevel
13 LawDispatchLevel
14 CreateWhen
15 CallType
16 Comment

*/
public class whitesviller implements MsgParser {
    private static final Logger log = LoggerFactory.getLogger(whitesviller.class);

    @Override
    public ParsedMessage Process(Customer cust, String msg, MessageService msgService) {
        ParsedMessage parsedMessage = new ParsedMessage();
        parsedMessage.ErrorFlag = 99;

        log.info("whitesviller msg");
        log.info(msg);

        try {
	    var idx = msg.indexOf("Msg-");
	    var plainMsg = "";

	    if (idx >= 0)
		plainMsg = msg.substring(idx + 4);
	    else
		plainMsg = msg;

            String [] flds = plainMsg.split(";");

            parsedMessage.text = plainMsg.replace(";", " ");

	    var code_ok = get_code(flds, parsedMessage);
	    var addr_ok = get_address(flds, parsedMessage);
	    var latlon_ok = get_latlon(flds, parsedMessage, msgService);

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

    private int get_code(String[] flds, ParsedMessage pm) {
	pm.Code = flds[15].trim();
	return 0;
    }

    private int get_latlon(String[] flds, ParsedMessage pm, MessageService ms) {
	var plat = Pattern.compile("([-+]?\\d+\\.\\d+)") ;
	var mlat = plat.matcher(flds[10]);

	var plon = Pattern.compile("([-+]?\\d+\\.\\d+)") ;
	var mlon = plat.matcher(flds[9]);
	var lat = "";
	var lon = "";

	if (mlat.matches())
	    lat = mlat.group(1);

	if (mlon.matches())
	    lon = mlon.group(1);
	
	if (lat.length() > 0 && lon.length() > 0) {
	    log.info("lat=" + lat);
	    log.info("lon=" + lon);

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

    private int get_address(String[] flds, ParsedMessage pm) {
	if (flds.length < 4)
	    return 1;

	var house_num = flds[0];
	var predir = flds[1];
	var street_name = flds[2];
	var city = flds[4];

	log.info("house_num=" + house_num);
	log.info("street_name=" + street_name);
	log.info("city=" + city);

	pm.Address = (house_num + " " + predir + " " + street_name + ", " + city + " " + "SC") ;

	return 0;
    }
}
