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


/*
Location: @M329 I17 S: @M329 I17 N MUN: COCO X_CORD: -111:41:08.3486 Y_CORD: 35:01:18.4804 MAP NUMBER: 453 EVENT ID: 3810585 EVENT NUMBER: R21000134 TYPE CODE: EMS CALLER NAME: DPS CALLER ADDR:  TIME: 01:57:11 Comments:  VEH WAS FLIPPED OVER THREE MILES BACK WAS SMOKING
*/

public class coconino428 implements MsgParser {
    private static final Logger log = LoggerFactory.getLogger(coconino428.class);

    @Override
    public ParsedMessage Process(Customer cust, String msg, MessageService msgService) {

        ParsedMessage parsedMessage = new ParsedMessage();
        parsedMessage.ErrorFlag = 99;

        try {
            parsedMessage.Address = get_address(cust, msg);
            var rc = get_latlon(msg, parsedMessage, msgService);
            parsedMessage.Code = get_code(msg);
            parsedMessage.incidentID = get_incidentID(msg);

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
            // log.error(ex);
            parsedMessage.ErrorFlag = 1;
        }

        return parsedMessage;
    }


    private String get_address(Customer cust, String msg) {
        var addr = "";

        if (msg.startsWith("Location: ")) {
           var fields = msg.split(":");
           addr = (fields[1].trim());
           addr += (", " + cust.address.city);
           addr += (", " + cust.address.state);
           addr += (", " + cust.address.zip);
        }
        return addr;
    }
    private int get_latlon(String msg, ParsedMessage pm, MessageService ms) {
        double lat = 0;
        double lon = 0;

        // Get Lat Long HMS from message
        Pattern pLat = Pattern.compile("Y_CORD: "+"([+]?\\d+[:]\\d+[:]\\d+.\\d+)");
        Pattern pLon = Pattern.compile("X_CORD: "+"([-]?\\d+[:]\\d+[:]\\d+.\\d+)");

        Matcher mLat = pLat.matcher(msg);
        Matcher mLon = pLon.matcher(msg);

        var fLat = mLat.find() ? mLat.group(1) : "";
        var fLon = mLon.find() ? mLon.group(1) : "";

        if(fLat.length() > 0)
        {
            // Convert to Lat
            String[] latDMS = fLat.split(":");
            lat = convertLatLon(latDMS);
        }
        if(fLon.length() > 0)
        {
            //Convert to Lon
            String[] lonDMS = fLon.split(":");
            lon = convertLatLon(lonDMS);
        }

        if (lat != 0 && lon != 0) {
            var pos = new Position(lon, lat);
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

    private double convertLatLon(String[] dms)
    {
        double deg = 0;
        deg = Math.signum(Integer.parseInt(dms[0])) * (Math.abs(Integer.parseInt(dms[0])) + (Integer.parseInt(dms[1]) / 60.0) + (Double.parseDouble(dms[2]) / 3600.0));

        //Truncate to 6 decimal places
        deg = deg * Math.pow(10, 6);
        deg = Math.floor(deg);
        deg = deg / Math.pow(10, 6);

        return deg;
    }

    private String get_code(String msg)
    {
        var code = "";

        if(msg.contains("CODE:")) {
            Pattern p = Pattern.compile("CODE:" + "\\W+(\\w+)");
            Matcher m = p.matcher(msg);
            code = m.find() ? m.group(1) : "";
        }

        return code;
    }

    private String get_incidentID(String msg)
    {
        var id = "";

        if(msg.contains("EVENT ID:")) {
            Pattern p = Pattern.compile("EVENT ID:" + "\\W+(\\d+)");
            Matcher m = p.matcher(msg);
            id = m.find() ? m.group(1) : "";
        }

        return id;

    }
}