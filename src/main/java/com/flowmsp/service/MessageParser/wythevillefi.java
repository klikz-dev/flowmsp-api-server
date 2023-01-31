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
ASSIST MOTORIST|2419 PEPPERS FERRY RD:Wytheville|20-019050|62 YO M- NOT BREATHING, CONGESTIVE HEART FAILURE
*/
public class wythevillefi implements MsgParser {
    private static final Logger log = LoggerFactory.getLogger(wythevillefi.class);

    @Override
    public ParsedMessage Process(Customer cust, String msg, MessageService msgService) {
        ParsedMessage parsedMessage = new ParsedMessage();
        parsedMessage.ErrorFlag = 99;

        log.info("wythevillefi msg");
        log.info(msg);

        try {
	    var idx = msg.indexOf("Msg-");
	    var plainMsg = "";

	    if (idx >= 0)
		plainMsg = msg.substring(idx + 4);
	    else
		plainMsg = msg;

            String [] flds = plainMsg.split("\\|");

            parsedMessage.text = plainMsg ;
	    parsedMessage.Code = flds[0] ;

	    var addr_ok = get_address(flds[1], parsedMessage);

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

    // 245 S 4TH ST; WYTHE COUNTY SHERIFFS OFFICE WCSO:Wytheville
    // 2419 PEPPERS FERRY RD:Wytheville
    private int get_address(String address_line, ParsedMessage pm) {
	String[] flds = address_line.split(":");
	String[] addr = flds[0].split(";");
	var city = "";
	
	// eg. 2419 PEPPERS FERRY RD:Wytheville
	if (flds.length > 1)
	    city = flds[1];
	else
	    city = "Wytheville";

	pm.Address = addr[0] + ", " + city + ", VA" ;

	return 0;
    }
}
