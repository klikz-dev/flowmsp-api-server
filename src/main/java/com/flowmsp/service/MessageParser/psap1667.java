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
public class psap1667 implements MsgParser {
    private static final Logger log = LoggerFactory.getLogger(psap1667.class);

    @Override
    public ParsedMessage Process(Customer cust, String msg, MessageService msgService) {
        ParsedMessage parsedMessage = new ParsedMessage();
        parsedMessage.ErrorFlag = 99;

        try {
            String[] lines = msg.split("\r?\n");

            parsedMessage.text = get_message_text(lines);
            parsedMessage.Code = get_code(lines);
            parsedMessage.Address = get_address(cust, lines);

            get_units(lines, parsedMessage);

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

    private String get_message_text(String[] lines) {
        var p = Pattern.compile("COMMENTS:.*?");
        var dashes = Pattern.compile("-+");
        var msg = "";

        for (var line : lines) {
            if (line.trim().length() == 0)
                continue;

            if (dashes.matcher(line).matches())
                continue;

            var m = p.matcher(line);

            if (m.matches())
                break;

            msg += (line + "\n");
        }

        return msg;
    }

    private String get_code(String[] lines) {
        var p = Pattern.compile("Inc Type: (.*)");

        for (var line : lines) {
            var m = p.matcher(line.trim());

            if (m.matches())
                return m.group(1).trim();
        }

        return "";
    }

    private String get_address(Customer cust, String[] lines) {
        var p = Pattern.compile("Location: (.*)");

        for (var line : lines) {
            var m = p.matcher(line.trim());

            if (m.matches()) {
                var addr = m.group(1).trim();

                addr += (", " + cust.address.city);
                addr += (", " + cust.address.state);

                return addr;
            }
        }
        return "";
    }

    private int get_units(String[] lines, ParsedMessage pm) {
        var p = Pattern.compile("UNITS DISPATCHED:");
        int i = 0;

        for (i = 0; i < lines.length; i++) {
            var line = lines[i].trim();
            var m = p.matcher(line);

            if (m.matches())
                break;
        }

        // exited loop early means match found
        // next line contains the units
        if (i < lines.length) {
            var units = lines[i+1].split(",");

            for (var u : units)
                pm.units.add(u.split("/")[1].trim());
        }

        return 0;
    }

}
