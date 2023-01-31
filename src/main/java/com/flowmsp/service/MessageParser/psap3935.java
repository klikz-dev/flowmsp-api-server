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


public class psap3935 implements PSAPMsgParser {
    private static final Logger log = LoggerFactory.getLogger(psap2262.class);

    @Override
    public ParsedMessage Process(PSAP psap, String msg, MessageService msgService, PSAPService psapService) {
        ParsedMessage parsedMessage = new ParsedMessage();
        parsedMessage.ErrorFlag = 99;

        log.info("psap3935 msg");

        try {
            String[] lines = msg.split("\r?\n");

            parsedMessage.Code = get_code(lines);
            parsedMessage.text = get_text(lines);
            parsedMessage.incidentID = get_incident_id(lines);

            get_units(parsedMessage, lines);

            //Address should be of at-least 5 characters
            for (String unit : parsedMessage.units) {
                Customer customer = psapService.getCustomerFromUnit(unit, psap.id);

                if (customer != null) {
                    parsedMessage.Address = get_address(lines, customer);

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

    // first one is the station id
    private void get_units(ParsedMessage pm, String[] lines) {
        var units = get_matching_line(lines, "INFO: (.*)");

        for (var u : units.split(", ?")) {
            var x = u.split(":");

            if (x.length > 1)
                pm.units.add(x[1].trim());
        }

        return;
    }

    private String get_code(String[] lines) {
        return get_matching_line(lines, "DISPATCH:CALL:(.*)");
    }

    private String get_incident_id(String[] lines) {
        return get_matching_line(lines, "ID: (.*)");
    }

    private String get_address(String[] lines, Customer cust) {
        var addr = get_matching_line(lines, "ADDR: (.*)");
        var city = get_matching_line(lines, "CITY: (.*)");

        if (city.length() > 0)
            addr += (", " + city);

        // add state from customer record
        addr += (", " + cust.address.state);

        return addr;
    }

    @Override
    public ParsedMessage Process(Customer cust, String msg, MessageService msgService) {
        return null;
    }

}
