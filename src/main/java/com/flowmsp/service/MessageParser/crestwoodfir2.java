package com.flowmsp.service.MessageParser;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import com.flowmsp.domain.customer.Customer;
import com.flowmsp.domain.location.Location;
import com.flowmsp.service.PolygonUtil;
import com.flowmsp.service.Message.MessageService;
import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Position;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
29 MVA Rescue Per PD ATHagemann Rd / Mooney Ln BUS: XST: TAC:CC911 SO - FTAC 20 Mehlville FPD 1740,7415,1737,RCDN1,1756,17DN
 */

public class crestwoodfir2 implements MsgParser {
    private static final Logger log = LoggerFactory.getLogger(crestwoodfir2.class);

    @Override
    public ParsedMessage Process(Customer cust, String msg, MessageService msgService) {
        ParsedMessage parsedMessage = new ParsedMessage();
        parsedMessage.ErrorFlag = 99;

        log.info("crestwoodfir2 msg");
        log.info(msg);

        String plainMsg = msg;

        if (plainMsg.contains("Msg-")) {
            plainMsg = plainMsg.substring(plainMsg.indexOf("Msg-") + "Msg-".length());
            plainMsg = plainMsg.trim();
        }

        try {
            parsedMessage.text = plainMsg;

            var p = Pattern.compile("(.*) AT:?(.*) (BUS|XST|TAC):(.*)");
            var m = p.matcher(plainMsg);

            if (!m.matches()) {
                log.info("crestwoodfir2, pattern match failed");
                parsedMessage.ErrorFlag = 1;
            }
            else {
                parsedMessage.Code = m.group(1);
                parsedMessage.Address = m.group(2) + ", Crestwood, MO";
                var bus  = m.group(3); // business name
                var xst  = m.group(4); //cross streets

                log.info("code: <" + parsedMessage.Code + ">");
                log.info("addr: <" + parsedMessage.Address + ">");
                log.info("bus", bus);
                log.info("xst", xst);

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
            }
        } catch (Exception ex) {
            parsedMessage.ErrorFlag = 1;
        }
        return parsedMessage;
    }
}
