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


public class HomewoodEmail implements MsgParser {
    private static final Logger log = LoggerFactory.getLogger(HomewoodEmail.class);

    @Override
    public ParsedMessage Process(Customer cust, String msg, MessageService msgService) {
        ParsedMessage parsedMessage = new ParsedMessage();
        parsedMessage.ErrorFlag = 99;

        log.info("msg");
        log.info(msg);

        String plainMsg = msg;

        if (plainMsg.contains("Msg-")) {
            plainMsg = plainMsg.substring(plainMsg.indexOf("Msg-") + "Msg-".length());
            plainMsg = plainMsg.trim();
        }

        try {
            parsedMessage.text = plainMsg;

            var latlon_rc = getLatLon(plainMsg, parsedMessage, msgService);
            var rc = getAddress(plainMsg, parsedMessage);

            // if LAT/LON not in message use address
            if (latlon_rc == 0)
                parsedMessage.ErrorFlag = 0;
            else if (rc == 0) {
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
            }
            else
                parsedMessage.ErrorFlag = 1;
        } catch (Exception ex) {
            parsedMessage.ErrorFlag = 1;
        }
        return parsedMessage;
    }

    public int getAddress(String msg, ParsedMessage pm) {
        String[] incident_types = {
                "EMS-[a-zA-Z]+",
                "Lift Asst",
                "Accident w Inj",
                "Assist PD",
                "Alarm-[a-zA-Z]+",
                "Elevator Releas",
                "Suicidal Subj",
                "Wire-[a-zA-Z]+",
                "Gas Leak Inside",
                "Gas Spill",
                "Fire-[a-zA-Z]+",
                "CO",
                "Odor Invest",
                "Assist Other FD",
                "Smoke Invest",
                "Wash Down",
        };
        var s = String.join("|", incident_types);
        var p = Pattern.compile("([0-9]{2}\\.[0-9]{2}\\.[0-9]{2}) ([0-9]{2}/[0-9]{2}/[0-9]{2}) (" + s + ") (.*)");
        var m = p.matcher(msg);

        //var p1 = Pattern.compile("([0-9]{2}\.[0-9]{2}\.[0-9]{2}) ([0-9]{2}/[0-9]{2}/[0-9]{2}) CO (.*)");

        if (!m.matches()) {
            log.info("getAddress, pattern match failed");
            return 1;
        }

        log.info("getAddress, pattern match succeeded");

        var addr = m.group(4)
                .replace(",HWD ", ", Homewood, IL ")
                .replace("#", "APT ")
                .replace(";", " ");

        if (addr.contains(" HWFD ")) {
            var i = addr.indexOf(" HWFD ");
            addr = addr.substring(0, i);
        }

        pm.Address = addr;
        pm.Code = m.group(3);

        log.info("Addr=<" + pm.Address + ">");
        log.info("Code=<" + pm.Code + ">");

        return 0;
    }

    public int getLatLon(String msg, ParsedMessage pm, MessageService ms) {
        var p = Pattern.compile(".*LAT=([+-]\\d+\\.\\d+) LON=([+-]\\d+\\.\\d+).*");

        var m = p.matcher(msg);

        if (!m.matches()) {
            log.info("getLatLon, pattern match failed");
            return 1;
        }

        log.info("group1=" + m.group(1));
        log.info("group2=" + m.group(2));
        var lat = Double.parseDouble(m.group(1));
        var lon = Double.parseDouble(m.group(2));
        var pos = new Position(lon, lat);
        var point = new Point(pos);

        pm.messageLatLon = point;

        try {
            pm.location = ms.getLocationByPoint(pm.messageLatLon);
        }
        catch (Exception e) {
            log.error("getLocationByPoint failed for " + lat + " " + lon, e);
        }
        log.info(pm.messageLatLon.toString());
        log.info(pm.location.toString());

        return 0;
    }
}
