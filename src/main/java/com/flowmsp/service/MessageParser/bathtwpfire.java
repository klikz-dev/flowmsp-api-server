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
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

/*
4175 HARDING HWY, BATH TWP |2020-00003045 (02-107)|11/26/2020 11:39:54|Fire Alarm|PERRY CHAPEL RD / JAMES BIDDLE DR|1302|Ops 1|BTFD 4-1|40.7311555078061 -84.0271143005938
*/
public class bathtwpfire implements MsgParser {
    private static final Logger log = LoggerFactory.getLogger(bathtwpfire.class);

    @Override
    public ParsedMessage Process(Customer cust, String msg, MessageService msgService) {
        ParsedMessage parsedMessage = new ParsedMessage();
        parsedMessage.ErrorFlag = 99;

        try {
            var idx = msg.indexOf("Msg-");
            var plainMsg = "";

            if (idx >= 0)
                plainMsg = msg.substring(idx + 4);
            else
                plainMsg = msg;

            String[] lines = plainMsg.split("\\|");

            parsedMessage.text = get_text(lines);

            parsedMessage.Code = get_code(lines, parsedMessage);
            parsedMessage.Address = get_address(lines, parsedMessage);
            var latlon_ok = get_latlon(lines, parsedMessage, msgService);

            log.info("Code: " + parsedMessage.Code);
            log.info("Address: " + parsedMessage.Address);

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

    private String get_text(String[] lines) {
        return String.join(" ", lines);
    }

    private String get_code(String[] lines, ParsedMessage pm) {
        if (lines.length > 3)
            return lines[3];
        else
            return "";
    }

    private int get_latlon(String[] lines, ParsedMessage pm, MessageService ms) {
        if (lines.length <= 8)
            return 1;

        String[] latlon = lines[8].split(" ");

        if (latlon.length < 2)
            return 1;

        var lat = latlon[0];
        var lon = latlon[1];

        if (lat.length() > 0 && lon.length() > 0) {
            var dlat = Double.parseDouble(lat);
            var dlon = Double.parseDouble(lon);

            var pos = new Position(dlon, dlat);
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

    private String get_address(String[] lines, ParsedMessage pm) {
    	var p = Pattern.compile("Alarm Level [a-zA-Z0-9]+ (.*)");

        if (lines.length > 1) {
            var m = p.matcher(lines[0]);

            if (m.matches())
                return m.group(1).trim();
            else
                return lines[0].trim();
        }
        else
            return "";
    }
}
