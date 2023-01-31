package com.flowmsp.service.MessageParser;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import com.flowmsp.domain.customer.Customer;
import com.flowmsp.domain.location.Location;
import com.flowmsp.service.PolygonUtil;
import com.flowmsp.service.Message.MessageService;
import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Position;
import com.univocity.parsers.annotations.Parsed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
29 MVA Rescue Per PD ATHagemann Rd / Mooney Ln BUS: XST: TAC:CC911 SO - FTAC 20 Mehlville FPD 1740,7415,1737,RCDN1,1756,17DN
 */

public class eurekafirepr implements MsgParser {
    private static final Logger log = LoggerFactory.getLogger(eurekafirepr.class);

    @Override
    public ParsedMessage Process(Customer cust, String msg, MessageService msgService) {
        ParsedMessage parsedMessage = new ParsedMessage();
        parsedMessage.ErrorFlag = 99;

        try {
        parsedMessage.text = get_text(msg);
        parsedMessage.Address = get_address(cust, msg);
        parsedMessage.Code = get_code(parsedMessage.text);
        var latlon_ok = get_latlon(msg, parsedMessage, msgService);
        get_units(msg, parsedMessage);

        log.info("Text: " + parsedMessage.text);
        log.info("Address: " + parsedMessage.Address);
        log.info("Code: " + parsedMessage.Code);
        log.info("Units: " + parsedMessage.units);

        //Address should be of at-least 5 characters
        if (latlon_ok == 1 && parsedMessage.Address.length() > 5) {
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
            parsedMessage.ErrorFlag = 1;
        }
        return parsedMessage;
    }

    private String get_text(String msg)
    {
        if (msg.contains("Msg-")) {
            msg = msg.substring(msg.indexOf("Msg-") + "Msg-".length());
            msg = msg.trim();
        }
        return msg;
    }

    private String get_address(Customer cust, String msg)
    {
        var p = Pattern.compile("(.*) AT:?(.*) (BUS|XST|TAC):(.*)");
        var m = p.matcher(msg);
        var addr = m.find() ? m.group(2) : "";

        return addr + ", " + cust.address.city + ", " + cust.address.state;
    }

    private String get_code(String msg)
    {
        var p = Pattern.compile("(.*)AT:.*");
        var m = p.matcher(msg);
        var code = m.find() ? m.group(1) : "";

        return code;
    }

    private int get_latlon(String msg, ParsedMessage pm, MessageService ms) {
        Double lat = null;
        Double lon = null;

        var p = Pattern.compile("(\\d{8})");
        var m = p.matcher(msg);

        lat = m.find() ? (Double.parseDouble(m.group(1)) / 1000000) : null;
        lon = m.find() ? ((Double.parseDouble(m.group(1)) / 1000000) * -1) : null;

        if (lat !=null && lon !=null) {
            var pos = new Position(lon, lat);
            var point = new Point(pos);

            pm.messageLatLon = point;

            return 0;
        }
        return 1;
    }

    private void get_units(String msg, ParsedMessage pm) {
        var p = Pattern.compile(".* +([\\d,A-Z]+) +(\\d+) +(\\d+)$");
        var m = p.matcher(msg);
        var f = m.find() ? m.group(1) : "";
        var units = f.split(",");

        for (String unit : units) {
            pm.units.add (unit.trim());
        }

        return;
    }
}
