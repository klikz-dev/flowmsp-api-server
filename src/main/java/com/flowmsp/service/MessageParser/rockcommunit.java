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

public class rockcommunit implements MsgParser {
    private static final Logger log = LoggerFactory.getLogger(rockcommunit.class);

    @Override
    public ParsedMessage Process(Customer cust, String msg, MessageService msgService) {
        ParsedMessage parsedMessage = new ParsedMessage();
        parsedMessage.ErrorFlag = 99;

        try {
            String[] lines = msg.split("\r?\n");

            parsedMessage.text = get_text(lines);
            parsedMessage.Code = get_code(lines);
            parsedMessage.Address = get_address(lines, cust);
            var latlon_ok = get_latlon(lines, parsedMessage, msgService);

            get_units(parsedMessage, lines);

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

    /* http://maps.google.com/maps?q=+38.34318%2C-90.39081 */
    private int get_latlon(String[] lines, ParsedMessage pm, MessageService ms) {
        var p = Pattern.compile("http://maps.google.com/maps\\?q=([-+]\\d+\\.\\d+)%2C([-+]\\d+\\.\\d+)") ;
        var lat = "";
        var lon = "";

        for (var line : lines) {
            var m = p.matcher(line);

            if (m.matches()) {
                lat = m.group(1);
                lon = m.group(2);
            }
        }

        if (lat.length() > 0 && lon.length() > 0) {
            var dlat = Double.parseDouble(lat) ;
            var dlon = Double.parseDouble(lon) ;

            var pos = new Position(dlon, dlat);
            var point = new Point(pos);

            pm.messageLatLon = point;

            try {
                pm.location = ms.getLocationByPoint(pm.messageLatLon);

                log.info(pm.messageLatLon.toString());
                return 0;
            }
            catch (Exception e) {
                log.error("getLocationByPoint failed for " + lat + " " + lon, e);
            }
        }

        return 1;
    }

    private String get_address(String[] lines, Customer cust) {
        if (lines.length < 1)
            return "";

        return lines[0] + ", " + cust.address.state;
    }

    private String get_code(String[] lines) {
        var p = Pattern.compile("[A-Z-]+ : (.*)");

        for (var line : lines) {
            var m = p.matcher(line);

            if (m.matches())
                return m.group(1);
        }

        return "";
    }

    private void get_units(ParsedMessage pm, String[] lines) {
        var p = Pattern.compile("(#.*)");

        for (var line : lines) {
            var m = p.matcher(line);

            if (m.matches()) {
                for (var node : m.group(1).split(" "))
                    pm.units.add(node.replace("#", ""));
            }
        }

        return;
    }

    private String get_text(String[] lines) {
        return String.join(" ", lines);
    }
}
