package com.flowmsp.service.MessageParser;

import com.flowmsp.domain.customer.Customer;
import com.flowmsp.domain.location.Location;
import com.flowmsp.service.Message.MessageService;
import com.flowmsp.service.PolygonUtil;
import com.mongodb.client.model.geojson.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.regex.Pattern;

/*
1537 1535 FD LIFE THREAT EMS
526 BRYAN DR KIRKWOOD MO MapRegions: 35 CrossStreets: BRYAN AVE 0.05 mi W BRYAN MEADOWS CT 0.05 mi W
Description: LONG FALL
 */

public class kirkwood3838 implements MsgParser{
    private static final Logger log = LoggerFactory.getLogger(kirkwood3838.class);

    @Override
    public ParsedMessage Process(Customer cust, String msg, MessageService msgService) {
        ParsedMessage parsedMessage = new ParsedMessage();
        parsedMessage.ErrorFlag = 99;

        try {
            String[] lines = msg.split("\r?\n");

            parsedMessage.text = get_text(lines);
            parsedMessage.Address = get_address(lines);
            get_units(parsedMessage, lines);

            log.info("Address: " + parsedMessage.Address);
            log.info("Units: " + parsedMessage.units);

            //Address should be of at-least 5 characters
            if (parsedMessage.Address.length() > 5) {
                // Now PlainMsg has Address Only
                Location location = msgService.getLocationByAddress(parsedMessage.Address);
                Point latLon;
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

    private String get_text(String[] lines) {
        return String.join(" ", lines);
    }

    private String get_address(String[] lines) {
        var addr = "";

        var p = Pattern.compile("(.*)(MapRegions:)");
        var m = p.matcher(lines[1]);
        addr = m.find() ? m.group(1).trim() : "";

        return addr;
    }

    private void get_units(ParsedMessage pm, String[] lines) {
        var p = Pattern.compile("\\d{4}");
        var m = p.matcher(lines[0]);

        while (m.find()) {
            pm.units.add(m.group(0).trim());
        }
        return;
    }
}
