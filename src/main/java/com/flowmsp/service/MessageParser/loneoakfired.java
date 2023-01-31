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
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/*
Incident:2021F7729
Nat:FALL-FALL
Loc:6000 GARLANDS LN-BARRINGTON-5118
Grid:16TDM0667 Tac:FG
Trucks:A362; B36
Notes: CN - CHRISTINE FRIEDMAN 031356 AND MICHAEL FRIEDMAN 020759 ISSUED TRESPASS WARNING 031519
FALL IN THE MEMORY UNIT
WAIVED UNLESS REQUESTED
 */

public class loneoakfired implements MsgParser {
    private static final Logger log = LoggerFactory.getLogger(loneoakfired.class);

    @Override
    public ParsedMessage Process(Customer cust, String msg, MessageService msgService) {
        ParsedMessage parsedMessage = new ParsedMessage();
        parsedMessage.ErrorFlag = 99;

        log.info("loneoakfired msg");
        log.info(msg);

        try {
            msg = msg.replace("<div>", "");
            msg = msg.replace("</div>", "");
            msg = msg.replaceAll("<style .*</style>", "");
            msg = msg.replace("<html>", "");
            msg = msg.replace("<body>", "");
            msg = msg.replaceAll("<table .*>", "");
            msg = msg.replaceAll("<tr>", "");
            msg = msg.replaceAll("</td>", "");
            msg = msg.replaceAll("</tr>", "");
            msg = msg.replaceAll("<td .*>", "");
            msg = msg.replaceAll("</table>", "");
            msg = msg.replaceAll("</body>", "");
            msg = msg.replaceAll("</html", "");

            String[] lines = msg.split("\r?\n");

            parsedMessage.Code = get_code(lines);
            parsedMessage.text = get_text(lines);
            parsedMessage.Address = get_address(cust, lines);
            parsedMessage.incidentID = get_incident_id(lines);

            get_units(parsedMessage, lines);

            var latlon_ok = get_latlon(lines, parsedMessage, msgService);

            log.info("Code: " + parsedMessage.Code);
            log.info("Address: " + parsedMessage.Address);

            //Address should be of at-least 5 characters
            if (latlon_ok == 0) {
                log.info("using LAT/LON");
                parsedMessage.ErrorFlag = 0;
            }

        else if (parsedMessage.Address.length() > 5) {
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

    private String get_incident_id(String[] lines) {
        var p = Pattern.compile("INC#: \\[(.*) \\d+\\]");

        for (var line : lines) {
            var m = p.matcher(line);

            if (m.matches())
                return m.group(1);
        }

        return "";
    }

    private String get_text(String[] lines) {
        return String.join(" ", lines);
    }

    private void get_units(ParsedMessage pm, String[] lines) {
        var p = Pattern.compile("UNITS: (.*)");

        for (var line : lines) {
            var m = p.matcher(line);

            if (m.matches()) {
                for (var node : m.group(1).split("; ?"))
                    pm.units.add(node);
            }
        }

        return;
    }

    private String get_code(String[] lines) {
        var p = Pattern.compile("CALL TYPE: (.*)");

        for (var line : lines) {
            var m = p.matcher(line);

            if (m.matches())
                return m.group(1);
        }

        return "";
    }

    private String get_address(Customer cust, String[] lines) {
        var pLine1 = Pattern.compile("LOCATION: (.*), .*");
        var pLine2 = Pattern.compile("CITY: (.*), +STATE: (.*) +ZIP: (.*)");
        var addr = "";
        var city = "";

        for (var line : lines) {
            var m1 = pLine1.matcher(line);
            var m2 = pLine2.matcher(line);

            if (m1.matches())
                addr = m1.group(1);

            if (m2.matches())
                city = m2.group(1) + ", " + m2.group(2) + ", " + m2.group(3);
        }

        if (addr.length() > 0)
            addr += (", " + city);

        return addr;
    }
    private int get_latlon(String[] lines, ParsedMessage pm, MessageService ms) {
        var p = Pattern.compile("LATITUDE: ([+-]?\\d+\\.\\d+) +LONGITUDE: ([+-]?\\d+\\.\\d+)");
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
}
