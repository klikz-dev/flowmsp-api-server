package com.flowmsp.service.MessageParser;

import com.flowmsp.domain.customer.Customer;
import com.flowmsp.domain.location.Location;
import com.flowmsp.service.PolygonUtil;
import com.flowmsp.service.Message.MessageService;
import com.mongodb.client.model.geojson.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Position;

public class pekinfiredepEmail implements MsgParser {
    private static final Logger log = LoggerFactory.getLogger(pekinfiredepEmail.class);

    @Override
    public ParsedMessage Process(Customer cust, String msg, MessageService msgService) {
        ParsedMessage parsedMessage = new ParsedMessage();
        parsedMessage.ErrorFlag = 99;

        log.info("pekinfiredep msg");
        log.info(msg);

	/*
	  FYI: ;PEFD;11/07/2019 18:34:57;DIABETIC PROBLEMS;2111 COURT ST;PE;S PARKWAY DR;COTTAGE GROVE AV;15 yof/bs low/c/b  [11/07/19 18:35:46 JUERGENSR] ;PFD1
	*/
        try {
	    var plainMsg = msg;
	    var idx = plainMsg.indexOf("Msg-");

	    if (idx >= 0) {
		plainMsg = plainMsg.replace("Msg-", "");
		plainMsg = plainMsg.substring(idx);
		plainMsg = plainMsg.trim();
		parsedMessage.text = plainMsg;
	    }
	    else {
		parsedMessage.text = msg;
	    }

            String [] messageArray = parsedMessage.text.split(";");

	    if (messageArray.length > 0) {
		parsedMessage.Code = messageArray[3].trim() ;
		parsedMessage.Address = messageArray[4].trim() + ", Pekin, IL" ;
	    }

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
