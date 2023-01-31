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
public class montgomery implements MsgParser {
    private static final Logger log = LoggerFactory.getLogger(montgomery.class);

    @Override
    public ParsedMessage Process(Customer cust, String msg, MessageService msgService) {
        ParsedMessage parsedMessage = new ParsedMessage();
        parsedMessage.ErrorFlag = 99;

        /* Address: 1601 COMMERCE DR, MONTGOMERY
        Cross: AUCUTT RD / DEAD END
        Alerts:
        Nature:
        Call Type: FIRE ALARM
        Description: FIRE ALARM
        Due: MBAT1, ME1, ME6, MS1, MTK2
        Incident Number: [2021-00000252 KA273]
        Date / Time: 4/1/2021 21:25:07
        https://www.google.com/maps/search/?api=1&query=1601+COMMERCE+DR,MONTGOMERY,IL */

        try {
            // get rid of HTML tags
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

            // parsedMessage.text = String.join(" ", lines);
            parsedMessage.text = get_message_text(lines);

            parsedMessage.Code = get_code(lines);
            parsedMessage.Address = get_address(cust, lines);
            parsedMessage.incidentID = get_incident(lines);

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
        return String.join(" ", lines);
    }

    //Incident Number: [2021-00000252 KA273]
    private String get_incident(String[] lines) {
        var p = Pattern.compile("Incident Number: (.*)");

        for (var line : lines) {
            var m = p.matcher(line.trim());

            if (m.matches())
                return m.group(1).trim();
        }

        return "";
    }

    private String get_code(String[] lines) {
        var p = Pattern.compile("Call Type: (.*)");

        for (var line : lines) {
            var m = p.matcher(line.trim());

            if (m.matches())
                return m.group(1).trim();
        }

        return "";
    }

    private String get_address(Customer cust, String[] lines) {
        var p = Pattern.compile("Address: (.*)");

        for (var line : lines) {
            var m = p.matcher(line.trim());

            if (m.matches()) {
                var addr = m.group(1).trim();

                addr += (", " + cust.address.state);

                return addr;
            }
        }
        return "";
    }

    private int get_units(String[] lines, ParsedMessage pm) {
        var p = Pattern.compile("Due: (.*)");

        for (var line : lines) {
            var m = p.matcher(line.trim());

            if (m.matches()) {
                var units = m.group(1).split(",");

                for (String u : units) {
                    pm.units.add(u.trim());
                }
            }
        }

        return 0;
    }

}
