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
hc@hamilton-co.org:CAD:Unconscious > >5377 Foley Rd >Bld: >Apt: > >Delhi Twp FD >22:48>BLS33,M33 > >Xst:JONAS DR/IVYHILL DR >[1] Multi-Agency HCL Incident #: DLHI-0505211234,[2] M/69 IN AND OUT, HAS HX OF KIDNEY FAILURE [Shared], >39104260 >84621314
*/
public class delhitwpfire implements MsgParser {
    private static final Logger log = LoggerFactory.getLogger(delhitwpfire.class);

    @Override
    public ParsedMessage Process(Customer cust, String msg, MessageService msgService) {
        ParsedMessage parsedMessage = new ParsedMessage();
        parsedMessage.ErrorFlag = 99;

        try {
            String[] lines = msg.trim().split(">");

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
        return String.join(" ", lines);
    }

    private String get_incident(String[] lines) {
        if (lines.length < 12)
            return "";

        var p = Pattern.compile(".* Incident #: ([A-Z0-9-]+),.*");
        var m = p.matcher(lines[11]);

        if (m.matches())
            return m.group(1).trim();
        else
            return "";
    }

    private String get_code(String[] lines) {
        if (lines.length > 0) {
            var flds = lines[0].split(":");

            if (flds.length > 2)
                return flds[2].trim();
        }

        return "";
    }

    private String get_address(Customer cust, String[] lines) {
        if (lines.length < 3)
            return "";

        var addr = lines[2].trim();

        addr += (", " + cust.address.city);
        addr += (", " + cust.address.state);

        return addr;
    }

    private int get_latlon(String[] lines, ParsedMessage pm, MessageService ms) {
        if (lines.length < 14)
            return 1;

        var sLat = lines[12].trim();
        var sLon = lines[13].trim();
        var p = Pattern.compile("\\d+");

        if (p.matcher(sLat).matches() && p.matcher(sLon).matches()) {
            var lat = (Double.parseDouble(sLat) / 1000000);
            var lon = ((Double.parseDouble(sLon) / 1000000) * -1);

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

        return 1;
    }

    private int get_units(String[] lines, ParsedMessage pm) {
        if (lines.length < 9)
            return 0;

        for (var u : lines[8].split(",")) {
            pm.units.add(u.trim());
        }

        return 0;
    }

}
