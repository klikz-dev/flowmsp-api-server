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

public class TriTownshipEmail implements MsgParser {
    private static final Logger log = LoggerFactory.getLogger(TriTownshipEmail.class);

    @Override
    public ParsedMessage Process(Customer cust, String msg, MessageService msgService) {
        ParsedMessage parsedMessage = new ParsedMessage();
        parsedMessage.ErrorFlag = 99;

        log.info("TriTownship msg");
        log.info(msg);

	/*
	  QFD-TTFD-PFD 10-33
	  Units: 3A15 BR5 TRI2   
	  N 72ND ST and ELLINGTON RD
	  City: QUINCYCross Streets : BARDON DR * ELLINGTON RD
	  Comments:
	  [13:33:13 Q22:jvahlkam] ProQA EMS Case Closed
	  [13:32:58 Q22:jvahlkam] LAYING ON THE GROUND BY THE VEH
	  [13:32:47 Q22:jvahlkam] =5. Doesn't have asthma or lung probs.
	  [13:32:47 Q22:jvahlkam] =4. Not clammy.
	  [13:32:47 Q22:jvahlkam] =3. Not changing color.
	  [13:32:47 Q22:jvahlkam] =2. No diff speaking btwn breaths.
	 */
        try {
	    var p_city = Pattern.compile("City: (.*)Cross Streets : (.*)");
	    var p_comments = Pattern.compile("Comments:");
	    var address = (String) null;
	    var city = "";
	    var cross_streets = (String) null;
	    var text = "";

            String [] messageArray = msg.split("\n");

	    if (messageArray.length > 0)
		parsedMessage.Code = messageArray[0].trim() ;

            for (int i = 0; i < messageArray.length; i++) {
		var line = messageArray[i].trim();
		log.info(i + " " + line);
		var c = p_city.matcher(line);

		if (c.matches()) {
		    city = c.group(1);
		    cross_streets = c.group(2);
		    log.info(i + " found city " + city);
		    log.info(i + " found cross streets" + cross_streets);

		    if (i > 0) {
			address = messageArray[i-1].trim();
			log.info("address=" + address);
		    }
		}

		var comment = p_comments.matcher(line);

		if (comment.matches()) {
		    log.info(i + " found beginning of comments");
		    break;
		}

		text += messageArray[i];
	    }
		
	    /*
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
	    */

	    if (address == null && cross_streets != null) {
		address = cross_streets;
	    }

            parsedMessage.text = text.replace("\r\n", " ");

	    if (address != null)
		parsedMessage.Address = address + ", " + city + ", IL";
	    else
		parsedMessage.Address = "";

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
