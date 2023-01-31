package com.flowmsp.service.MessageParser;

import com.flowmsp.SlugContext;
import com.flowmsp.domain.customer.Customer;
import com.flowmsp.domain.location.Location;
import com.flowmsp.domain.psap.PSAP;
import com.flowmsp.service.Message.MessageService;
import com.flowmsp.service.PolygonUtil;
import com.flowmsp.service.psap.PSAPService;
import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Position;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.regex.Pattern;
import java.util.HashMap;


/*
Call Time: 03/21/2021 19:59:15
Call Type: FIRE - EMS Excluding Vehicle Accident With Injury
Lat: 41.876255
Lng: -87.902221
Address: 323 WOLF RD
City: HILLSIDE
Primary Incident: C21-009065
Assigned Units: AMB401, ENG404
 */
public class psap2262 implements PSAPMsgParser {
    private static final Logger log = LoggerFactory.getLogger(psap2262.class);

    @Override
    public ParsedMessage Process(PSAP psap, String msg, MessageService msgService, PSAPService psapService) {
        ParsedMessage parsedMessage = new ParsedMessage();
        parsedMessage.ErrorFlag = 99;

        log.info("psap2262 msg");

        try {
            String[] flds = msg.split("\r?\n");

            parsedMessage.Code = get_code(flds);
            parsedMessage.text = get_text(flds);
            parsedMessage.incidentID = get_incident_id(flds);

            get_units(parsedMessage, flds);

            var latlon = get_latlon(flds, parsedMessage, msgService);

            //Address should be of at-least 5 characters
            for (String unit : parsedMessage.units) {
                Customer customer = psapService.getCustomerFromUnit(unit, psap.id);

                if (customer != null) {
                    parsedMessage.Address = get_address(flds, customer);

                    if (latlon == 0)
                        break;

                    //Address should be of at-least 5 characters
                    if (parsedMessage.Address.length() > 5) {
                        SlugContext.setSlug(customer.slug);
                        boolean isLocationRetrieved = getLocationByAddress(msgService, parsedMessage, customer);
                        SlugContext.clearSlug();

                        //needed this so we can stop loking for location after we already found it in one unit
                        if (isLocationRetrieved)
                            break;
                    }
                }
            }

            parsedMessage.ErrorFlag = 0;
        } catch (Exception ex) {
            // log.error(ex);
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

    private String get_text(String[] lines) {
        return String.join(" ", lines);
    }

    // first one is the station id
    private void get_units(ParsedMessage pm, String[] lines) {
        var p = Pattern.compile("Assigned Units: (.*)");

        for (var line : lines) {
            var m = p.matcher(line);

            if (m.matches()) {
                var units = m.group(1);
                for (var u : units.split(", ?")) {
                    pm.units.add(u);
                }
            }
        }

        return;
    }

    private String get_code(String[] lines) {
        var p = Pattern.compile("Call Type: (.*)");

        for (var line : lines) {
            var m = p.matcher(line);

            if (m.matches())
                return m.group(1);
        }

        return "";
    }

    private String get_incident_id(String[] lines) {
        var p = Pattern.compile("Primary Incident: (.*)");

        for (var line : lines) {
            var m = p.matcher(line);

            if (m.matches())
                return m.group(1);
        }

        return "";
    }

    private String get_address(String[] lines, Customer cust) {
        var pAddr = Pattern.compile("Address: (.*)");
        var pCity = Pattern.compile("City: (.*)");
        var address = "";
        var city = "";

        for (var line : lines) {
            var mAddr = pAddr.matcher(line);
            var mCity = pCity.matcher(line);

            if (mAddr.matches())
                address = mAddr.group(1);
            else if (mCity.matches())
                city = mCity.group(1);
        }

        if (city.length() > 0)
            address += (", " + city);

        // add state from customer record
        address += (", " + cust.address.state);

        return address;
    }

    private int get_latlon(String[] lines, ParsedMessage pm, MessageService ms) {
        var pLat = Pattern.compile("Lat: ([-+]?\\d+\\.\\d+)");
        var pLon = Pattern.compile("Lng: ([-+]?\\d+\\.\\d+)");
        var lat = "";
        var lon = "";

        for (var line : lines) {
            var mLat = pLat.matcher(line);
            var mLon = pLon.matcher(line);

            if (mLat.matches())
                lat = mLat.group(1);
            else if (mLon.matches())
                lon = mLon.group(1);
        }
        
        if (lat.length() > 0 && lon.length() > 0) {
            var dlat = Double.parseDouble(lat);
            var dlon = Double.parseDouble(lon);

            var pos = new Position(dlon, dlat);
            var point = new Point(pos);

            pm.messageLatLon = point;
            return 0;
            /* commented this out as it requires a slug which we don't have
            try {
                pm.location = ms.getLocationByPoint(pm.messageLatLon);

                log.info(pm.messageLatLon.toString());
                return 0;
            } catch (Exception e) {
                log.error("getLocationByPoint failed for " + lat + " " + lon, e);
            }
             */
        }

        return 1;
    }

    @Override
    public ParsedMessage Process(Customer cust, String msg, MessageService msgService) {
        return null;
    }

}
