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
1081;;OLD US HWY 421;UNIT 1;SUGAR GROVE;;;END OF DALE ADAMS RD;END OF OLD MILL RD;-81.783176114307;36.2656513224774;;67D02U;;05/03/2020 21:02:37;OUTSIDE FIRE;large fire near the road ;2020012838;"

not sure of the layout, guesses in parens

0 1081  house number
1 
2 OLD US HWY 421 (addr1)
3 UNIT 1 (addr2)
4 SUGAR GROVE (city)
5  (cross streets)
6 
7 END OF DALE ADAMS RD
8 END OF OLD MILL RD
9 -81.783176114307 (lat)
10 36.2656513224774 (lon)
11 
12 67D02U
13 
14 05/03/2020 21:02:37
15 OUTSIDE FIRE (code)
16 large fire near the road 
17 2020012838
18 

*/
public class covecreekvol implements MsgParser {
    private static final Logger log = LoggerFactory.getLogger(covecreekvol.class);

    @Override
    public ParsedMessage Process(Customer cust, String msg, MessageService msgService) {
        ParsedMessage parsedMessage = new ParsedMessage();
        parsedMessage.ErrorFlag = 99;

        log.info("covecreekvol msg");
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
	if (flds.length > 15) {
	    log.info("code=" + flds[15]);

	    pm.Code = flds[15].trim();
	    return 0;
	}
	else
	    return 1;
    }

    private int get_latlon(String[] flds, ParsedMessage pm, MessageService ms) {
	// not enough fields
	if (flds.length <= 10)
	    return 1;

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
	var street_name = flds[2];
	var city = flds[4];

	log.info("house_num=" + house_num);
	log.info("street_name=" + street_name);
	log.info("city=" + city);

	pm.Address = (house_num + " " + street_name + ", " + city + ", NC") ;

	return 0;
    }
}
