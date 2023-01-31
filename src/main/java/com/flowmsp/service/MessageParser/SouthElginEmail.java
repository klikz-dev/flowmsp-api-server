package com.flowmsp.service.MessageParser;

import com.flowmsp.domain.customer.Customer;
import com.flowmsp.domain.location.Location;
import com.flowmsp.service.PolygonUtil;
import com.flowmsp.service.Message.MessageService;
import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Position;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
Call Type|Location|Common Name|Closest Intersection|Create Date/Time|Units|Narrative (up to 100 characters)|Latitude|Longitude|District
*/
public class SouthElginEmail implements MsgParser {
    private static final Logger log = LoggerFactory.getLogger(SouthElginEmail.class);

    @Override
    public ParsedMessage Process(Customer cust, String msg, MessageService msgService) {
        ParsedMessage parsedMessage = new ParsedMessage();
        parsedMessage.ErrorFlag = 99;

        try {
            String plainMsg = msg;

            if (plainMsg.contains("Msg-")) {
                plainMsg = plainMsg.substring(plainMsg.indexOf("Msg-") + "Msg-".length());
                plainMsg = plainMsg.trim();
            }

            String[] flds = plainMsg.split("\\|");

            var latlon_ok = get_latlon(flds, parsedMessage, msgService);

            parsedMessage.text = plainMsg;
            parsedMessage.Address = flds[1];
            parsedMessage.Code = flds[0];

            get_units(parsedMessage, flds);

            if (!parsedMessage.Address.contains(",")) {
                parsedMessage.Address += (", " + cust.address.city);
                parsedMessage.Address += (", " + cust.address.state);
                log.info("city/state added South Elgin");
            }

            parsedMessage.text = plainMsg;

            log.info("Code: " + parsedMessage.Code);
            log.info("Address: " + parsedMessage.Address);

            if (latlon_ok == 0) {
                log.info("using LAT/LON");
                parsedMessage.ErrorFlag = 0;
            }

            //Address should be of at-least 5 characters
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

    private int get_latlon(String[] flds, ParsedMessage pm, MessageService ms) {
        var lat = flds[7];
        var lon = flds[8];

        // seems to indicate lat/lon is not populated
        if (lat.equals("-361") || lon.equals("-361"))
            return 1;

        log.info("lat=" + lat);
        log.info("lon=" + lon);

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
    // first one is the station id
    private void get_units(ParsedMessage pm, String[] lines) {
        if (lines.length <= 5)
            return;

        for (var u: lines[5].split(","))
            pm.units.add(u.trim());

        return;
    }
}
