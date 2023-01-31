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
public class emporiafired implements MsgParser {
    private static final Logger log = LoggerFactory.getLogger(emporiafired.class);

    @Override
    public ParsedMessage Process(Customer cust, String msg, MessageService msgService) {
        ParsedMessage parsedMessage = new ParsedMessage();
        parsedMessage.ErrorFlag = 99;

        try {
            String[] lines = msg.split("\r\n");

            // parsedMessage.text = String.join(" ", lines);
            parsedMessage.text = get_message_text(lines);

            parsedMessage.Code = get_code(lines);
            parsedMessage.Address = get_address(cust, lines);
            parsedMessage.incidentID = get_incident(lines);

            var latlon_ok = get_latlon(lines, parsedMessage, msgService);

            get_units(lines, parsedMessage);

            if (latlon_ok == 0) {
                log.info("using LAT/LON");
                parsedMessage.ErrorFlag = 0;
            } else if (parsedMessage.Address.length() > 5) {
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
        String[] patterns = {".*(REPORTED: .*)",
                "(Nature: .*)Type: .*",
                "(Address: .*)Zone: .*",
                "(City: .*)",
                "(Responding Units: .*)",
        };
        var c = Pattern.compile("Comments:.*");
        var i2 = lines.length;
        String text = "";

        for (int i = 0; i < lines.length; i++) {
            var line = lines[i].trim();

            for (int j = 0; j < patterns.length; j++) {
                var p = Pattern.compile(patterns[j]);
                var m = p.matcher(line);

                if (m.matches()) {
                    text += m.group(1).trim();
                    text += "\n";
                }
            }

            var m = c.matcher(line);

            if (m.matches()) {
                log.info("comment found");
                text += line;
                text += "\n";
                i2 = i;
                break;
            }
        }

        // copy remaining lines
        for (int i = i2; i < lines.length; i++) {
            var line = lines[i].trim();

            text += line;
            text += "\n";
        }

        return text;
    }

    private String get_incident(String[] lines) {
        var p = Pattern.compile("INCIDENT # (.*)");

        for (var line : lines) {
            var m = p.matcher(line.trim());

            if (m.matches())
                return m.group(1).trim();
        }

        return "";
    }

    private String get_code(String[] lines) {
        var p = Pattern.compile("Nature: (.*)Type: .*");

        for (var line : lines) {
            var m = p.matcher(line.trim());

            if (m.matches())
                return m.group(1).trim();
        }

        return "";
    }

    private int get_latlon(String[] lines, ParsedMessage pm, MessageService ms) {
        var p = Pattern.compile("Lat= ([-+]?\\d+\\.\\d+) +Lon= ([-+]?\\d+\\.\\d+)");

        for (var line : lines) {
            var m = p.matcher(line.trim());

            if (m.matches()) {
                var lat = Double.parseDouble(m.group(1));
                var lon = Double.parseDouble(m.group(2));
                var pos = new Position(lon, lat);
                var point = new Point(pos);

                pm.messageLatLon = point;

                try {
                    pm.location = ms.getLocationByPoint(pm.messageLatLon);

                    log.info(pm.messageLatLon.toString());
                    return 0;
                } catch (Exception e) {
                    log.error("getLocationByPoint failed for " + lat + " " + lon, e);
                }
            }
        }

        return 1;
    }

    private String get_address(Customer cust, String[] lines) {
        var a = Pattern.compile("Address: (.*)");
        var c = Pattern.compile("City: (.*)");
        var addr = "";
        var city = "";

		for (var line : lines) {
            var mAddr = a.matcher(line.trim());
            var mCity = c.matcher(line.trim());

            if (mAddr.matches())
                addr = mAddr.group(1).trim();

            if (mCity.matches())
            	city = mCity.group(1).trim();
        }

		// clean up address
		{
			var p = Pattern.compile("(.*);.*");
			var m = p.matcher(addr);

			if (m.matches())
				addr = m.group(1).trim();
		}

		{
			var p = Pattern.compile("(.*) +Zone:.*");
			var m = p.matcher(addr);

			if (m.matches())
				addr = m.group(1).trim();
		}

		// ignore city if it is just an abbreviation, eg "EMP"
		if (city.length() == 0 || city.contentEquals("EMP"))
			city = cust.address.city;

        // append city
        if (city.length() > 0)
            addr += (", " + city);

        // append state
        if (cust.address.state.length() > 0)
            addr += (", " + cust.address.state);

        return addr;
    }

    private int get_units(String[] lines, ParsedMessage pm) {
        var p = Pattern.compile("Responding Units: (.*)");

        for (var line : lines) {
            var m = p.matcher(line.trim());

            if (m.matches()) {
                var units = m.group(1).split(",");

                for (String u : units) {
                    pm.units.add(u);
                }
            }
        }

        return 0;
    }

}
