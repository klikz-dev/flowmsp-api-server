package com.flowmsp.service.MessageParser;

import com.flowmsp.domain.customer.Customer;
import com.flowmsp.domain.location.Location;
import com.flowmsp.service.PolygonUtil;
import com.flowmsp.service.Message.MessageService;
import com.mongodb.client.model.geojson.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.HashMap;

public class SikestonEmail implements MsgParser {
    private static final Logger log = LoggerFactory.getLogger(SikestonEmail.class);
    private final HashMap<String, String> city_mapping = new HashMap<String, String>();

    @Override
    public ParsedMessage Process(Customer cust, String msg, MessageService msgService) {
        ParsedMessage parsedMessage = new ParsedMessage();
        parsedMessage.ErrorFlag = 99;
	parsedMessage.Address = "";
	//private final HashSet<String> skip = new HashSet<String>();

        //skip.add("Call Number");
        //skip.add("Zone");

        log.info("Sikeston msg");
        log.info(msg);

	/*
	  Call Number: P201919280 
	  Situation: FIRE-MUTUAL AID 
	  Address: 205 DAVIS , MATTHEWS,  MO 
	  Zone: New Madrid County
	*/
        try {
	    var loc = Pattern.compile("Address:(.+)");
	    var sit = Pattern.compile("Situation:(.+)");
	    var plainMsg = msg.replace("<br>", "\n");
            String [] messageArray = plainMsg.split("\r\n");

            parsedMessage.text = plainMsg.replace("\r\n", " ");

            for (int i = 0; i < messageArray.length; i++) {
		log.info(i + " " + messageArray[i]);
		var line = messageArray[i].trim();
		var m = loc.matcher(line);
		var s = sit.matcher(line);

		if (m.matches()) {
		    parsedMessage.Address = m.group(1) + ", MO 63801";
		    log.info(i + " found location " + m.group(1));
		}

		if (s.matches()) {
		    parsedMessage.Code = s.group(1);
		    log.info(i + " found situation " + s.group(1));
		}
	    }

	    /*
	      look for records with empty address line
	     */
	    
	    log.info("Address: " + parsedMessage.Address);
	    log.info("Code: " + parsedMessage.Code);

            //Address should be of at-least 5 characters
            if (parsedMessage.Address.length() > 5) {
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

		parsedMessage.ErrorFlag = 0;
            }
	    else
		parsedMessage.ErrorFlag = 1;
		
        } catch (Exception ex) {
	    // log.error(ex);
            parsedMessage.ErrorFlag = 1;
        }

        return parsedMessage;
    }
}
