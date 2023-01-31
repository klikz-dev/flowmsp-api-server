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

public class SpringLakeEmail implements MsgParser {
    private static final Logger log = LoggerFactory.getLogger(SpringLakeEmail.class);

    @Override
    public ParsedMessage Process(Customer cust, String msg, MessageService msgService) {
        ParsedMessage parsedMessage = new ParsedMessage();
        parsedMessage.ErrorFlag = 99;

        try {
            String[] lines = msg.split("\n");

            parsedMessage.text = get_text(lines);
            parsedMessage.Code = get_code(lines);
            parsedMessage.Address = get_address(lines);

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

    private String get_address(String[] lines) {
        var p = Pattern.compile("Addr: (.+)");
        var city = "";
        var addr = "";

        for (int i = 0; i < lines.length; i++) {
            var line = lines[i].trim();
            var m = p.matcher(line);

            if (m.matches()) {
                addr = m.group(1);
                addr += (", " + lines[i + 1].trim());
            }
        }

        return addr;
    }

    private String get_code(String[] lines) {
        if (lines.length > 0)
            return lines[0].trim();
        else
            return "";
    }

    private String get_text(String[] lines) {
        var text = "";

        for (var line : lines) {
            if (line.length() == 0)
                break;

            text += (line.trim() + " ");
        }

        return text.trim();
    }
}
