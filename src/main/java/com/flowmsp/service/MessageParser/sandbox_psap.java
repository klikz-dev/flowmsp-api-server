package com.flowmsp.service.MessageParser;

import com.flowmsp.domain.customer.Customer;
import com.flowmsp.domain.location.Location;
import com.flowmsp.domain.psap.PSAP;
import com.flowmsp.service.PolygonUtil;
import com.flowmsp.service.Message.MessageService;
import com.flowmsp.service.psap.PSAPService;
import com.mongodb.client.model.geojson.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Position;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class sandbox_psap implements PSAPMsgParser {
    private static final Logger log = LoggerFactory.getLogger(sandbox_psap.class);

    @Override
    public ParsedMessage Process(PSAP psap, String msg, MessageService msgService, PSAPService psapService) {
        ParsedMessage parsedMessage = new ParsedMessage();
        parsedMessage.ErrorFlag = 99;

        log.info("sandbox psap msg");
        log.info(msg);

	/*
	  CALL: random population of fire alarms, odor of gas and smell of smoke
	  INFO: Auto-Traced Area
	  ADDR: 115 Franklin Turnpike
	  CITY: Mahwah
	  DST: NJ
	  ZIP: 07430
	  GPS: 41.100019,-74.146818
	 */
        try {
            String [] lines = msg.split("\n");

            parsedMessage.psap = psap;
            parsedMessage.text = msg.replace("\n", " ");
            parsedMessage.Code = get_matching_line(lines, "CALL: (.*)");

            var address = get_matching_line(lines, "ADDR: (.*)");
            var city = get_matching_line(lines, "CITY: (.*)");
            var gps = get_matching_line(lines, "GPS: (.*)");
            var dst = get_matching_line(lines, "DST: (.*)");
            var zip = get_matching_line(lines, "ZIP: (.*)");
            var units = get_matching_line(lines, "UNITS: (.*)");

            parsedMessage.incidentID = get_matching_line(lines, "ID: (.*)");

            get_units(units, parsedMessage);

            if (address.length() > 0 && city.length() > 0)
                parsedMessage.Address = address + ", " + city + ", " + dst + " " + zip;
            else
                parsedMessage.Address = "";

            log.info("Code: " + parsedMessage.Code);
            log.info("Address: " + parsedMessage.Address);

            // prefer lat/lon if available
            var p = Pattern.compile("([+-]?\\d+\\.\\d+), ?([+-]?\\d+\\.\\d+)");
            var m = p.matcher(gps);

            if (m.matches()) {
                var lat = m.group(1);
                var lon = m.group(2);

                var dlat = Double.parseDouble(lat) ;
                var dlon = Double.parseDouble(lon) ;

                var pos = new Position(dlon, dlat);
                var point = new Point(pos);

                parsedMessage.messageLatLon = point;

                try {
                    parsedMessage.location = msgService.getLocationByPoint(parsedMessage.messageLatLon);

                    log.info(parsedMessage.messageLatLon.toString());
                }
                catch (Exception e) {
                    log.error("getLocationByPoint failed for " + lat + " " + lon, e);
                }
            }
            else {
                //Address should be of at-least 5 characters
                    for (String unit : parsedMessage.units) {
                        Customer customer = psapService.getCustomerFromUnit(unit, psap.id);
                        if (customer != null) {
                            //Address should be of at-least 5 characters
                            if (parsedMessage.Address.length() > 5) {
                                boolean isLocationRetrieved = getLocationByAddress(msgService, parsedMessage, customer);
                                if (isLocationRetrieved) break; //needed this so we can stop loking for location after we already found it in one unit
                            }
                        }
                    }
                    parsedMessage.ErrorFlag = 0;
            }

            parsedMessage.ErrorFlag = 0;

        } catch (Exception ex) {
            //log.error(ex);
            parsedMessage.ErrorFlag = 1;
        }

        return parsedMessage;
    }

    private boolean getLocationByAddress(MessageService msgService, ParsedMessage parsedMessage, Customer cust) {
        boolean isLocationRetrieved = false;
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
            isLocationRetrieved = true;
        }
        if (latLon != null) {
            parsedMessage.messageLatLon = latLon;
            isLocationRetrieved = true;
        }

        return isLocationRetrieved;
    }

    private int get_units(String units, ParsedMessage pm) {
        if (units.length() == 0)
            return 1;

        for (String u : units.split(",")) {
            log.info("u=" + u);
            pm.units.add(u.trim());
        }

        return 0;
    }

    private String get_matching_line(String[] lines, String pattern) {
        var p = Pattern.compile(pattern);

        for (int i = 0; i < lines.length; i++) {
            var line = lines[i].trim();

            var m = p.matcher(line);

            if (m.matches()) {
                log.info(i + " found pattern " + pattern);
                return m.group(1);
            }
        }

        return "";
    }

    @Override
    public ParsedMessage Process(Customer cust, String msg, MessageService msgService) {
        return null;
    }
}
