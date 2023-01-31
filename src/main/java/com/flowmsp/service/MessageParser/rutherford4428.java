package com.flowmsp.service.MessageParser;

import com.flowmsp.domain.customer.Customer;
import com.flowmsp.domain.location.Location;
import com.flowmsp.service.Message.MessageService;
import com.flowmsp.service.PolygonUtil;
import com.mongodb.client.model.geojson.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.regex.Pattern;

//To - rfd1
//Location49 SIMS SANDPIT RD*
//APT/ROOM*
//
//CityRUTHERFORDTON*
//
//
//
//
//Call TypeDIABETIC*
//
//
//
//UnitsHNG1,RFD1*

public class rutherford4428 implements MsgParser {
    private static final Logger log = LoggerFactory.getLogger(rutherford4428.class);

    @Override
    public ParsedMessage Process(Customer cust, String msg, MessageService msgService) {
    ParsedMessage parsedMessage = new ParsedMessage();
    parsedMessage.ErrorFlag = 99;

        try {
        String[] lines = msg.split("\r?\n");

        parsedMessage.text = get_text(lines);
        parsedMessage.Address = get_address(cust, lines);
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

    private String get_address(Customer cust, String[] lines) {
        var addr = "";
        for (var line : lines) {
            if (line.startsWith("Location")) {
                var p = Pattern.compile("Location(.*)");
                var m = p.matcher(line);
                addr = m.find() ? m.group(1).replaceAll("[//? \\*]", " ") : "";
                addr = addr.trim() + ", " + cust.address.city + ", " + cust.address.state + ", " + cust.address.zip;
            }
        }
        return addr;
    }

    private void get_units(ParsedMessage pm, String[] lines) {
        var p = Pattern.compile("Units(.*)");

        for (var line : lines) {
            var m = p.matcher(line);

            if (m.matches()) {
                for (var node : m.group(1).split(","))
                    pm.units.add(node.replaceAll("\\*","").trim());
            }
        }

        return;
    }
}
