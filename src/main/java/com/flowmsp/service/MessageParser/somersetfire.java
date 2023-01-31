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
CAD #1469684:
ALARM
470 TOM BALL RD, TAZEWELL [379] (36.434669, -83.571917)
BUILDING 200;  ZONE 63
*/
public class somersetfire implements MsgParser {
    private static final Logger log = LoggerFactory.getLogger(somersetfire.class);

    @Override
    public ParsedMessage Process(Customer cust, String msg, MessageService msgService) {
        ParsedMessage parsedMessage = new ParsedMessage();
        parsedMessage.ErrorFlag = 99;

        try {
            parsedMessage.text = get_message_text(msg);

            parsedMessage.Code = get_code(msg);
            parsedMessage.Address = get_address(cust, msg);
            parsedMessage.incidentID = get_incident(msg);

            get_units(msg, parsedMessage);

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

    private String get_message_text(String msg) {
        return msg;
    }

    private String get_incident(String msg) {
        var p = Pattern.compile("\\d{2}/\\d{2}/\\d{2} \\d{2}:\\d{2} - ([A-Z0-9]+) Incident: .*");
        var m = p.matcher(msg);

        if (m.matches())
            return m.group(1).trim();
        else
            return "";
    }

    private String get_code(String msg) {
        var p = Pattern.compile(".*Incident: (.*) Additional Incident: .*");
        var m = p.matcher(msg);

        if (m.matches())
            return m.group(1).trim();
        else
            return "";
    }
    private String get_address(Customer cust, String msg) {
        var p = Pattern.compile(".* Location: (.*) Common Name: .*");
        var m = p.matcher(msg);

        if (m.matches())
            return m.group(1).trim();
        else
            return "";
    }

    private int get_units(String msg, ParsedMessage pm) {
        var p = Pattern.compile(".* Assigned Units: (.*)");
        var m = p.matcher(msg);

        if (m.matches()) {
            var units = m.group(1).split(";");

            for (String u : units)
                pm.units.add(u.trim());
        }

        return 0;
    }

}
