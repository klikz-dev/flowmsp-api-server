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

public class JacksonvilleEmail implements MsgParser {
    private static final Logger log = LoggerFactory.getLogger(JacksonvilleEmail.class);

    @Override
    public ParsedMessage Process(Customer cust, String msg, MessageService msgService) {
        ParsedMessage parsedMessage = new ParsedMessage();
        parsedMessage.ErrorFlag = 99;

        log.info("Jacksonville msg");
        log.info(msg);

	/*
	  DISPATCH:JFD:JFD - CALL: 0006 BREATHING PROBLEMS
	  PLACE: ALIASJACKSONVILLE SKILLED NURSING AND REHAB
	  ADDR: 1517 W WALNUT, Apt/Unit #131
	  CITY: JACKSONVILLE
	  ID: JFD:19-002177
	  DATE: 09/11/2019
	  TIME: 01:24:38
	  MAP:=20
	  UNIT:=20
	  INFO: COMPLAI
	*/
        try {
	    var p_addr = Pattern.compile("ADDR: (.+)");
	    var p_place = Pattern.compile("PLACE: (.+)");
	    var p_city = Pattern.compile("CITY: (.+)");
	    var p_dispatch = Pattern.compile("DISPATCH:JFD:JFD - CALL: .... (.+)");
	    var address = "";
	    var city = "";
	    var place = (String) null;
	    var code = "";

            String [] messageArray = msg.split("\n");
            parsedMessage.text = msg.replace("\n", " ");

            for (int i = 0; i < messageArray.length; i++) {
		var line = messageArray[i].trim();
		log.info(i + " " + line);
		var a = p_addr.matcher(line);

		if (a.matches()) {
		    address = a.group(1);
		    log.info(i + " found address " + address);
		}

		var p = p_place.matcher(line);

		if (p.matches()) {
		    place = p.group(1).replace("ALIAS", "");
		    log.info(i + " found place " + place);
		}

		var c = p_city.matcher(line);

		if (c.matches()) {
		    city = c.group(1);
		    log.info(i + " found city " + city);
		}

		var d = p_dispatch.matcher(line);

		if (d.matches()) {
		    code = d.group(1);
		    log.info(i + " found code " + code);
		}
	    }
		
	    // remove apartment numbers, google doesn't seem to like them
	    var aptno = Pattern.compile(".*(, Apt/Unit #.+)");
	    var m = aptno.matcher(address);

	    if (m.matches()) {
		var apt = m.group(1);

		log.info("removing apartment number: " + apt);
		log.info("before: " + address);
		address = address.replace(apt, "");
		log.info("after: " + address);
	    }

	    parsedMessage.Code = code ;

	    /*
	    if (place != null)
		parsedMessage.Address = place + ", " + address + ", " + city + ", " + "IL";
	    else
		parsedMessage.Address = address + ", " + city + ", " + "IL";
	    */

	    parsedMessage.Address = address + ", " + city + ", " + "IL";

	    log.info("Code: " + parsedMessage.Code);
	    log.info("Address: " + parsedMessage.Address);

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
            }

            parsedMessage.ErrorFlag = 0;
        } catch (Exception ex) {
	    // log.error(ex);
            parsedMessage.ErrorFlag = 1;
        }

        return parsedMessage;
    }
}
