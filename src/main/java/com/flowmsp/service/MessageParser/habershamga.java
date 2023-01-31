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

import java.util.regex.Pattern;

/*
0 1/30/2021 12:49:02                      (date/time)
1 Medical - Difficulty Breathing          (Incident Type)
2 2021-00000332 (06814)                   (call number)
3 550 RIVER BRIDGE TRL, CORNELIA          (location/address)
4 WAYNE                                   (caller's name)
5 1250,E12,800,M12                        (units dispatched)
6 57 YO FEMALE /WEIGHT 145                (narrative)
7 HAVING DIFF BREATHING=20
8 NO FEVER=20
 */
public class habershamga implements PSAPMsgParser {
    private static final Logger log = LoggerFactory.getLogger(habershamga.class);

    @Override
    public ParsedMessage Process(PSAP psap, String msg, MessageService msgService, PSAPService psapService) {
        ParsedMessage parsedMessage = new ParsedMessage();
        parsedMessage.ErrorFlag = 99;

        log.info("habershamga msg");

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

    private String get_incident_id(String[] lines) {
        if (lines.length <= 2)
            return "";

        var p = Pattern.compile("(\\d{4}-\\d{8}) .*");
        var m = p.matcher(lines[2]);

        if (m.matches())
            return m.group(1);
        else
            return "";
    }

    private String get_text(String[] lines) {
        return String.join(" ", lines);
    }

    // first one is the station id
    private void get_units(ParsedMessage pm, String[] lines) {
        if (lines.length <= 5)
            return;

        for (var n : lines[5].split(","))
            pm.units.add(n);

        return;
    }

    private String get_code(String[] lines) {
        if (lines.length > 1)
            return lines[1];
        else
            return "";
    }

    private String get_address(String[] lines, Customer cust) {
        if (lines.length <= 3)
            return "";

        var addr = lines[3];

        // add state from customer record
        addr += (", " + cust.address.state);

        return addr;
    }

    @Override
    public ParsedMessage Process(Customer cust, String msg, MessageService msgService) {
        return null;
    }

}
