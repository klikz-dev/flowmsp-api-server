package com.flowmsp.service.MessageParser;

import com.flowmsp.domain.customer.Customer;
import com.flowmsp.domain.location.Location;
import com.flowmsp.service.PolygonUtil;
import com.flowmsp.service.Message.MessageService;
import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Position;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import javax.xml.xpath.XPath;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.HashMap;

public class rantoulfire implements MsgParser {
    private static final Logger log = LoggerFactory.getLogger(rantoulfire.class);

    @Override
    public ParsedMessage Process(Customer cust, String msg, MessageService msgService) {
        ParsedMessage parsedMessage = new ParsedMessage();
        parsedMessage.ErrorFlag = 99;

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

            parsedMessage.Code = get_code(lines);
            parsedMessage.text = get_text(lines);
            parsedMessage.Address = get_address(cust, lines);
            parsedMessage.incidentID = get_incident(lines);

            get_units(parsedMessage, lines);

            var has_latlon = get_latlon(lines, parsedMessage, msgService);

            if (has_latlon == 0)
                parsedMessage.ErrorFlag = 0;

                //Address should be of at-least 5 characters
            else if (parsedMessage.Address.length() > 5) {
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

    private String get_text(String[] lines) {
        var msg = "";

        for (var line : lines) {
            if (line.contains("Narrative"))
                break;

            msg += line.trim() + " ";
        }

        return msg;
    }

    private void get_units(ParsedMessage pm, String[] lines) {
        var p = Pattern.compile(".*Unit: (.*)");

        for (var line : lines) {
            var m = p.matcher(line);

            if (m.matches())
                pm.units.add(m.group(1).trim());
        }
    }

    private String get_code(String[] lines) {
        var p = Pattern.compile("Call Type (.*)");

        for (var line : lines) {
            var m = p.matcher(line);

            if (m.matches())
                return m.group(1).trim();
        }

        return "";
    }

    private String get_address(Customer cust, String[] lines) {
        var p = Pattern.compile("Address (.*)");

        for (var line : lines) {
            var m = p.matcher(line);

            if (m.matches())
                return m.group(1).trim() + ", " + cust.address.state;
        }

        return "";
    }

    /*
    40.1042717437536/-88.2013793589179
     */
    private int get_latlon(String[] lines, ParsedMessage pm, MessageService ms) throws javax.xml.xpath.XPathExpressionException {
        var p = Pattern.compile("([-+]?\\d+\\.\\d+)/([-+]?\\d+\\.\\d+)");
        var lat = "";
        var lon = "";

        for (int i = 0; i < lines.length; i++) {
            var line = lines[i].trim();
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

    private String get_incident(String[] lines) {
        var p = Pattern.compile("Incident Number \\[(\\d{4}-\\d+) .*\\].*");

        for (var line : lines) {
            var m = p.matcher(line);

            if (m.matches())
                return m.group(1).trim();
        }

        return "";
    }
}
