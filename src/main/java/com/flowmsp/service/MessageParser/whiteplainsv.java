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

/*
DISPATCH:BAFD:0BAFD - 10/14 22:46 - BAFD:20-000050 9914 EMERGENCY AID SERVICES

150 HICKORY,CARLYLE//BAFD:0BAFD//53 Y/O F, C/DIFF BREATHING, NAUSEA, DEHYDRATED
 */

public class whiteplainsv implements MsgParser {
    private static final Logger log = LoggerFactory.getLogger(whiteplainsv.class);

    @Override
    public ParsedMessage Process(Customer cust, String msg, MessageService msgService) {
        ParsedMessage parsedMessage = new ParsedMessage();
        parsedMessage.ErrorFlag = 99;

        log.info("whiteplainsv msg");
        log.info(msg);

        try {
            String[] lines = msg.split("\r\n");

            parsedMessage.Code = get_code(lines);
            parsedMessage.text = get_text(lines);
            parsedMessage.Address = get_address(cust, lines);

            get_units(parsedMessage, lines);

            log.info("Code: " + parsedMessage.Code);
            log.info("Address: " + parsedMessage.Address);

            //Address should be of at-least 5 characters
            if (parsedMessage.Address.length() > 5) {
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

    private String get_text(String[] lines) {
        return String.join(" ", lines);
    }

    private void get_units(ParsedMessage pm, String[] lines) {
        if (lines.length > 4) {
            var nodes = lines[4].split(",");

            for (int i = 0; i < nodes.length; i++)
                pm.units.add(nodes[i]);
        }
    }

    private String get_code(String[] lines) {
        if (lines.length > 1)
            return lines[1];
        else
            return "";
    }

    private String get_address(Customer cust, String[] lines) {
        if (lines.length < 2)
            return "";

        var p = Pattern.compile("^(.*),(.*)$");
        var m = p.matcher(lines[2]);
        var addr = "";
        var city_code = "";

        if (m.matches()) {
            addr = m.group(1);
            city_code = m.group(2);
        }
        else
            addr = lines[2];

        // add city and state
        if (city_code.equalsIgnoreCase("ANN"))
            addr += ", Anniston, AL";
        else if (city_code.equalsIgnoreCase("PIE"))
            addr += ", Piedmont, AL";
        else if (city_code.equalsIgnoreCase("PIC"))
            addr += ", Piedmont, AL";
        else if (city_code.equalsIgnoreCase("JAU"))
            addr += ", Jacksonville, AL";
        else {
            addr += (", " + cust.address.city + ", AL");
        }

        return addr;
    }
}
